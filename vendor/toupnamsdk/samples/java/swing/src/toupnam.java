import java.util.concurrent.atomic.AtomicInteger;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.*;
import com.sun.jna.Structure.FieldOrder;

/*
    Version: 5.33015.20261024
    We use JNA (https://github.com/java-native-access/jna) to call into the toupnam.dll/so API, the java class toupnam is a thin wrapper class to the native api.
*/
public class toupnam implements AutoCloseable {
    public final static int MAX = 64;
    
    public final static int PARA_UNKNOWN                = 0x00;
    public final static int PARA_EXPOTIME               = 0x01;    /* exposure time */
    public final static int PARA_AGAIN                  = 0x02;    /* gain */
    public final static int PARA_AEXPOTARGET            = 0x03;    /* auto exposure target */
    public final static int PARA_TEMP                   = 0x04;    /* color temperature */
    public final static int PARA_TINT                   = 0x05;
    public final static int PARA_CONTRAST               = 0x06;    /* contrast */
    public final static int PARA_HUE                    = 0x07;    /* hue */
    public final static int PARA_SATURATION             = 0x08;    /* saturation */
    public final static int PARA_BRIGHTNESS             = 0x09;    /* brightness */
    public final static int PARA_GAMMA                  = 0x0a;    /* gamma */
    public final static int PARA_AEXPO                  = 0x0b;    /* auto exposure */
    public final static int PARA_AWB                    = 0x0c;    /* XCAM1080P:once;  XCAM4K:(0:manual;1:global auto;2:roi) */
    public final static int PARA_BINSKIP                = 0x0d;    /* bin / skip */
    public final static int PARA_HZ                     = 0x0e;    /* power supply: 0 -> 60HZ AC;  1 -> 50Hz AC;   2 -> DC */
    public final static int PARA_BPS                    = 0x0f;    /* bits per second, kbps */
    public final static int PARA_KEYFRAME               = 0x10;    /* key frame interval */
    public final static int PARA_LOWLIGHTCOMPENSATION   = 0x11;    /* low light compensation */
    public final static int PARA_SHARPNESS              = 0x12;    /* sharpness */
    public final static int PARA_WBREDGAIN              = 0x13;    /* white balance red gain */
    public final static int PARA_WBGREENGAIN            = 0x14;    /* white balance green gain */
    public final static int PARA_WBBLUEGAIN             = 0x15;    /* white balance blue gain */
    public final static int PARA_DENOISE                = 0x16;    /* denoise */
    public final static int PARA_APSTA                  = 0x17;    /* ap/sta */
    public final static int PARA_CODEC                  = 0x18;    /* codec, H264, H265, etc */
    public final static int PARA_AFPOSITION             = 0x19;    /* auto focus sensor board positon (applicable to both AFDM and EFL) */
    public final static int PARA_AFMODE                 = 0x1a;    /* auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration) */
    public final static int PARA_AFZONE                 = 0x1b;    /* auto focus zone:
                                                                        the whole resolution is divided in w * h zones:
                                                                          w = imax >> 16
                                                                          h = imax & 0xffff
                                                                   */
    public final static int PARA_AFFEEDBACK             = 0x1c;    /* auto focus information feedback
                                                                        AFDM:
                                                                            0: unknown
                                                                            1: focused
                                                                            2: focusing
                                                                            3: defocuse (out of focus)
                                                                            4: up (workbench move up)
                                                                            5: down (workbench move down)
                                                                        EFL:
                                                                            NA           = 0x0, Not available
                                                                            PEAKPOINT    = 0x1, Focus completed, find the focus position
                                                                            DEFOCUS      = 0x2, End of focus, defocus
                                                                            NEAR         = 0x3, Focusing ended, object too close
                                                                            FAR          = 0x4, Focusing ended, object too far
                                                                            ROICHANGED   = 0x5, Focusing ends, roi changes
                                                                            SCENECHANGED = 0x6, Focusing ends, scene changes
                                                                            MODECHANGED  = 0x7, The end of focusing and the change in focusing mode is usually determined by the user moderator
                                                                            UNFINISH     = 0x8, The focus is not complete. At the beginning of focusing, it will be set as incomplete
                                                                   */
    public final static int PARA_AFPOSITION_ABSOLUTE    = 0x1d;    /* absolute auto focus sensor board positon */
    public final static int PARA_STATUS                 = 0x1e;    /* status */
    public final static int PARA_EVENT                  = 0x1f;    /* event */
    public final static int PARA_WBROILEFT              = 0x20;    /* white balance roi left */
    public final static int PARA_WBROITOP               = 0x21;    /* white balance roi top */
    public final static int PARA_WBROIWIDTH             = 0x22;    /* white balance roi width */
    public final static int PARA_WBROIHEIGHT            = 0x23;    /* white balance roi height */
    public final static int PARA_VFLIP                  = 0x24;    /* vertical flip */
    public final static int PARA_HFLIP                  = 0x25;    /* horizontal flip */
    public final static int PARA_CHROME                 = 0x26;    /* monochromatic mode */
    public final static int PARA_SIZE                   = 0x27;    /* video width & height */
    public final static int PARA_LIGHTADJUSTMENT        = 0x28;    /* light source brightness adjustment */
    public final static int PARA_ZOOM                   = 0x29;    /* zoom */
    public final static int PARA_EF_MODE                = 0x2a;    /* EFL, auto focus mode, 0: lens calibration mode; 1: manual focus; 2: single focus; 3: auto focus; */
    public final static int PARA_EF_FL                  = 0x2b;    /* EFL, focal length */
    public final static int PARA_EF_APERTURE            = 0x2c;    /* EFL, F#, 24~16bit:Cur, 15~8bit:Min, 7~0bit:Max */r, 15~8bit:Min, 7~0bit:Max */
    public final static int PARA_EF_FOCUS_MAX           = 0x2d;    /* EFL, maximum focal length */
    public final static int PARA_EF_LENS_ID             = 0x2e;    /* EFL, lens ID */
    public final static int PARA_EF_AFMF                = 0x2f;    /* EFL, lens focus mode, 0: AF; 1: MF; */
    public final static int PARA_EF_WD_ENABLE           = 0x30;    /* EFL, enable working distance function */
    public final static int PARA_EF_WD_NEAR             = 0x31;    /* EFL, near working distance point */
    public final static int PARA_EF_WD_FAR              = 0x32;    /* EFL, far working distance point */
    
