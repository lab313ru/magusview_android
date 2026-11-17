using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;
using System.Windows.Forms;
using System.Runtime.InteropServices;

namespace imageptrwinform
{
    public partial class Form1 : Form
    {
        [StructLayout(LayoutKind.Sequential)]
        private struct BITMAPINFOHEADER
        {
            public int biSize;
            public int biWidth;
            public int biHeight;
            public short biPlanes;
            public short biBitCount;
            public int biCompression;
            public int biSizeImage;
            public int biXPelsPerMeter;
            public int biYPelsPerMeter;
            public int biClrUsed;
            public int biClrImportant;
        }
        [StructLayout(LayoutKind.Sequential)]
        private struct BITMAPINFO
        {
            public BITMAPINFOHEADER bmiHeader;
        }
        [DllImport("gdi32.dll")]
        private static extern int StretchDIBits(IntPtr hdc, int XDest, int YDest, int DestWidth, int DestHeight, int XSrc, int YSrc, int SrcWidth, int SrcHeight, IntPtr lpBits, ref BITMAPINFO lpbmi, int iUsage, int rop);
        [DllImport("gdi32.dll")]
        private static extern int SetStretchBltMode(IntPtr hdc, int mode);

        private Toupnam cam_ = null;
        private Toupnam.device dev_;
        private uint count_ = 0;
        private Toupnam.imageptr ptr_ = new Toupnam.imageptr();
        private BITMAPINFO bmi_ = new BITMAPINFO();

        public Form1()
        {
            InitializeComponent();
            pictureBox1.Width = ClientRectangle.Right - pictureBox1.Left - button1.Top;
            pictureBox1.Height = ClientRectangle.Height - 2 * button1.Top;
            Toupnam.Init(null);

            bmi_.bmiHeader.biSize = Marshal.SizeOf(typeof(BITMAPINFOHEADER));
            bmi_.bmiHeader.biPlanes = 1;
            bmi_.bmiHeader.biBitCount = 24;
        }

        private void Form_SizeChanged(object sender, EventArgs e)
        {
            pictureBox1.Width = ClientRectangle.Right - pictureBox1.Left - button1.Top;
            pictureBox1.Height = ClientRectangle.Height - 2 * button1.Top;
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            button2.Enabled = false;
            trackBar1.Enabled = false;
            checkBox1.Enabled = false;
            checkBox2.Enabled = false;
            trackBar2.Enabled = false;
            trackBar3.Enabled = false;
            trackBar4.Enabled = false;
        }

        private void OnEventError()
        {
            cam_.Close();
            cam_ = null;
            MessageBox.Show("Generic error.");
        }

        private void OnEventPara(uint nPara)
        {
            switch (nPara)
            {
                case Toupnam.PARA_EXPOTIME:
                    OnEvenPara((val) =>
                    {
                        trackBar1.Value = val;
                        label1.Text = (val / 1000).ToString();
                    });
                    break;
                case Toupnam.PARA_WBREDGAIN:
                    OnEvenPara((val) =>
                    {
                        trackBar2.Value = val;
                        label2.Text = val.ToString();
                    });
                    break;
                case Toupnam.PARA_WBGREENGAIN:
                    OnEvenPara((val) =>
                    {
                        trackBar3.Value = val;
                        label3.Text = val.ToString();
                    });
                    break;
                case Toupnam.PARA_WBBLUEGAIN:
                    OnEvenPara((val) =>
                    {
                        trackBar4.Value = val;
                        label4.Text = val.ToString();
                    });
                    break;
                case Toupnam.PARA_AWB:
                    OnEvenPara((val) =>
                    {
                        checkBox2.Checked = (val == 1);
                        trackBar2.Enabled = (val == 0);
                        trackBar3.Enabled = (val == 0);
                        trackBar4.Enabled = (val == 0);
                    });
                    break;
                case Toupnam.PARA_AEXPO:
                    OnEvenPara((val) =>
                    {
                        checkBox1.Checked = (val == 1);
                        trackBar1.Enabled = (val == 0);
                    });
                    break;
                default:
                    break;
            }

            void OnEvenPara(Action<int> pAction)
            {
                int val = 0;
                if (cam_.get_Para(nPara, out val) >= 0)
                    pAction(val);
            }
        }

        private void OnEventImage()
        {
            Toupnam.imageptr ptr = new Toupnam.imageptr();
            if (cam_.PullImagePtr(out ptr) >= 0) {
                if (ptr_.id != 0)
                    cam_.PushImagePtr(ptr_.id);
                ptr_ = ptr;
                pictureBox1.Invalidate();
            }
        }

