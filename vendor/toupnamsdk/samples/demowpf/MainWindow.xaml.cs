using System;
using System.IO;
using System.Windows.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Runtime.InteropServices;
using System.ComponentModel;

namespace demowpf
{
    public partial class MainWindow : Window
    {
        private Toupnam cam_ = null;
        private Toupnam.device dev_;
        private WriteableBitmap bmp_ = null;
        private bool started_ = false;
        private uint count_ = 0;

        public MainWindow()
        {
            InitializeComponent();

            snap_.IsEnabled = false;
            auto_exposure_.IsEnabled = false;
            white_balance_.IsEnabled = false;
            slider_expotime_.IsEnabled = false;
            slider_r_.IsEnabled = false;
            slider_g_.IsEnabled = false;
            slider_b_.IsEnabled = false;

            Toupnam.Init(null);

            Closing += (sender, e) =>
            {
                cam_?.Close();
                cam_ = null;
            };
            Closed += (sender, e) => Toupnam.Fini();
        }

        private void OnEventError()
        {
            cam_.Close();
            cam_ = null;
            MessageBox.Show("Generic error.");
        }

        private void OnEventImage()
        {
            if (bmp_ != null)
            {
                uint width = 0, height = 0;
                try
                {
                    bmp_.Lock();
                    try
                    {
                        cam_.PullImage(bmp_.BackBuffer, 32, out width, out height);
                        bmp_.AddDirtyRect(new Int32Rect(0, 0, bmp_.PixelWidth, bmp_.PixelHeight));
                    }
                    finally
                    {
                        bmp_.Unlock();
                    }
                    image_.Source = bmp_;
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.ToString());
                }
            }
        }

        private void OnEventPara(uint nPara)
        {
            switch (nPara)
            {
                case Toupnam.PARA_EXPOTIME:
                    OnEventPara((val) =>
                    {
                        slider_expotime_.Value = (int)val;
                        label_expotime_.Content = (val / 1000).ToString();
                    });
                    break;
                case Toupnam.PARA_WBREDGAIN:
                    OnEventPara((val) =>
                    {
                        label_r_.Content = val.ToString();
                        slider_r_.Value = val;
                    });
                    break;
                case Toupnam.PARA_WBGREENGAIN:
                    OnEventPara((val) =>
                    {
                        label_g_.Content = val.ToString();
                        slider_g_.Value = val;
                    });
                    break;
                case Toupnam.PARA_WBBLUEGAIN:
                    OnEventPara((val) =>
                    {
                        label_b_.Content = val.ToString();
                        slider_b_.Value = val;
                    });
                    break;
                case Toupnam.PARA_AWB:
                    OnEventPara((val) =>
                    {
                        white_balance_.IsChecked = (val == 1);
                        slider_r_.IsEnabled = (val == 0);
                        slider_g_.IsEnabled = (val == 0);
                        slider_b_.IsEnabled = (val == 0);
                    });
                    break;
                case Toupnam.PARA_AEXPO:
                    OnEventPara((val) =>
                    {
                        auto_exposure_.IsChecked = (val == 1);
                        slider_expotime_.IsEnabled = (val == 0);
                    });
                    break;
                default:
                    break;
            }

            void OnEventPara(Action<int> pAction)
            {
                int val = 0;
                if (cam_.get_Para(nPara, out val) >= 0)
                    pAction(val);
            }
        }