    public final static int PARA_CHROME_LOCAL           = 0x80;    /* local monochromatic mode */
    public final static int PARA_VFLIP_LOCAL            = 0x81;    /* local vertical flip */
    public final static int PARA_HFLIP_LOCAL            = 0x82;    /* local horizontal flip */
    public final static int PARA_NEGATIVE_LOCAL         = 0x83;    /* local negative film */
    public final static int PARA_FORMAT_LOCAL           = 0x84;    /* output format: 0 => BGR888, 1 => BGRA8888, 2 => RGB888, 3 => RGBA8888, 4 => RAW; default: 0
                                                                      MUST be set BEFORE StartXXXX
                                                                   */
    
    public final static int PARA_STATUS_RECORDING       = 0x00000001;      /* recording */
    public final static int PARA_STATUS_SD              = 0x00000002;      /* sd card available */
    public final static int PARA_STATUS_SD_FULL         = 0x00000004;      /* sd card full */
    
    public final static int PARA_EVENT_FAT4G            = 0x00000001;      /* file size limit 4g in FAT32 */
    
    public final static int STATE_INITING               = 0x00;    /* initialization */
    public final static int STATE_NORMAL                = 0x01;    /* normal */
    public final static int STATE_UNREACHABLE           = 0x02;    /* network not reachable */
    
    public final static int FLAG_WIFI_AP                = 0x00000001;
    public final static int FLAG_WIFI_STA               = 0x00000002;
    public final static int FLAG_ETHERNET               = 0x00000004;
    public final static int FLAG_CAPTURE                = 0x00000008;  /* support the ability of capture image from camera */
    public final static int FLAG_AWBCHECKMODE           = 0x00000010;  /* auto white balance: check mode vs 'once' mode */
    public final static int FLAG_UVC                    = 0x00000020;  /* uvc camera */
    public final static int FLAG_WBGAIN                 = 0x00000040;  /* white balance gain mode or temp tint mode */
    public final static int FLAG_MULTICAST              = 0x00000080;  /* RTSP/RTP multicast */
    public final static int FLAG_AF                     = 0x00000100;  /* support auto focus */
    public final static int FLAG_SD_LIST                = 0x00000200;  /* support to list sd card */
    public final static int FLAG_SD                     = 0x00000400;  /* support sd card */
    public final static int FLAG_WBROI                  = 0x00000800;  /* white balance: 0:manual;1:global auto;2:roi */
    public final static int FLAG_STA_SUPPORT            = 0x00001000;  /* wifi camera has sta mode, app should have sta ssid & password function */
    public final static int FLAG_RTP_OVER_RTSP          = 0x00002000;  /* rtp over rtsp */
    public final static int FLAG_HZ_AUTOEXPO            = 0x00004000;  /* enable auto exposure when 50/60 hz */
    public final static int FLAG_AFDM                   = 0x00008000;
    public final static int FLAG_EFL                    = 0x00010000;
    public final static int FLAG_CAPTURERAW             = 0x00020000;  /* capture raw image */
    
    public final static int EVENT_ENUM                  = 0x01;    /* enum */
    public final static int EVENT_WIFI                  = 0x02;    /* wifi */
    public final static int EVENT_PARA                  = 0x03;    /* parameter change TOUPNAM_PARA_xxxx */
    public final static int EVENT_IMAGE                 = 0x04;    /* image */
    public final static int EVENT_LISTWIFI              = 0x05;    /* list wifi finished */
    public final static int EVENT_LISTDIR               = 0x07;    /* list dir */
    public final static int EVENT_THUMBNAIL             = 0x08;    /* thumbnail */
    public final static int EVENT_DIRCHANGE             = 0x09;    /* dir change notify */
    public final static int EVENT_RECORDSTART           = 0x0a;    /* record start */
    public final static int EVENT_RECORDSTOP            = 0x0b;    /* record stop */
    public final static int EVENT_DATETIME              = 0x0c;    /* date time */
    public final static int EVENT_ERROR                 = 0x80;    /* error */
    public final static int EVENT_EOF                   = 0x81;    /* end of file */

    static public int TDIBWIDTHBYTES(int bits) {
        return ((bits + 31) & (~31)) / 8;
    }