        private void OnStart(object sender, EventArgs e)
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
                ContextMenuStrip ctxmenu = new ContextMenuStrip();
                ctxmenu.ItemClicked += (nsender, ne) =>
                {
                    int idx = (int)(ne.ClickedItem.Tag); //oubox
                    if ((idx >= 0) && (idx < arr.Length))
                    {
                        dev_ = arr[idx];
                        startDevice();
                    }
                };
                for (int i = 0; i < arr.Length; ++i)
                    ctxmenu.Items.Add(arr[i].name).Tag = i; //inbox
                ctxmenu.Show(button1, 0, 0);
            }
        }

        private void startDevice()
        {
            cam_ = Toupnam.Open(dev_.id);
            if (cam_ != null)
            {
                checkBox1.Enabled = true;
                button2.Enabled = true;
                checkBox2.Enabled = true;
                trackBar1.SetRange(dev_.r[Toupnam.PARA_EXPOTIME].imin, dev_.r[Toupnam.PARA_EXPOTIME].imax);
                trackBar2.SetRange(dev_.r[Toupnam.PARA_WBREDGAIN].imin, dev_.r[Toupnam.PARA_WBREDGAIN].imax);
                trackBar3.SetRange(dev_.r[Toupnam.PARA_WBGREENGAIN].imin, dev_.r[Toupnam.PARA_WBGREENGAIN].imax);
                trackBar4.SetRange(dev_.r[Toupnam.PARA_WBBLUEGAIN].imin, dev_.r[Toupnam.PARA_WBBLUEGAIN].imax);

                if (cam_.get_Size(out bmi_.bmiHeader.biWidth, out bmi_.bmiHeader.biHeight) >= 0)
                {
                    if (cam_.StartPullModeWithCallback((uint nEvent, uint nPara, ref Toupnam.eventextra pExtra) =>
                    {
                        /* this is called by internal thread of toupnam.dll which is NOT the same of UI thread.
                         * Why we use BeginInvoke, Please see:
                         * http://msdn.microsoft.com/en-us/magazine/cc300429.aspx
                         * http://msdn.microsoft.com/en-us/magazine/cc188732.aspx
                         * http://stackoverflow.com/questions/1364116/avoiding-the-woes-of-invoke-begininvoke-in-cross-thread-winform-event-handling
                         */
                        BeginInvoke((Action)(() =>
                        {
                            /* this run in the UI thread */
                            if (cam_ != null)
                            {
                                switch (nEvent)
                                {
                                    case Toupnam.EVENT_IMAGE:
                                        OnEventImage();
                                        break;
                                    case Toupnam.EVENT_PARA:
                                        OnEventPara(nPara);
                                        break;
                                    case Toupnam.EVENT_ERROR:
                                        OnEventError();
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
                }
            }
        }

        private void OnClosing(object sender, FormClosingEventArgs e)
        {
            cam_?.Close();
            cam_ = null;
			Toupnam.Fini();
        }

        private void checkBox1_CheckedChanged(object sender, EventArgs e)
        {
            cam_?.put_Para(Toupnam.PARA_AEXPO, checkBox1.Checked ? 1 : 0);
        }

        private void checkBox2_CheckedChanged(object sender, EventArgs e)
        {
            cam_?.put_Para(Toupnam.PARA_AWB, checkBox2.Checked ? 1 : 0);
        }

        private void OnExpoValueChange(object sender, EventArgs e)
        {
            if ((!checkBox1.Checked) && (cam_ != null))
                cam_.put_Para(Toupnam.PARA_EXPOTIME, trackBar1.Value);
        }

        private void OnWBRedChange(object sender, EventArgs e)
        {
            if ((!checkBox2.Checked) && (cam_ != null))
                cam_.put_Para(Toupnam.PARA_WBREDGAIN, trackBar2.Value);
        }

        private void OnWBGreenChange(object sender, EventArgs e)
        {
            if ((!checkBox2.Checked) && (cam_ != null))
                cam_.put_Para(Toupnam.PARA_WBGREENGAIN, trackBar3.Value);
        }

        private void OnWBBlueChange(object sender, EventArgs e)
        {
            if ((!checkBox2.Checked) && (cam_ != null))
                cam_.put_Para(Toupnam.PARA_WBBLUEGAIN, trackBar4.Value);
        }

        private void OnWhiteBalanceOnce(object sender, EventArgs e)
        {
            cam_?.put_Para(Toupnam.PARA_AWB, 1);
        }

        private void pictureBox1_Paint(object sender, PaintEventArgs e)
        {
            if (ptr_.buf != IntPtr.Zero) {
                Graphics g = e.Graphics;
                IntPtr hdc = g.GetHdc();
                SetStretchBltMode(hdc, 3);
                StretchDIBits(hdc, 0, 0, pictureBox1.ClientRectangle.Width, pictureBox1.ClientRectangle.Height,
                        0, 0, bmi_.bmiHeader.biWidth, bmi_.bmiHeader.biHeight,
                        ptr_.buf,
                        ref bmi_, 0, 0x00CC0020);
                g.ReleaseHdc();
                g.DrawString(label4.Text, this.Font, SystemBrushes.ControlText, 0, 0);
            }
        }

        private void OnCapture(object sender, EventArgs e)
        {
            if ((dev_.flag & Toupnam.FLAG_CAPTURE) == 0) {
                if (ptr_.buf != IntPtr.Zero) {
                    Bitmap bmp = new Bitmap(bmi_.bmiHeader.biWidth, bmi_.bmiHeader.biHeight, ((bmi_.bmiHeader.biWidth * 3 + 3) / 4) * 4, PixelFormat.Format24bppRgb, ptr_.buf);
                    bmp.Save(string.Format("imageptrwinform{0}.bmp", ++count_), ImageFormat.Bmp);
                }
            }
            else {
                cam_?.Capture(null, (int result, IntPtr pData, int nLength, ref Toupnam.bitmapinfo info) =>
                {
                    if ((pData != IntPtr.Zero) && (nLength > 0)) {
                        try {
                            using (FileStream file = new FileStream(string.Format("imageptrwinform{0}.jpg", ++count_), FileMode.Create, FileAccess.Write)) {
                                if (file != null) {
                                    byte[] bytes = new byte[nLength];
                                    Marshal.Copy(pData, bytes, 0, nLength);
                                    file.Write(bytes, 0, nLength);
                                }
                            }
                        }
                        catch (Exception ex) {
                            MessageBox.Show(ex.ToString());
                        }
                    }
                });
            }
        }
    }
}