        private void startDevice()
        {
            cam_ = Toupnam.Open(dev_.id);
            if (cam_ != null)
            {
                auto_exposure_.IsEnabled = true;
                snap_.IsEnabled = true;
                white_balance_.IsEnabled = true;
                slider_expotime_.Minimum = dev_.r[Toupnam.PARA_EXPOTIME].imin;
                slider_expotime_.Maximum = dev_.r[Toupnam.PARA_EXPOTIME].imax;
                slider_r_.Minimum = dev_.r[Toupnam.PARA_WBREDGAIN].imin;
                slider_r_.Maximum = dev_.r[Toupnam.PARA_WBREDGAIN].imax;
                slider_g_.Minimum = dev_.r[Toupnam.PARA_WBGREENGAIN].imin;
                slider_g_.Maximum = dev_.r[Toupnam.PARA_WBGREENGAIN].imax;
                slider_b_.Minimum = dev_.r[Toupnam.PARA_WBBLUEGAIN].imin;
                slider_b_.Maximum = dev_.r[Toupnam.PARA_WBBLUEGAIN].imax;

                int width = 0, height = 0;
                if (cam_.get_Size(out width, out height) >= 0)
                {
                    /* The backend of WPF is Direct3D/Direct2D, which is different from Winform's backend GDI.
                     * We use their respective native formats, Bgr32 in WPF, and Bgr24 in Winform
                     */
                    bmp_ = new WriteableBitmap(width, height, 0, 0, PixelFormats.Bgr32, null);
                    cam_.put_Para(Toupnam.PARA_FORMAT_LOCAL, 1);
                    if (cam_.StartPullModeWithCallback((uint nEvent, uint nPara, ref Toupnam.eventextra pExtra) =>
                    {
                        /* this is called by internal thread of toupnam.dll which is NOT the same of UI thread.
                         * So we use BeginInvoke
                         */
                        Dispatcher.BeginInvoke((Action)(() =>
                        {
                            /* this run in the UI thread */
                            if (cam_ != null)
                            {
                                switch (nEvent)
                                {
                                    case Toupnam.EVENT_ERROR:
                                        OnEventError();
                                        break;
                                    case Toupnam.EVENT_PARA:
                                        OnEventPara(nPara);
                                        break;
                                    case Toupnam.EVENT_IMAGE:
                                        OnEventImage();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }));
                    }) < 0)
                        MessageBox.Show("Failed to start camera.");
                    else
                    {
                        OnEventPara(Toupnam.PARA_EXPOTIME);
                        OnEventPara(Toupnam.PARA_WBREDGAIN);
                        OnEventPara(Toupnam.PARA_WBGREENGAIN);
                        OnEventPara(Toupnam.PARA_WBBLUEGAIN);
                        OnEventPara(Toupnam.PARA_AWB);
                        OnEventPara(Toupnam.PARA_AEXPO);
                    }
                        
                    started_ = true;
                }
            }
        }

        private void onClick_start(object sender, RoutedEventArgs e)
        {
            if (cam_ != null)
                return;

            Toupnam.device[] arr = Toupnam.Enum();
            if (arr.Length <= 0)
                MessageBox.Show("No camera found.");
            else if (1 == arr.Length)
            {
                dev_ = arr[0];
                startDevice();
            }
            else
            {
                ContextMenu menu = new ContextMenu() { PlacementTarget = start_, Placement = PlacementMode.Bottom };
                for (int i = 0; i < arr.Length; ++i)
                {
                    MenuItem mitem = new MenuItem() { Header = arr[i].name, CommandParameter = i }; //inbox
                    mitem.Click += (nsender, ne) =>
                    {
                        int idx = (int)(((MenuItem)nsender).CommandParameter); //oubox
                        if ((idx >= 0) && (idx < arr.Length))
                        {
                            dev_ = arr[idx];
                            startDevice();
                        }
                    };
                    menu.Items.Add(mitem);
                }
                menu.IsOpen = true;
            }
        }

        private void onClick_whitebalance(object sender, RoutedEventArgs e)
        {
            if (started_)
                cam_?.put_Para(Toupnam.PARA_AWB, (white_balance_.IsChecked ?? false) ? 1 : 0);
        }

        private void onChanged_r(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if ((cam_ != null) && started_)
                cam_.put_Para(Toupnam.PARA_WBREDGAIN, (int)slider_r_.Value);
        }

        private void onChanged_g(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if ((cam_ != null) && started_)
                cam_.put_Para(Toupnam.PARA_WBGREENGAIN, (int)slider_g_.Value);
        }

        private void onChanged_b(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if ((cam_ != null) && started_)
                cam_.put_Para(Toupnam.PARA_WBBLUEGAIN, (int)slider_b_.Value);
        }

        private void onChanged_expotime(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if ((cam_ != null) && started_)
                cam_.put_Para(Toupnam.PARA_EXPOTIME, (int)slider_expotime_.Value);
        }

        private void onClick_auto_exposure(object sender, RoutedEventArgs e)
        {
            if (started_)
                cam_?.put_Para(Toupnam.PARA_AEXPO, (auto_exposure_.IsChecked ?? false) ? 1 : 0);
        }

        private void OnClick_snap(object sender, RoutedEventArgs e)
        {
            if (cam_ != null)
            {
                if ((dev_.flag & Toupnam.FLAG_CAPTURE) == 0) // not support capture
                {
                    if (bmp_ != null)
                    {
                        using (FileStream fileStream = new FileStream(string.Format("demowpf_{0}.bmp", ++count_), FileMode.Create))
                        {
                            if (fileStream != null)
                            {
                                BitmapEncoder encoder = new BmpBitmapEncoder();
                                encoder.Frames.Add(BitmapFrame.Create(bmp_));
                                encoder.Save(fileStream);
                            }
                        }
                    }
                }
                else
                {
                    cam_.Capture(null, (int result, IntPtr pData, int nLength, ref Toupnam.bitmapinfo info) =>
                    {
                        if ((pData != IntPtr.Zero) && (nLength > 0))
                        {
                            try
                            {
                                using (FileStream file = new FileStream(string.Format("demowpf_{0}.jpg", ++count_), FileMode.Create, FileAccess.Write))
                                {
                                    if (file != null)
                                    {
                                        byte[] bytes = new byte[nLength];
                                        Marshal.Copy(pData, bytes, 0, nLength);
                                        file.Write(bytes, 0, nLength);
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                MessageBox.Show(ex.ToString());
                            }
                        }
                    });
                }
            }
        }
    }
}