    public static class HRESULTException extends Exception {
        public final static int S_OK            = 0x00000000;
        public final static int S_FALSE         = 0x00000001;
        public final static int E_UNEXPECTED    = 0x8000ffff;
        public final static int E_NOTIMPL       = 0x80004001;
        public final static int E_NOINTERFACE   = 0x80004002;
        public final static int E_ACCESSDENIED  = 0x80070005;
        public final static int E_OUTOFMEMORY   = 0x8007000e;
        public final static int E_INVALIDARG    = 0x80070057;
        public final static int E_POINTER       = 0x80004003;
        public final static int E_FAIL          = 0x80004005;
        public final static int E_WRONG_THREAD  = 0x8001010e;
        public final static int E_GEN_FAILURE   = 0x8007001f;
        
        private final int _hresult;
        
        public HRESULTException(int hresult) {
            _hresult = hresult;
        }
        
        public int getHRESULT() {
            return _hresult;
        }
        
        @Override
        public String toString() {
            return toString(_hresult);
        }
        
        public static String toString(int hresult) {
            switch (hresult) {
                case E_INVALIDARG:
                    return "One or more arguments are not valid";
                case E_NOTIMPL:
                    return "Not supported or not implemented";
                case E_POINTER:
                    return "Pointer that is not valid";
                case E_UNEXPECTED:
                    return "Unexpected failure";
                case E_ACCESSDENIED:
                    return "General access denied error";
                case E_OUTOFMEMORY:
                    return "Out of memory";
                case E_WRONG_THREAD:
                    return "call function in the wrong thread";
                case E_GEN_FAILURE:
                    return "device not functioning";
                default:
                    return "Unspecified failure";
            }
        }
    }
    
    private static int errCheck(int hresult) throws HRESULTException {
        if (hresult < 0)
            throw new HRESULTException(hresult);
        return hresult;
    }
    
    @FieldOrder({ "idisable", "imin", "imax", "idef" })
    public static class range extends Structure {
        public int idisable;        /* 0 = "support this feature", 1 = "not support" */
        public int imin;            /* minimum value */
        public int imax;            /* maximum value */
        public int idef;            /* default value */
    }
    
    public static class device {
        public String   id;         /* unique camera id, used for Toupnam_Open */
        public String   sn;         /* serial number */
        public String   name;
        public String   model;
        public String   version;
        public String   addr;       /* ip */
        public String   url;        /* playback url, such as rtsp://xxxx/yyyy */
        public int      state;      /* STATE_xxx */
        public int      flag;       /* FLAG_xxx */
        public range[]  r;
    }
    
    @FieldOrder({ "id", "sn", "name", "model", "version", "addr", "url", "state", "flag", "r" })
    private static class _device extends Structure {
        public byte[]   id = new byte[64];         /* unique id, used for Open */
        public byte[]   sn = new byte[64];         /* serial number */
        public byte[]   name = new byte[64];
        public byte[]   model = new byte[64];
        public byte[]   version = new byte[64];
        public byte[]   addr = new byte[64];       /* ip */
        public byte[]   url = new byte[256];       /* playback url, such as rtsp://xxxx/yyyy */
        public int      state;                     /* STATE_xxx */
        public int      flag;                      /* FLAG_xxx */
        public range[]  r = new range[MAX];
    }

    @FieldOrder({ "id", "width", "height", "length", "buf" })
    public static class ImagePtr extends Structure {
        public int id;        /* 0 is reserved as an invalid id */
        public int width;     /* image width */
        public int height;    /* image height */
        public int length;    /* buf length */
        public Pointer buf;
    }
    
    public static class wifi {
        public String   ssid;
        public String   password;
    }
    
    @FieldOrder({ "ssid", "password" })
    private static class _wifi extends Structure {
        public byte[]   ssid = new byte[64];
        public byte[]   password = new byte[64];
    }
    
    public static class diritem {
        public int    type;                  /* 0 => file, 1 => directory */
        public String name;                  /* download the file with the url http://addr/path/name
                                                For example, Camera's ip is 192.168.1.2, and file in the sd card directory abc/xyz.mp4, then the url is http://192.168.1.2/abc/xyz.mp4
                                                So, it can be downloaded from this url with libcurl or WinInet.
                                             */
    }
    
    @FieldOrder({ "type", "name" })
    private static class _diritem extends Structure {
        public int    type;                  /* 0 => file, 1 => directory */
        public byte[] name = new byte[256];  /* download the file with the url http://addr/path/name
                                                For example, Camera's ip is 192.168.1.2, and file in the sd card directory abc/xyz.mp4, then the url is http://192.168.1.2/abc/xyz.mp4
                                                So, it can be downloaded from this url with libcurl or WinInet.
                                             */
    }
    
    public static class dirchange {
        public int      type;                /* 0 => add, 1 => del, 2 => rename */
        public String   name;
        public String   newname;
    }
    
    @FieldOrder({ "type", "name", "newname" })
    private static class _dirchange extends Structure {
        public int      type;                /* 0 => add, 1 => del, 2 => rename */
        public byte[]   name = new byte[256];
        public byte[]   newname = new byte[256];
    }
    
    public static class bitmapinfo {
        public int width;
        public int height;
        
        public bitmapinfo(int w, int h)
        {
            width = w;
            height = h;
        }
    }
    
    @FieldOrder({ "biSize", "biWidth", "biHeight", "biPlanes", "biBitCount", "biCompression", "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter", "biClrUsed", "biClrImportant" })
    public static class BITMAPINFOHEADER extends Structure {
        public int   biSize;
        public int   biWidth;
        public int   biHeight;
        public short biPlanes;
        public short biBitCount;
        public int   biCompression;
        public int   biSizeImage;
        public int   biXPelsPerMeter;
        public int   biYPelsPerMeter;
        public int   biClrUsed;
        public int   biClrImportant;
    }
    
