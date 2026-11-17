import java.nio.ByteBuffer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class swing {
    static toupnam _cam;
    static String _name;
    static byte[] _buf;
    static int[] _iarr;
    static BufferedImage _img;
    static int _total;
    static JFrame _frame;
    static Canvas _canvas;

    private static class ImplCanvas extends Canvas {
        @Override
        public void paint(Graphics g) {
            if (_img != null)
                g.drawImage(_img, 0, 0, getWidth(), getHeight(), 0, 0, _img.getWidth(), _img.getHeight(), null);
        }
    }

    /* run in the UI thread: AWT event dispatching thread */
    private void OnEvent(int nEvent, int nPara) {
        toupnam.bitmapinfo info = new toupnam.bitmapinfo(0, 0);
        try {
            _cam.PullImage(_buf, 24, info);
            _total += 1;
        } catch (toupnam.HRESULTException ex) {
            JOptionPane.showMessageDialog(null, ex.toString());
            return;
        }
            
        WritableRaster ras = _img.getRaster();
        for (int i = 0; i < _img.getWidth() * _img.getHeight() * 3; ++i)
            _iarr[i] = _buf[i];
        ras.setPixels(0, 0, _img.getWidth(), _img.getHeight(), _iarr);
        _canvas.repaint();
        _frame.setTitle(_name + ": " + _total);
    }

    private class EventRunnable implements Runnable {
        private int nEvent_;
        private int nPara_;
        public EventRunnable(int nEvent, int nPara) {
            nEvent_ = nEvent;
            nPara_ = nPara;
        }
        @Override
        public void run() {
            OnEvent(nEvent_, nPara_);
        }
    }

    private void createAndShow() {
        _frame = new JFrame("");
        _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (_cam != null)
                {
                    _cam.close();
                    _cam = null;
                }
            }
        });
        _frame.getContentPane().setLayout(new BoxLayout(_frame.getContentPane(), BoxLayout.Y_AXIS));
        _frame.setSize(800, 600);

        JCheckBox checkAutoExposure = new JCheckBox("Auto Exposure");
        checkAutoExposure.setPreferredSize(new Dimension(0, 20));
        checkAutoExposure.setAlignmentX(Component.LEFT_ALIGNMENT);
        _frame.getContentPane().add(checkAutoExposure);
        checkAutoExposure.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_cam != null) {
                    try {
                        _cam.put_Para(toupnam.PARA_EXPOTIME, ((JCheckBox) e.getSource()).isSelected() ? 1 : 0);
                    } catch (toupnam.HRESULTException ex) {
                        JOptionPane.showMessageDialog(null, ex.toString());
                    }
                }
            }
        });

        _canvas = new ImplCanvas();
        _frame.getContentPane().add(_canvas);

        toupnam.Init(null);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
            
        try {
            toupnam.device[] arr = toupnam.Enum();
            if (arr.length <= 0)
                _frame.setTitle("no camera found");
            else {
                _name = arr[0].model;
                _frame.setTitle(_name + ": 0");
                _cam = toupnam.Open(arr[0].id);
                if (_cam != null) {
                    checkAutoExposure.setSelected(_cam.get_Para(toupnam.PARA_EXPOTIME) != 0);
                    toupnam.bitmapinfo info = _cam.get_Size();
                    _buf = new byte[toupnam.TDIBWIDTHBYTES(info.width * 24) * info.height];
                    _iarr = new int[info.width * info.height * 3];
                    _img = new BufferedImage(info.width, info.height, BufferedImage.TYPE_INT_BGR);
                    _cam.StartPullModeWithCallback(new toupnam.IEventCallback() {
                        /* callbacks come from toupnam.dll/so internal threads */
                        @Override
                        public void onEvent(int nEvent, int nPara) {
                            SwingUtilities.invokeLater(new EventRunnable(nEvent, nPara));
                        }
                    });
                }
            }
        } catch (toupnam.HRESULTException ex) {
            JOptionPane.showMessageDialog(null, ex.toString());
        }

        checkAutoExposure.setEnabled(_cam != null);
        _frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                swing o = new swing();
                o.createAndShow();
            }
        });
    }
}