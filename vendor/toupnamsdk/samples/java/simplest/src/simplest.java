import java.io.IOException;
import java.nio.ByteBuffer;

public class simplest {
    static toupnam _cam = null;
    static byte[] _buf = null;
    static int _total = 0;

    private static class ImplEventCallback implements toupnam.IEventCallback {
        /* callbacks come from toupnam.dll/so internal threads */
        @Override
        public void onEvent(int nEvent, int nPara) {
            if (nEvent == toupnam.EVENT_IMAGE) {
                try {
                    toupnam.bitmapinfo info = new toupnam.bitmapinfo(0, 0);
                    _cam.PullImage(_buf, 24, info);
                    ++_total;
                } catch (toupnam.HRESULTException ex) {
                    System.out.println("start camera exception: " + ex);
                } finally {
                    System.out.println("out callback: " + _total + ", " + String.format("%02x", _buf[_buf.length / 2]));
                }
            }
        }
    }
    
    public static void main(String[] args) {
        toupnam.Init(null);
        System.out.println("wait 5 seconds to discover the online cameras");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        toupnam.device[] arr = toupnam.Enum();
        if (arr.length == 0)
            System.out.println("no camera found");
        else {
            System.out.println(arr[0].model);
            _cam = toupnam.Open(arr[0].id);
            if (_cam != null) {
                try {
                    toupnam.bitmapinfo info = _cam.get_Size();
                    int bufsize = toupnam.TDIBWIDTHBYTES(info.width * 24) * info.height;
                    System.out.println("width = " + info.width + ", height = " + info.height + ", bufsize = " + bufsize);
                    _buf = new byte[bufsize];
                    _cam.StartPullModeWithCallback(new ImplEventCallback());
                    System.out.println("Press Enter to exit");
                    try {
                        System.in.read();
                    } catch (IOException e) {
                    }
                } catch (toupnam.HRESULTException ex) {
                    System.out.println("start camera exception: " + ex);
                } finally {
                    _cam.close();
                    _cam = null;
                    _buf = null;
                }
            }
        }
    }
}