    @FieldOrder({ "result", "length", "ptr", "ctx" })
    public static class eventextra extends Structure {
        public int  result;
        public int  length;
        public Pointer ptr;
        public Pointer ctx;
    }
    
    private interface CLib {
        String Toupnam_Version();
        void Toupnam_Fini();
        int Toupnam_Enum(_device[] arr, int sz);
        Pointer Toupnam_Open(String camId);
        Pointer Toupnam_Open_ByIndex(int index);
        void Toupnam_Close(Pointer h);
        int Toupnam_PullImagePtr(Pointer h, ImagePtr ptrImage);
        int Toupnam_PushImagePtr(Pointer h, int ptrId);
        int Toupnam_PullImage(Pointer h, Pointer pImageData, int bits, IntByReference pnWidth, IntByReference pnHeight);
        int Toupnam_StartPullModeWithCallback(Pointer h, long pCallbackContext);
        int Toupnam_Stop(Pointer h);
        int Toupnam_Pause(Pointer h, int bPause);
        _device Toupnam_get_Device(Pointer h);
        int Toupnam_get_Size(Pointer h, IntByReference pWidth, IntByReference pHeight) ;
        int Toupnam_get_CapSize(Pointer h, IntByReference pWidth, IntByReference pHeight);
        int Toupnam_get_FourCC(Pointer h, IntByReference pFourCC);
        int Toupnam_Record(Pointer h, byte[] outputFile);
        int Toupnam_put_Para(Pointer h, int para, int value);
        int Toupnam_get_Para(Pointer h, int para, IntByReference value);
        int Toupnam_put_Wifi(String camId, _wifi wifi);
        int Toupnam_get_Wifi(String camId);
        int Toupnam_list_Wifi(String camId, long pExtraCtx);
        int Toupnam_list_Dir(String camId, byte[] path, long pExtraCtx);
        int Toupnam_get_Thumbnail(String camId, byte[] path, long pExtraCtx);
        int Toupnam_change_Dir(String camId, byte[] path, _dirchange[] dc, int dclength);
        int Toupnam_RecordStart(String camId, byte[] outputFile, int recordtime, long pExtraCtx);
        int Toupnam_RecordStop(String camId, long pExtraCtx);
        int Toupnam_get_DateTime(String camId, long pExtraCtx);
        int Toupnam_put_DateTime(String camId, long t, long pExtraCtx);
        int Toupnam_put_Para_ById(String camId, int para, int value);
        int Toupnam_get_Para_ById(String camId, int para, IntByReference value);
        int Toupnam_get_Size_ById(String camId, int res, IntByReference pWidth, IntByReference pHeight);
        void Toupnam_PriFlag(int nFlag, int nMask);
        void Toupnam_add_Ip(Pointer[] arr);
        void Toupnam_del_Ip(Pointer[] arr);
        
        int Toupnam_PullImageArray(Pointer h, byte[] pImageData, int bits, IntByReference pnWidth, IntByReference pnHeight);
    }
    
    private interface WinLibrary extends CLib, StdCallLibrary {
        WinLibrary INSTANCE = (WinLibrary)Native.load("toupnam", WinLibrary.class, options_);
        
        interface EVENT_CALLBACK extends StdCallCallback {
            void invoke(int nEvent, int nPara, long pCallbackCtx, eventextra pExtra);
        }
        
        interface PARA_CALLBACK extends StdCallCallback {
            void invoke(int para, int value, long pCallbackCtx);
        }
        
        interface DATA_CALLBACK extends StdCallCallback {
            void invoke(Pointer pData, BITMAPINFOHEADER pHeader, long pCallbackCtx);
        }
        
        interface CAPTURE_CALLBACK extends StdCallCallback {
            void invoke(int result, Pointer pData, long nLength, BITMAPINFOHEADER pHeader, long pCallbackCtx);
        }
        
        void Toupnam_Init(EVENT_CALLBACK pCallback, long ctx);
        int Toupnam_StartPushMode(Pointer h, DATA_CALLBACK pDataCallback, PARA_CALLBACK pParaCallback, long pCallbackCtx);
        int Toupnam_Capture(Pointer h, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, long pCallbackCtx);
        int Toupnam_Capture_ById(String camId, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, long pCallbackCtx);
        int Toupnam_StartPullModeWithWndMsg(Pointer h, Pointer hWnd, int nMsg);
    }
    
    private interface CLibrary extends CLib, Library {
        CLibrary INSTANCE = (CLibrary)Native.load("toupnam", CLibrary.class, options_);
        
        interface EVENT_CALLBACK extends Callback {
            void invoke(int nEvent, int nPara, long pCallbackCtx, eventextra pExtra);
        }
        
        interface PARA_CALLBACK extends Callback {
            void invoke(int para, int value, long pCallbackCtx);
        }
        
        interface DATA_CALLBACK extends Callback {
            void invoke(Pointer pData, BITMAPINFOHEADER pHeader, long pCallbackCtx);
        }
        
        interface CAPTURE_CALLBACK extends Callback {
            void invoke(int result, Pointer pData, long nLength, BITMAPINFOHEADER pHeader, long pCallbackCtx);
        }
        
        void Toupnam_Init(EVENT_CALLBACK pCallback, long ctx);
        int Toupnam_StartPushMode(Pointer h, DATA_CALLBACK pDataCallback, PARA_CALLBACK pParaCallback, long pCallbackCtx);
        int Toupnam_Capture(Pointer h, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, long pCallbackCtx);
        int Toupnam_Capture_ById(String camId, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, long pCallbackCtx);
    }
    
    /*
        when frame arrive, IDataCallback is callbacked. (null == pData) means that something is error.
        when PARA_xxx value is changed, IParaCallback is callbacked.
        pCallbackCtx is the callback context which is passed by Start.
        IDataCallback and IParaCallback are callbacked by an internal thread of toupnam.dll, so please pay attention to multithread problem.
    */
    public interface IEventCallback {
        void onEvent(int nEvent, int nPara);
    }
    
    public interface IParaCallback {
        void onPara(int para, int value);
    }
    
    public interface IDataCallback {
        void onData(Pointer pData, bitmapinfo info);
    }
    
    public interface ICaptureCallback {
        void onCapture(int result, Pointer pData, long nLength, bitmapinfo info);
    }
    
    private final static Map options_ = new HashMap() {
        {
            put(Library.OPTION_FUNCTION_MAPPER, new FunctionMapper() {
                HashMap<String, String> funcmap_ = new HashMap() {
                    {
                        put("Toupnam_PullImageArray", "Toupnam_PullImage");
                    }
                };
                
                @Override
                public String getFunctionName(NativeLibrary library, Method method) {
                    String name = method.getName();
                    String str = funcmap_.get(name);
                    if (str != null)
                        return str;
                    else
                        return name;
                }
            });
        }
    };
    
    private static void copyByteArray(byte[] dst, String src) {
        byte[] arr = Native.toByteArray(src);
        System.arraycopy(arr, 0, dst, 0, (dst.length < arr.length) ? dst.length : arr.length);
    }
    
    private static class cbobj {
        Object  obj;    /* callback interface */
        int     id;     /* objid of toupnam */
        public cbobj(Object o, int i) {
            obj = o;
            id = i;
        }
    }
    
    private final static CLib _lib = Platform.isWindows() ?  WinLibrary.INSTANCE : CLibrary.INSTANCE;
    private final static Hashtable<Integer, toupnam> _hash = new Hashtable<Integer, toupnam>();
    private final static Hashtable<Integer, cbobj> _cbmap = new Hashtable<Integer, cbobj>();
    private static Callback _callback_event = null;
    private static Callback _callback_capture = null;
    private static AtomicInteger _clsid = new AtomicInteger(0);
    private int _objid = 0;
    private Pointer _handle = null;
    private Callback _callback_data = null;
    private Callback _callback_para = null;
    private IDataCallback _cb_data = null;
    private IParaCallback _cb_para = null;
    
    /*
        the object of toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = new toupnam (The constructor is private on purpose)
    */
    private toupnam(Pointer h) {
        _handle = h;
        _objid = _clsid.incrementAndGet();
        _hash.put(_objid, this);
    }
    
    @Override
    public void close() {
        if (_handle != null) {
            _lib.Toupnam_Close(_handle);
            _handle = null;
        }

        _cbmap.entrySet().removeIf(entry -> entry.getValue().id == _objid);
        _hash.remove(_objid);
    }
    
    private static void __convert_device(device newdev, _device dev)
    {
        newdev.id = Native.toString(dev.id);
        newdev.sn = Native.toString(dev.sn);
        newdev.name = Native.toString(dev.name);
        newdev.model = Native.toString(dev.model);
        newdev.version = Native.toString(dev.version);
        newdev.addr = Native.toString(dev.addr);
        newdev.url = Native.toString(dev.url);
        newdev.state = dev.state;
        newdev.flag = dev.flag;
        newdev.r = dev.r;
    }
    
    private static void OnCaptureCallback(int result, Pointer pData, long nLength, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
        cbobj o = _cbmap.get((int)pCallbackCtx);
        if ((o != null) && (o.obj instanceof ICaptureCallback)) {
            ICaptureCallback cb = (ICaptureCallback)o.obj;
            cb.onCapture(result, pData, nLength, new bitmapinfo(pHeader.biWidth, pHeader.biHeight));
        }
    }
    
    private static void OnEventCallback(int nEvent, int nPara, long pCallbackCtx, eventextra pExtra) {
        cbobj o = _cbmap.get((int)pCallbackCtx);
        if ((o != null) && (o.obj instanceof IEventCallback)) {
            IEventCallback cb = (IEventCallback)o.obj;
            if (cb != null)
                cb.onEvent(nEvent, nPara);
        }
    }
    
    private static class WinImplEventCallback implements WinLibrary.EVENT_CALLBACK {
        public void invoke(int nEvent, int nPara, long pCallbackCtx, eventextra pExtra) {
            OnEventCallback(nEvent, nPara, pCallbackCtx, pExtra);
        }
    }
    
    private static class CImplEventCallback implements CLibrary.EVENT_CALLBACK {
        public void invoke(int nEvent, int nPara, long pCallbackCtx, eventextra pExtra) {
            OnEventCallback(nEvent, nPara, pCallbackCtx, pExtra);
        }
    }
    
    /*
        get the version of this dll/so/dylib, which is: 5.33015.20261024
    */
    public static String Version() {
        return _lib.Toupnam_Version();
    }
    
    /*
        when toupnam.dll/libtoupnam.so discovery new camera, pCallback will be called.
        pCallback can be null if the application does not interest this.
        Init: call only once when application startup
        Fini: call only once when application exit
    */
    public static void Init(IEventCallback pCallback) {
        if (pCallback != null)
            _cbmap.put(0, new cbobj(pCallback, 0));
        if (Platform.isWindows()) {
            _callback_event = new WinImplEventCallback();
            Native.setCallbackThreadInitializer(_callback_event, new CallbackThreadInitializer(false, false, "EventCallback"));
            ((WinLibrary)_lib).Toupnam_Init((WinLibrary.EVENT_CALLBACK)_callback_event, 0);
        }
        else {
            _callback_event = new CImplEventCallback();
            Native.setCallbackThreadInitializer(_callback_event, new CallbackThreadInitializer(false, false, "EventCallback"));
            ((CLibrary)_lib).Toupnam_Init((CLibrary.EVENT_CALLBACK)_callback_event, 0);
        }
    }
    
    public static void Fini() {
        _lib.Toupnam_Fini();
    }
    
    /* enumerate the cameras discovered by the computer, return the number */
    public static device[] Enum() {
        int sz = MAX;
        do {
            _device[] arr = new _device[sz];
            int cnt = _lib.Toupnam_Enum(arr, MAX);
            if (cnt <= 0)
                return new device[0];
            if (cnt > sz)
                sz = cnt;
            else
            {
                device[] newarr = new device[cnt];
                for (int i = 0; i < cnt; ++i) {
                    newarr[i] = new device();
                    __convert_device(newarr[i], arr[i]);
                }
                return newarr;
            }
        } while (true);
    }
    
    /*
        the object of toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = new toupnam (The constructor is private on purpose)
    */
    public static toupnam Open(String camId) {
        Pointer h = _lib.Toupnam_Open(camId);
        if (h != null)
            return new toupnam(h);
        return null;
    }
    
    public static toupnam Open_ByIndex(int index) {
        Pointer h = _lib.Toupnam_Open_ByIndex(index);
        if (h != null)
            return new toupnam(h);
        return null;
    }
    
    private void OnDataCallback(Pointer pData, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
        Object o = _hash.get((int)pCallbackCtx);
        if (o instanceof toupnam) {
            toupnam t = (toupnam)o;
            if (t._cb_data != null)
                t._cb_data.onData(pData, new bitmapinfo(pHeader.biWidth, pHeader.biHeight));
        }
    }
    
    private void OnParaCallback(int para, int value, long pCallbackCtx) {
        Object o = _hash.get((int)pCallbackCtx);
        if (o instanceof toupnam) {
            toupnam t = (toupnam)o;
            if (t._cb_para != null)
                t._cb_para.onPara(para, value);
        }
    }
    
    public void StartPushMode(IDataCallback pDataCallback, IParaCallback pParaCallback) throws HRESULTException {
        _cb_data = pDataCallback;
        _cb_para = pParaCallback;
        if (Platform.isWindows()) {
            _callback_data = new WinLibrary.DATA_CALLBACK() {
                @Override
                public void invoke(Pointer pData, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
                    OnDataCallback(pData, pHeader, pCallbackCtx);
                }
            };
            _callback_para = new WinLibrary.PARA_CALLBACK() {
                @Override
                public void invoke(int para, int value, long pCallbackCtx) {
                    OnParaCallback(para, value, pCallbackCtx);
                }
            };
            Native.setCallbackThreadInitializer(_callback_data, new CallbackThreadInitializer(false, false, "DataCallback"));
            Native.setCallbackThreadInitializer(_callback_para, new CallbackThreadInitializer(false, false, "ParaCallback"));
            errCheck(((WinLibrary)_lib).Toupnam_StartPushMode(_handle, (WinLibrary.DATA_CALLBACK)_callback_data, (WinLibrary.PARA_CALLBACK)_callback_para, _objid));
        }
        else {
            _callback_data = new CLibrary.DATA_CALLBACK() {
                @Override
                public void invoke(Pointer pData, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
                    OnDataCallback(pData, pHeader, pCallbackCtx);
                }
            };
            _callback_para = new WinLibrary.PARA_CALLBACK() {
                @Override
                public void invoke(int para, int value, long pCallbackCtx) {
                    OnParaCallback(para, value, pCallbackCtx);
                }
            };
            Native.setCallbackThreadInitializer(_callback_data, new CallbackThreadInitializer(false, false, "DataCallback"));
            Native.setCallbackThreadInitializer(_callback_para, new CallbackThreadInitializer(false, false, "ParaCallback"));
            errCheck(((CLibrary)_lib).Toupnam_StartPushMode(_handle, (CLibrary.DATA_CALLBACK)_callback_data, (CLibrary.PARA_CALLBACK)_callback_para, _objid));
        }
    }
    
    public void StartPullModeWithWndMsg(Pointer hWnd, int nMsg) throws HRESULTException {
        if (Platform.isWindows())
            errCheck(((WinLibrary)_lib).Toupnam_StartPullModeWithWndMsg(_handle, hWnd, nMsg));
        else
            errCheck(HRESULTException.E_NOTIMPL);
    }

    /*
        bits: 24 (RGB24), 32 (RGB32), or 8 (Grey), see: TOUPNAM_PARA_FORMAT_LOCAL
        if RAW format, pnWidth = data size, pnHeight = not used
    */
    public void PullImage(ByteBuffer pImageData, int bits, bitmapinfo info) throws HRESULTException {
        if (pImageData.isDirect()) {
            IntByReference pw = new IntByReference();
            IntByReference ph = new IntByReference();
            errCheck(_lib.Toupnam_PullImage(_handle, Native.getDirectBufferPointer(pImageData), bits, pw, ph));
            info.width = pw.getValue();
            info.height = ph.getValue();
        }
        else if (pImageData.hasArray())
            PullImage(pImageData.array(), bits, info);
        else
            errCheck(HRESULTException.E_INVALIDARG);
    }

    /* Obtains a pointer to the frame buffer directly from the SDK, eliminating the need to copy frame data and thus improving performance */
    public void PullImagePtr(ImagePtr ptrImage) throws HRESULTException {
        errCheck(_lib.Toupnam_PullImagePtr(_handle, ptrImage));
    }

    /* After a frame buffer has been used, it must be returned to the SDK for reuse. Please note the following:
        (a) The frame buffer must only be returned back after it is no longer in use. Any access after returning it is unsafe, as the SDK may have already reused the buffer and overwritten the memory with new data.
        (b) If a frame buffer is not returned, the pool of available buffers will gradually decrease.
        (c) Each frame buffer obtained via Pull (identified by its ID) may be returned only once; duplicate returns are not allowed.
        (d) The return order does not need to match the Pull order; buffers may be returned out of order.
        (e) Frame buffers that have not been returned become invalid immediately after Close.
    */
    public void PushImagePtr(int ptrId) throws HRESULTException {
        errCheck(_lib.Toupnam_PushImagePtr(_handle, ptrId));
    }

    public void PullImage(byte[] pImageData, int bits, bitmapinfo info) throws HRESULTException {
        IntByReference pw = new IntByReference();
        IntByReference ph = new IntByReference();
        errCheck(_lib.Toupnam_PullImageArray(_handle, pImageData, bits, pw, ph));
        info.width = pw.getValue();
        info.height = ph.getValue();
    }
    
    public void StartPullModeWithCallback(IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, _objid));
        }
        int ret = _lib.Toupnam_StartPullModeWithCallback(_handle, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public void Stop() throws HRESULTException {
        errCheck(_lib.Toupnam_Stop(_handle));
    }
    
    public void Pause(boolean bPause) throws HRESULTException {
        errCheck(_lib.Toupnam_Pause(_handle, bPause ? 1 : 0));
    }
    
    private static void init_callback_capture() {
        if (_callback_capture == null) {
            if (Platform.isWindows()) {
                _callback_capture = new WinLibrary.CAPTURE_CALLBACK() {
                    @Override
                    public void invoke(int result, Pointer pData, long nLength, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
                        OnCaptureCallback(result, pData, nLength, pHeader, pCallbackCtx);
                    }
                };
            }
            else {
                _callback_capture = new CLibrary.CAPTURE_CALLBACK() {
                    @Override
                    public void invoke(int result, Pointer pData, long nLength, BITMAPINFOHEADER pHeader, long pCallbackCtx) {
                        OnCaptureCallback(result, pData, nLength, pHeader, pCallbackCtx);
                    }
                };
            }
            Native.setCallbackThreadInitializer(_callback_capture, new CallbackThreadInitializer(false, false, "CaptureCallback"));
        }
    }
    
    /* capture image, compare this to image extracted from video
       outputFile:
                NULL        -> capture image and then return by callback
                "raw"       -> capture raw image and then return by callback
                "abc.jpg"   -> capture image and then save it in the camera sd card with filename 'abc.jpg'
                "abc.raw"   -> capture raw image and then save it in the camera sd card with filename 'abc.raw'
                "thumbnail" -> capture the thumbnail image and then return by callback
                "*"         -> capture image and then save it in the camera sd card with auto generated file name
                "*.raw"     -> capture raw image and then save it in the camera sd card with auto generated file name
    */
    public void Capture(String outputFile, ICaptureCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, _objid));
        }
        int ret = 0;
        if (Platform.isWindows())
            ((WinLibrary)_lib).Toupnam_Capture(_handle, (outputFile + "\0").getBytes(StandardCharsets.UTF_8), (WinLibrary.CAPTURE_CALLBACK)_callback_capture, sid);
        else
            ((CLibrary)_lib).Toupnam_Capture(_handle, (outputFile + "\0").getBytes(StandardCharsets.UTF_8), (CLibrary.CAPTURE_CALLBACK)_callback_capture, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void Capture_ById(String camId, String outputFile, ICaptureCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = 0;
        if (Platform.isWindows())
            ((WinLibrary)_lib).Toupnam_Capture_ById(camId, (outputFile + "\0").getBytes(StandardCharsets.UTF_8), (WinLibrary.CAPTURE_CALLBACK)_callback_capture, sid);
        else
            ((CLibrary)_lib).Toupnam_Capture_ById(camId, (outputFile + "\0").getBytes(StandardCharsets.UTF_8), (CLibrary.CAPTURE_CALLBACK)_callback_capture, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public device get_Device() throws HRESULTException {
        _device dev = _lib.Toupnam_get_Device(_handle);
        device newdev = new device();
        __convert_device(newdev, dev);
        return newdev;
    }
    
    public bitmapinfo get_Size() throws HRESULTException {
        IntByReference pw = new IntByReference();
        IntByReference ph = new IntByReference();
        errCheck(_lib.Toupnam_get_Size(_handle, pw, ph));
        return new bitmapinfo(pw.getValue(), ph.getValue());
    }
    
    public bitmapinfo get_CapSize() throws HRESULTException {
        IntByReference pw = new IntByReference();
        IntByReference ph = new IntByReference();
        errCheck(_lib.Toupnam_get_CapSize(_handle, pw, ph));
        return new bitmapinfo(pw.getValue(), ph.getValue());
    }
    
    public int get_FourCC() throws HRESULTException {
        IntByReference p = new IntByReference();
        errCheck(_lib.Toupnam_get_FourCC(_handle, p));
        return p.getValue();
    }
    
    /*
        (fullpathname == null) means to stop record.
        support file extension: *.asf, *.mp4, *.mkv
    */
    public void Record(String outputFile) throws HRESULTException {
        if (outputFile == null)
            errCheck(_lib.Toupnam_Record(_handle, null));
        else
            errCheck(_lib.Toupnam_Record(_handle, (outputFile + "\0").getBytes(StandardCharsets.UTF_8)));
    }
    
    /* para is one of PARA_xxx */
    public void put_Para(int para, int value) throws HRESULTException {
        errCheck(_lib.Toupnam_put_Para(_handle, para, value));
    }
    
    public int get_Para(int para) throws HRESULTException {
        IntByReference p = new IntByReference();
        errCheck(_lib.Toupnam_get_Para(_handle, para, p));
        return p.getValue();
    }
    
    public static void put_Wifi(String camId, wifi wf) throws HRESULTException {
        _wifi p = new _wifi();
        copyByteArray(p.ssid, wf.ssid);
        copyByteArray(p.password, wf.password);
        errCheck(_lib.Toupnam_put_Wifi(camId, p));
    }
    
    public static void get_Wifi(String camId) throws HRESULTException {
        errCheck(_lib.Toupnam_get_Wifi(camId));
    }
    
    public static void list_Wifi(String camId, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_list_Wifi(camId, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void list_Dir(String camId, String path, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_list_Dir(camId, (path + "\0").getBytes(StandardCharsets.UTF_8), sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void get_Thumbnail(String camId, String path, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_get_Thumbnail(camId, (path + "\0").getBytes(StandardCharsets.UTF_8), sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    /* del file or directory, rename file or directory */
    public static void change_Dir(String camId, String path, dirchange[] dc) throws HRESULTException {
        _dirchange[] arr = new _dirchange[dc.length];
        for (int i = 0; i < dc.length; ++i)
        {
            arr[i].type = dc[i].type;
            copyByteArray(arr[i].name, dc[i].name);
            copyByteArray(arr[i].newname, dc[i].newname);
        }
        errCheck(_lib.Toupnam_change_Dir(camId, (path + "\0").getBytes(StandardCharsets.UTF_8), arr, dc.length));
    }
    
    /* record to the sd card */
    public static void RecordStart(String camId, String outputFile, int recordtime, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_RecordStart(camId, (outputFile + "\0").getBytes(StandardCharsets.UTF_8), recordtime, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    /* stop record to the sd card */
    public static void RecordStop(String camId, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_RecordStop(camId, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void get_DateTime(String camId, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_get_DateTime(camId, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void put_DateTime(String camId, long t, IEventCallback pCallback) throws HRESULTException {
        int sid = 0;
        if (pCallback != null)
        {
            sid = _clsid.incrementAndGet();
            _cbmap.put(sid, new cbobj(pCallback, 0));
        }
        int ret = _lib.Toupnam_put_DateTime(camId, t, sid);
        if ((ret < 0) && (pCallback != null))
            _cbmap.remove(sid);
        errCheck(ret);
    }
    
    public static void put_Para_ById(String camId, int para, int value) throws HRESULTException {
        errCheck(_lib.Toupnam_put_Para_ById(camId, para, value));
    }
    
    public static int get_Para_ById(String camId, int para) throws HRESULTException {
        IntByReference p = new IntByReference();
        errCheck(_lib.Toupnam_get_Para_ById(camId, para, p));
        return p.getValue();
    }
    
    public static bitmapinfo get_Size_ById(String camId, int res) throws HRESULTException {
        IntByReference pw = new IntByReference();
        IntByReference ph = new IntByReference();
        errCheck(_lib.Toupnam_get_Size_ById(camId, res, pw, ph));
        return new bitmapinfo(pw.getValue(), ph.getValue());
    }
    
    public static void PriFlag(int nFlag, int nMask) {
        _lib.Toupnam_PriFlag(nFlag, nMask);
    }
    
    /* arr = { "1.2.3.4", "1.2.3.5", ... } */
    public static void add_Ip(String[] arr) {
        Memory[] marr = new Memory[arr.length + 1];
        for (int i = 0; i < arr.length; ++i) {
            marr[i] = new Memory(arr[i].length() + 1);
            marr[i].setString(0, arr[i], "ascii");
        }
        _lib.Toupnam_add_Ip(marr);
    }
    
    public static void del_Ip(String[] arr) {
        Memory[] marr = new Memory[arr.length + 1];
        for (int i = 0; i < arr.length; ++i) {
            marr[i] = new Memory(arr[i].length() + 1);
            marr[i].setString(0, arr[i], "ascii");
        }
        _lib.Toupnam_del_Ip(marr);
    }
}
