using System;
using System.Runtime.InteropServices;
using Microsoft.Win32.SafeHandles;
#if !(NETFX_CORE || WINDOWS_UWP)
using System.Security.Permissions;
using System.Runtime.ConstrainedExecution;
#endif
using System.Collections.Generic;
using System.Threading;
using System.Text;

/*
    Version: 5.33015.20261024
    For Microsoft dotNET Framework & dotNet Core
    We use P/Invoke to call into the toupnam.dll/so API, the c# class Toupnam is a thin wrapper class to the native api of toupnam.dll/so.
*/

internal class Toupnam : IDisposable
{
    public const uint MAX = 64;
    
    public const uint PARA_UNKNOWN                = 0x00;
    public const uint PARA_EXPOTIME               = 0x01;    /* exposure time */
    public const uint PARA_AGAIN                  = 0x02;    /* gain */
    public const uint PARA_AEXPOTARGET            = 0x03;    /* auto exposure target */
    public const uint PARA_TEMP                   = 0x04;    /* color temperature */
    public const uint PARA_TINT                   = 0x05;
    public const uint PARA_CONTRAST               = 0x06;    /* contrast */
    public const uint PARA_HUE                    = 0x07;    /* hue */
    public const uint PARA_SATURATION             = 0x08;    /* saturation */
    public const uint PARA_BRIGHTNESS             = 0x09;    /* brightness */
    public const uint PARA_GAMMA                  = 0x0a;    /* gamma */
    public const uint PARA_AEXPO                  = 0x0b;    /* auto exposure */
    public const uint PARA_AWB                    = 0x0c;    /* XCAM1080P:once;  XCAM4K:(0:manual;1:global auto;2:roi) */
    public const uint PARA_BINSKIP                = 0x0d;    /* bin / skip */
    public const uint PARA_HZ                     = 0x0e;    /* power supply: 0 -> 60HZ AC;  1 -> 50Hz AC;   2 -> DC */
    public const uint PARA_BPS                    = 0x0f;    /* bits per second, kbps */
    public const uint PARA_KEYFRAME               = 0x10;    /* key frame interval */
    public const uint PARA_LOWLIGHTCOMPENSATION   = 0x11;    /* low light compensation */
    public const uint PARA_SHARPNESS              = 0x12;    /* sharpness */
    public const uint PARA_WBREDGAIN              = 0x13;    /* white balance red gain */
    public const uint PARA_WBGREENGAIN            = 0x14;    /* white balance green gain */
    public const uint PARA_WBBLUEGAIN             = 0x15;    /* white balance blue gain */
    public const uint PARA_DENOISE                = 0x16;    /* denoise */
    public const uint PARA_APSTA                  = 0x17;    /* ap/sta */
    public const uint PARA_CODEC                  = 0x18;    /* codec, H264, H265, etc */
    public const uint PARA_AFPOSITION             = 0x19;    /* auto focus sensor board positon (applicable to both AFDM and EFL) */
    public const uint PARA_AFMODE                 = 0x1a;    /* auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration) */
    public const uint PARA_AFZONE                 = 0x1b;    /* auto focus zone:
                                                                  the whole resolution is divided in w * h zones:
                                                                    w = imax >> 16
                                                                    h = imax & 0xffff
                                                             */
    public const uint PARA_AFFEEDBACK             = 0x1c;    /* auto focus information feedback
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
    public const uint PARA_AFPOSITION_ABSOLUTE    = 0x1d;    /* absolute auto focus sensor board positon */
    public const uint PARA_STATUS                 = 0x1e;    /* status */
    public const uint PARA_EVENT                  = 0x1f;    /* event */
    public const uint PARA_WBROILEFT              = 0x20;    /* white balance roi left */
    public const uint PARA_WBROITOP               = 0x21;    /* white balance roi top */
    public const uint PARA_WBROIWIDTH             = 0x22;    /* white balance roi width */
    public const uint PARA_WBROIHEIGHT            = 0x23;    /* white balance roi height */
    public const uint PARA_VFLIP                  = 0x24;    /* vertical flip */
    public const uint PARA_HFLIP                  = 0x25;    /* horizontal flip */
    public const uint PARA_CHROME                 = 0x26;    /* monochromatic mode */
    public const uint PARA_SIZE                   = 0x27;    /* video width & height */
    public const uint PARA_LIGHTADJUSTMENT        = 0x28;    /* light source brightness adjustment */
    public const uint PARA_ZOOM                   = 0x29;    /* zoom */
    public const uint PARA_EF_MODE                = 0x2a;    /* EFL, auto focus mode, 0: lens calibration mode; 1: manual focus; 2: single focus; 3: auto focus; */
    public const uint PARA_EF_FL                  = 0x2b;    /* EFL, focal length */
    public const uint PARA_EF_APERTURE            = 0x2c;    /* EFL, F#, 24~16bit:Cur, 15~8bit:Min, 7~0bit:Max */
    public const uint PARA_EF_FOCUS_MAX           = 0x2d;    /* EFL, maximum focal length */
    public const uint PARA_EF_LENS_ID             = 0x2e;    /* EFL, lens ID */
    public const uint PARA_EF_AFMF                = 0x2f;    /* EFL, lens focus mode, 0: AF; 1: MF; */
    public const uint PARA_EF_WD_ENABLE           = 0x30;    /* EFL, enable working distance function */
    public const uint PARA_EF_WD_NEAR             = 0x31;    /* EFL, near working distance point */
    public const uint PARA_EF_WD_FAR              = 0x32;    /* EFL, far working distance point */
    
    public const uint PARA_CHROME_LOCAL           = 0x80;    /* local monochromatic mode */
    public const uint PARA_VFLIP_LOCAL            = 0x81;    /* local vertical flip */
    public const uint PARA_HFLIP_LOCAL            = 0x82;    /* local horizontal flip */
    public const uint PARA_NEGATIVE_LOCAL         = 0x83;    /* local negative film */
    public const uint PARA_FORMAT_LOCAL           = 0x84;    /* output format: 0 => BGR888, 1 => BGRA8888, 2 => RGB888, 3 => RGBA8888, 4 => RAW; default: 0
                                                                MUST be set BEFORE StartXXXX
                                                             */
    
    public const uint PARA_RETRY                  = 0xe0;
    
    public const uint PARA_STATUS_RECORDING       = 0x00000001;      /* recording */
    public const uint PARA_STATUS_SD              = 0x00000002;      /* sd card available */
    public const uint PARA_STATUS_SD_FULL         = 0x00000004;      /* sd card full */
    
    public const uint PARA_EVENT_FAT4G            = 0x00000001;      /* file size limit 4g in FAT32 */
    
    public const uint STATE_INITING               = 0x00;    /* initialization */
    public const uint STATE_NORMAL                = 0x01;    /* normal */
    public const uint STATE_UNREACHABLE           = 0x02;    /* network not reachable */
    
    public const uint FLAG_WIFI_AP                = 0x00000001;
    public const uint FLAG_WIFI_STA               = 0x00000002;
    public const uint FLAG_ETHERNET               = 0x00000004;
    public const uint FLAG_CAPTURE                = 0x00000008;  /* support the ability of capture image from camera */
    public const uint FLAG_AWBCHECKMODE           = 0x00000010;  /* auto white balance: check mode vs 'once' mode */
    public const uint FLAG_UVC                    = 0x00000020;  /* uvc camera */
    public const uint FLAG_WBGAIN                 = 0x00000040;  /* white balance gain mode or temp tint mode */
    public const uint FLAG_MULTICAST              = 0x00000080;  /* RTSP/RTP multicast */
    public const uint FLAG_AF                     = 0x00000100;  /* support auto focus */
    public const uint FLAG_SD_LIST                = 0x00000200;  /* support to list sd card */
    public const uint FLAG_SD                     = 0x00000400;  /* support sd card */
    public const uint FLAG_WBROI                  = 0x00000800;  /* white balance: 0:manual;1:global auto;2:roi */
    public const uint FLAG_STA_SUPPORT            = 0x00001000;  /* wifi camera has sta mode, app should have sta ssid & password function */
    public const uint FLAG_RTP_OVER_RTSP          = 0x00002000;  /* rtp over rtsp */
    public const uint FLAG_HZ_AUTOEXPO            = 0x00004000;  /* enable auto exposure when 50/60 hz */
    public const uint FLAG_AFDM                   = 0x00008000;
    public const uint FLAG_EFL                    = 0x00010000;
    public const uint FLAG_CAPTURERAW             = 0x00020000;  /* capture raw image */
    
    public const uint EVENT_ENUM                  = 0x01;    /* enum */
    public const uint EVENT_WIFI                  = 0x02;    /* wifi */
    public const uint EVENT_PARA                  = 0x03;    /* parameter change TOUPNAM_PARA_xxxx */
    public const uint EVENT_IMAGE                 = 0x04;    /* image */
    public const uint EVENT_LISTWIFI              = 0x05;    /* list wifi finished */
    public const uint EVENT_LISTDIR               = 0x07;    /* list dir */
    public const uint EVENT_THUMBNAIL             = 0x08;    /* thumbnail */
    public const uint EVENT_DIRCHANGE             = 0x09;    /* dir change notify */
    public const uint EVENT_RECORDSTART           = 0x0a;    /* record start */
    public const uint EVENT_RECORDSTOP            = 0x0b;    /* record stop */
    public const uint EVENT_DATETIME              = 0x0c;    /* date time */
    public const uint EVENT_ERROR                 = 0x80;    /* error */
    public const uint EVENT_EOF                   = 0x81;    /* end of file */
    
    [StructLayout(LayoutKind.Sequential)]
    public struct range
    {
        public int idisable;   /* 0 = "support this feature", 1 = "not support" */
        public int imin;       /* minimum value */
        public int imax;       /* maximum value */
        public int idef;       /* default value */
    };
    
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct device
    {
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  id;         /* unique camera id, used for Open */
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  sn;         /* serial number */
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  name;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  model;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  version;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  addr;       /* ip */
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string  url;       /* playback url, such as rtsp://xxxx/yyyy */
        public uint    state;     /* STATE_xxx */
        public uint    flag;      /* FLAG_xxx */
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = (int)MAX)]
        public range[] r;
    };

    [StructLayout(LayoutKind.Sequential)]
    public struct imageptr
    {
        public uint id;        /* 0 is reserved as an invalid id */
        public uint width;     /* image width */
        public uint height;    /* image height */
        public uint length;    /* buf length */
        public IntPtr buf;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct wifi
    {
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  ssid;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string  password;
    };
    
    [StructLayout(LayoutKind.Sequential)]
    public struct eventextra
    {
        public int     result;
        public uint    length;
        public IntPtr  ptr;
        public IntPtr  ctx;
    };
    
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct diritem
    {
        public uint   type;         /* 0 => file, 1 => directory */
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string name;         /* download the file with the url http://addr/path/name
                                        For example, Camera's ip is 192.168.1.2, and file in the sd card directory abc/xyz.mp4, then the url is http://192.168.1.2/abc/xyz.mp4
                                        So, it can be downloaded from this url with libcurl or WinInet.
                                    */
    };
    
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
    public struct dirchange
    {
        public uint     type;       /* 0 => add, 1 => del, 2 => rename */
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string   name;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string   newname;
    };
    
    public struct bitmapinfo
    {
        public int width;
        public int height;
        public bitmapinfo(int w, int h)
        {
            width = w;
            height = h;
        }
    }
    
    [StructLayout(LayoutKind.Sequential)]
    private struct BITMAPINFOHEADER
    {
        public uint    biSize;
        public int     biWidth;
        public int     biHeight;
        public ushort  biPlanes;
        public ushort  biBitCount;
        public uint    biCompression;
        public uint    biSizeImage;
        public int     biXPelsPerMeter;
        public int     biYPelsPerMeter;
        public uint    biClrUsed;
        public uint    biClrImportant;
    };
    
    /*
        when frame arrive, DelegateEventCallback is callbacked. (NULL == pData) means that something is error.
        when PARA_xxx value is changed, DelegateParaCallback is callbacked.
        pCallbackCtx is the callback context which is passed by Start.
        DelegateEventCallback and DelegateParaCallback are callbacked by an internal thread of toupnam.dll, so please pay attention to multithread problem.
    */
    public delegate void DelegateEventCallback(uint nEvent, uint nPara, ref eventextra pExtra);
    public delegate void DelegateParaCallback(uint para, int val);
    public delegate void DelegateDataCallback(IntPtr pData, ref bitmapinfo info);
    public delegate void DelegateCaptureCallback(int result, IntPtr pData, int nLength, ref bitmapinfo info);
        
    [DllImport("ntdll.dll", ExactSpelling = true, CallingConvention = CallingConvention.Cdecl, SetLastError = false)]
    public static extern void memcpy(IntPtr dest, IntPtr src, IntPtr count);

    public static int TDIBWIDTHBYTES(int bits)
    {
        return ((bits + 31) & (~31)) / 8;
    }
    
    public void Close()
    {
        Dispose();
    }

    public void Dispose()  // Follow the Dispose pattern - public nonvirtual.
    {
        Dispose(true);
        map_.Remove(id_);
        GC.SuppressFinalize(this);
    }

    /*
        get the version of this dll/so/dylib, which is: 5.33015.20261024
    */
    public static string Version()
    {
        return Toupnam_Version();
    }
    
    /*
        when toupnam.dll/libtoupnam.so discovery new camera, pCallback will be called.
        pCallback can be null if the application does not interest this.
        Init: call only once when application startup
        Fini: call only once when application exit
    */
    public static void Init(DelegateEventCallback pCallback)
    {
        if (pCallback != null)
            dic_.Add(0, new cbobj(pCallback, 0));
        Toupnam_Init(event_callback_, IntPtr.Zero);
    }
    
    public static void Fini()
    {
        Toupnam_Fini();
    }
    
    /* enumerate the cameras discovered by the computer, return the number
        sz: size of the array
        when sz is too small, return value will be greater than sz, means that the caller must use bigger array
    */
    public static device[] Enum()
    {
        int sz = (int)MAX;
        do
        {
            IntPtr ti = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(device)) * sz);
            int n = Toupnam_Enum(ti, sz);
            if (n > sz)
            {
                Marshal.FreeHGlobal(ti);
                sz = (int)n;
            }
            else if (0 == n)
            {
                Marshal.FreeHGlobal(ti);
                return new device[0];
            }
            else
            {
                device[] arr = new device[n];
                for (int i = 0; i < n; ++i)
#if !(NETFX_CORE || WINDOWS_UWP)
                    arr[i] = (device)Marshal.PtrToStructure(ti + Marshal.SizeOf(typeof(device)) * i, typeof(device));
#else
                    arr[i] = Marshal.PtrToStructure<device>(ti + Marshal.SizeOf(typeof(device)) * i);
#endif
                Marshal.FreeHGlobal(ti);
                return arr;
            }
        } while (true);
    }
    
    /*
        the object of Toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = new Toupnam (The constructor is private on purpose)
    */
    public static Toupnam Open(string camId)
    {
        SafeCamHandle h = Toupnam_Open(camId);
        if (h == null || h.IsInvalid || h.IsClosed)
            return null;
        return new Toupnam(h);
    }
    
    public static Toupnam Open_ByIndex(uint index)
    {
        SafeCamHandle h = Toupnam_Open_ByIndex(index);
        if (h.IsInvalid)
            return null;
        return new Toupnam(h);
    }
    
    public int StartPushMode(DelegateDataCallback pDataCallback, DelegateParaCallback pParaCallback)
    {
        datadele_ = pDataCallback;
        paradele_ = pParaCallback;
        datacallback_ = delegate (IntPtr pData, ref BITMAPINFOHEADER pHeader, IntPtr pCallbackCtx)
        {
            Toupnam pthis = null;
            if (map_.TryGetValue(pCallbackCtx.ToInt32(), out pthis) && (pthis != null) && (pthis.datadele_ != null))
            {
                bitmapinfo info = new bitmapinfo(pHeader.biWidth, pHeader.biHeight);
                pthis.datadele_(pData, ref info);
            }
        };
        paracallback_ = delegate (uint para, int val, IntPtr pCallbackCtx)
        {
            Toupnam pthis = null;
            if (map_.TryGetValue(pCallbackCtx.ToInt32(), out pthis) && (pthis != null) && (pthis.paradele_ != null))
                pthis.paradele_(para, val);
        };
        return Toupnam_StartPushMode(handle_, datacallback_, paracallback_, new IntPtr(id_));
    }
    
    public int StartPullModeWithWndMsg(IntPtr hWnd, uint nMsg)
    {
        return Toupnam_StartPullModeWithWndMsg(handle_, hWnd, nMsg);
    }

    /* Obtains a pointer to the frame buffer directly from the SDK, eliminating the need to copy frame data and thus improving performance */
    public int PullImagePtr(out imageptr ptrImage)
    {
        return Toupnam_PullImagePtr(handle_, out ptrImage);
    }

    /* After a frame buffer has been used, it must be returned to the SDK for reuse. Please note the following:
        (a) The frame buffer must only be returned back after it is no longer in use. Any access after returning it is unsafe, as the SDK may have already reused the buffer and overwritten the memory with new data.
        (b) If a frame buffer is not returned, the pool of available buffers will gradually decrease.
        (c) Each frame buffer obtained via Pull (identified by its ID) may be returned only once; duplicate returns are not allowed.
        (d) The return order does not need to match the Pull order; buffers may be returned out of order.
        (e) Frame buffers that have not been returned become invalid immediately after Close.
    */
    public int PushImagePtr(uint ptrId)
    {
        return Toupnam_PushImagePtr(handle_, ptrId);
    }

    /*
        bits: 24 (RGB24), 32 (RGB32), or 8 (Grey), see: TOUPNAM_PARA_FORMAT_LOCAL
        if RAW format, pnWidth = data size, pnHeight = not used
    */
    public int PullImage(IntPtr pImageData, int bits, out uint pnWidth, out uint pnHeight)
    {
        return Toupnam_PullImage(handle_, pImageData, bits, out pnWidth, out pnHeight);
    }

    public int PullImage(byte[] pImageData, int bits, out uint pnWidth, out uint pnHeight)
    {
        return Toupnam_PullImage(handle_, pImageData, bits, out pnWidth, out pnHeight);
    }

    public int StartPullModeWithCallback(DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, id_));
        }
        int ret = Toupnam_StartPullModeWithCallback(handle_, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public int Stop()
    {
        return Toupnam_Stop(handle_);
    }
    
    public int Pause(bool bPause)
    {
        return Toupnam_Pause(handle_, bPause ? 1 : 0);
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
    public int Capture(string outputFile, DelegateCaptureCallback pCaptureCallback)
    {
        int sid = 0;
        if (pCaptureCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCaptureCallback, id_));
        }
        if (capture_callback_ == null)
            capture_callback_ = new CAPTURE_CALLBACK(OnCaptureCallback);
        int ret = Toupnam_Capture(handle_, Encoding.UTF8.GetBytes(outputFile + "\0"), capture_callback_, new IntPtr(sid));
        if ((ret < 0) && (pCaptureCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public static int Capture_ById(string camId, string outputFile, DelegateCaptureCallback pCaptureCallback)
    {
        int sid = 0;
        if (pCaptureCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCaptureCallback, 0));
        }
        if (capture_callback_ == null)
            capture_callback_ = new CAPTURE_CALLBACK(OnCaptureCallback);
        int ret = Toupnam_Capture_ById(camId, Encoding.UTF8.GetBytes(outputFile + "\0"), capture_callback_, new IntPtr(sid));
        if ((ret < 0) && (pCaptureCallback != null))
            dic_.Remove(sid);
        return ret;
    }

    public device get_Device()
    {
        device dev = new device();
        IntPtr ptr = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(device)));
        if (Toupnam_query_Device(handle_, ptr) >= 0)
        {
#if !(NETFX_CORE || WINDOWS_UWP)
            dev = (device)Marshal.PtrToStructure(ptr, typeof(device));
#else
            dev = Marshal.PtrToStructure<device>(ptr);
#endif
        }
        Marshal.FreeHGlobal(ptr);
        return dev;
    }
    
    public int get_Size(out int pWidth, out int pHeight)
    {
        return Toupnam_get_Size(handle_, out pWidth, out pHeight);
    }
    
    public int get_CapSize(out int pWidth, out int pHeight)
    {
        return Toupnam_get_CapSize(handle_, out pWidth, out pHeight);
    }
    
    public int get_FourCC(out uint pFourCC)
    {
        return Toupnam_get_FourCC(handle_, out pFourCC);
    }
    
    /*
        (outputFile == null) means to stop record.
        support file extension: *.asf, *.mp4, *.mkv
    */
    public int Record(string outputFile)
    {
        if (string.IsNullOrEmpty(outputFile))
            return Toupnam_Record(handle_, null);
        else
            return Toupnam_Record(handle_, Encoding.UTF8.GetBytes(outputFile + "\0"));
    }
    
    /* para is one of PARA_xxx */
    public int put_Para(uint para, int val)
    {
        return Toupnam_put_Para(handle_, para, val);
    }
    
    public int get_Para(uint para, out int val)
    {
        return Toupnam_get_Para(handle_, para, out val);
    }
    
    public static int put_Wifi(string camId, wifi wf)
    {
        return Toupnam_put_Wifi(camId, wf);
    }
    
    public static int get_Wifi(string camId)
    {
        return Toupnam_get_Wifi(camId);
    }
    
    public static int list_Wifi(string camId, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_list_Wifi(camId, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public static int list_Dir(string camId, string path, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_list_Dir(camId, Encoding.UTF8.GetBytes(path + "\0"), new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public static int get_Thumbnail(string camId, string path, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_get_Thumbnail(camId, Encoding.UTF8.GetBytes(path + "\0"), new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    /* del file or directory, rename file or directory */
    public static int change_Dir(string camId, string path, dirchange[] dc)
    {
        return Toupnam_change_Dir(camId, Encoding.UTF8.GetBytes(path + "\0"), dc, dc.Length);
    }
    
    /* record to the sd card */
    public static int RecordStart(string camId, string outputFile, uint recordtime, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_RecordStart(camId, Encoding.UTF8.GetBytes(outputFile + "\0"), recordtime, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    /* stop record to the sd card */
    public static int RecordStop(string camId, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_RecordStop(camId, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public static int get_DateTime(string camId, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_get_DateTime(camId, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    public static int put_DateTime(string camId, long t, DelegateEventCallback pCallback)
    {
        int sid = 0;
        if (pCallback != null)
        {
            sid = Interlocked.Increment(ref sid_);
            dic_.Add(sid, new cbobj(pCallback, 0));
        }
        int ret = Toupnam_put_DateTime(camId, t, new IntPtr(sid));
        if ((ret < 0) && (pCallback != null))
            dic_.Remove(sid);
        return ret;
    }
    
    /* para is one of PARA_XXX */
    public static int put_Para_ById(string camId, uint para, int val)
    {
        return Toupnam_put_Para_ById(camId, para, val);
    }
    
    public static int get_Para_ById(string camId, uint para, out int val)
    {
        return Toupnam_get_Para_ById(camId, para, out val);
    }
    
    public static int get_Size_ById(string camId, int res, out int pWidth, out int pHeight)
    {
        return Toupnam_get_Size_ById(camId, res, out pWidth, out pHeight);
    }
    
    public static void PriFlag(uint nFlag, uint nMask)
    {
        Toupnam_PriFlag(nFlag, nMask);
    }
    
    /* arr = { "1.2.3.4", "1.2.3.5", ... } */
    public static void add_Ip(string[] arr)
    {
        IntPtr[] newarr = new IntPtr[arr.Length + 1];
        for (int i = 0; i < arr.Length; ++i)
            newarr[i] = Marshal.StringToHGlobalAnsi(arr[i]);
        Toupnam_add_Ip(newarr);
        for (int i = 0; i < arr.Length; ++i)
            Marshal.FreeHGlobal(newarr[i]);
    }
    
    public static void del_Ip(string[] arr)
    {
        IntPtr[] newarr = new IntPtr[arr.Length + 1];
        for (int i = 0; i < arr.Length; ++i)
            newarr[i] = Marshal.StringToHGlobalAnsi(arr[i]);
        Toupnam_del_Ip(newarr);
        for (int i = 0; i < arr.Length; ++i)
            Marshal.FreeHGlobal(newarr[i]);
    }

    private class cbobj
    {
        public Delegate de;
        public int id;
        public cbobj(Delegate d, int i)
        {
            de = d;
            id = i;
        }
    }

    private static int sid_ = 0;
    private static EVENT_CALLBACK event_callback_ = new EVENT_CALLBACK(OnEventCallback);
    private static CAPTURE_CALLBACK capture_callback_;
    private static Dictionary<int, cbobj> dic_ = new Dictionary<int, cbobj>();
    private static Dictionary<int, Toupnam> map_ = new Dictionary<int, Toupnam>();

    private SafeCamHandle handle_;
    private int id_;
    private DelegateParaCallback paradele_;
    private DelegateDataCallback datadele_;
    private PARA_CALLBACK paracallback_;
    private DATA_CALLBACK datacallback_;

    /*
        the object of Toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = new Toupnam (The constructor is private on purpose)
    */
    private Toupnam(SafeCamHandle h)
    {
        handle_ = h;
        id_ = Interlocked.Increment(ref sid_);
        map_.Add(id_, this);
    }

    ~Toupnam()
    {
        Dispose(false);
    }

#if !(NETFX_CORE || WINDOWS_UWP)
    [SecurityPermission(SecurityAction.Demand, UnmanagedCode = true)]
#endif
    protected virtual void Dispose(bool disposing)
    {
        // Note there are three interesting states here:
        // 1) CreateFile failed, _handle contains an invalid handle
        // 2) We called Dispose already, _handle is closed.
        // 3) _handle is null, due to an async exception before
        //    calling CreateFile. Note that the finalizer runs
        //    if the constructor fails.
        if (handle_ != null && !handle_.IsInvalid)
        {
            // Free the handle
            handle_.Dispose();
        }
        // SafeHandle records the fact that we've called Dispose.
    }

#if !(WINDOWS_UWP)
    public class SafeCamHandle : SafeHandleZeroOrMinusOneIsInvalid
    {
        [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
        private static extern void Toupnam_Close(IntPtr h);

        public SafeCamHandle()
            : base(true)
        {
        }

        [ReliabilityContract(Consistency.WillNotCorruptState, Cer.MayFail)]
        override protected bool ReleaseHandle()
        {
            // Here, we must obey all rules for constrained execution regions.
            Toupnam_Close(handle);
            return true;
        }
    };
#else
    public class SafeCamHandle : SafeHandle
    {
        [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
        private static extern void Toupnam_Close(IntPtr h);
        
        public SafeCamHandle()
            : base(IntPtr.Zero, true)
        {
        }
        
        override protected bool ReleaseHandle()
        {
            Toupnam_Close(handle);
            return true;
        }
        
        public override bool IsInvalid
        {
            get { return base.handle == IntPtr.Zero || base.handle == (IntPtr)(-1); }
        }
    };
#endif

    private static void OnCaptureCallback(int result, IntPtr pData, IntPtr nLength, ref BITMAPINFOHEADER pHeader, IntPtr pCallbackCtx)
    {
        cbobj d = null;
        if (dic_.TryGetValue(pCallbackCtx.ToInt32(), out d) && (d != null))
        {
            DelegateCaptureCallback e = d.de as DelegateCaptureCallback;
            if (e != null)
            {
                bitmapinfo info = new bitmapinfo(pHeader.biWidth, pHeader.biHeight);
                e(result, pData, nLength.ToInt32(), ref info);
            }
        }
    }

    private static void OnEventCallback(uint nEvent, uint nPara, IntPtr pCallbackCtx, ref eventextra pExtra)
    {
        cbobj d = null;
        if (dic_.TryGetValue(pCallbackCtx.ToInt32(), out d) && (d != null))
        {
            DelegateEventCallback e = d.de as DelegateEventCallback;
            if (e != null)
                e(nEvent, nPara, ref pExtra);
        }
    }

    [UnmanagedFunctionPointer(CallingConvention.Winapi)]
    private delegate void EVENT_CALLBACK(uint nEvent, uint nPara, IntPtr pCallbackCtx, ref eventextra pExtra);
    [UnmanagedFunctionPointer(CallingConvention.Winapi)]
    private delegate void PARA_CALLBACK(uint para, int val, IntPtr pCallbackCtx);
    [UnmanagedFunctionPointer(CallingConvention.Winapi)]
    private delegate void DATA_CALLBACK(IntPtr pData, ref BITMAPINFOHEADER pHeader, IntPtr pCallbackCtx);
    [UnmanagedFunctionPointer(CallingConvention.Winapi)]
    private delegate void CAPTURE_CALLBACK(int result, IntPtr pData, IntPtr nLength, ref BITMAPINFOHEADER pHeader, IntPtr pCallbackCtx);

    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    [return: MarshalAs(UnmanagedType.LPStr)]
    private static extern string Toupnam_Version();
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_Init(EVENT_CALLBACK pCallback, IntPtr pCallbackCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_Fini();
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Enum(IntPtr arr, int sz);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern SafeCamHandle Toupnam_Open([MarshalAs(UnmanagedType.LPStr)] string camId);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern SafeCamHandle Toupnam_Open_ByIndex(uint index);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_Close(SafeCamHandle h);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_StartPushMode(SafeCamHandle h, DATA_CALLBACK pDataCallback, PARA_CALLBACK pParaCallback, IntPtr pCallbackCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_StartPullModeWithWndMsg(SafeCamHandle h, IntPtr hWnd, uint nMsg);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_PullImage(SafeCamHandle h, IntPtr pImageData, int bits, out uint pnWidth, out uint pnHeight);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_PullImage(SafeCamHandle h, byte[] pImageData, int bits, out uint pnWidth, out uint pnHeight);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_PullImagePtr(SafeCamHandle h, out imageptr ptrImage);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_PushImagePtr(SafeCamHandle h, uint ptrId);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_StartPullModeWithCallback(SafeCamHandle h, IntPtr pCallbackContext);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Stop(SafeCamHandle h);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Pause(SafeCamHandle h, int bPause);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Capture(SafeCamHandle h, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, IntPtr pCallbackCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Capture_ById([MarshalAs(UnmanagedType.LPStr)] string camId, byte[] outputFile, CAPTURE_CALLBACK pCaptureCallback, IntPtr pCallbackCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_query_Device(SafeCamHandle h, IntPtr ptr);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Size(SafeCamHandle h, out int pWidth, out int pHeight);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_CapSize(SafeCamHandle h, out int pWidth, out int pHeight);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_FourCC(SafeCamHandle h, out uint pFourCC);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_Record(SafeCamHandle h, byte[] outputFile);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_put_Para(SafeCamHandle h, uint para, int val);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Para(SafeCamHandle h, uint para, out int val);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_put_Wifi([MarshalAs(UnmanagedType.LPStr)] string camId, wifi wf);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Wifi([MarshalAs(UnmanagedType.LPStr)] string camId);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_list_Wifi([MarshalAs(UnmanagedType.LPStr)] string camId, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_list_Dir([MarshalAs(UnmanagedType.LPStr)] string camId, byte[] path, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Thumbnail([MarshalAs(UnmanagedType.LPStr)] string camId, byte[] path, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_change_Dir([MarshalAs(UnmanagedType.LPStr)] string camId, byte[] path, dirchange[] dc, int dclength);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_RecordStart([MarshalAs(UnmanagedType.LPStr)] string camId, byte[] outputFile, uint recordtime, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_RecordStop([MarshalAs(UnmanagedType.LPStr)] string camId, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_DateTime([MarshalAs(UnmanagedType.LPStr)] string camId, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_put_DateTime([MarshalAs(UnmanagedType.LPStr)] string camId, long t, IntPtr pExtraCtx);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_put_Para_ById([MarshalAs(UnmanagedType.LPStr)] string camId, uint para, int val);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Para_ById([MarshalAs(UnmanagedType.LPStr)] string camId, uint para, out int val);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_get_Size_ById([MarshalAs(UnmanagedType.LPStr)] string camId, int res, out int pWidth, out int pHeight);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_PriFlag(uint nFlag, uint nMask);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_add_Ip(IntPtr[] arr);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern void Toupnam_del_Ip(IntPtr[] arr);
}

internal class EncVideo : IDisposable
{
    /*
        the object of EncVideo must be obtained by static mothod Open, it cannot be obtained by obj = new EncVideo (The constructor is private on purpose)
    */
    /* outputFile: file format is based on the file extension: .mp4 used for H264/H265, while .avi used for MJPEG or uncompressed video
    *  If outputFile is null, the compressed video data will not be written to a file.
    *  Instead, the encoded output will be written to the memory buffer pointed to by outputData through the Toupnam_EncodeVideo function.
    */
    /* codec:
         h264_nvenc, hevc_nvenc: Nvidia GPU
         h264_qsv, hevc_qsv: Intel GPU (x64 only)
         h264_amf, hevc_amf: AMD GPU (x64 only)
         h264_v4l2m2m, hevc_v4l2m2m: V4L2 memory-to-memory device (linux arm64 only)
         libx264: software
         libx265: software
         mjpeg: motion jpeg
         rawvideo: uncompressed
      use : to split extra parameters, such as:
         input format: rgb24, bgr24, rgba, bgra, gray8, such as: "h264_nvenc:rgb24"; default = bgr24
         mono: used for uncompressed avi, "rawvideo:mono", reduce the size of video file by 2/3
         timestamp: fps(use fps for timestamp), tick(use os tick for timestamp), param(use the input value of the function parameter), such as: "timestamp=fps"; default = tick
         stride: 0(padded to a 4-byte boundary, see TDIBWIDTHBYTES), -1(no padding), positive integer(use this specific value); default = 0
    */
    /* always use Constant Quality Mode
         quality = [1, 100]
         bitrate = 0
    */
    public static EncVideo Open(int width, int height, int fps, int bitrate, int quality, string outputFile, string codec)
    {
        SafeEncHandle handle = Toupnam_OpenVideo(width, height, fps, bitrate, quality, Encoding.UTF8.GetBytes(outputFile + "\0"), codec);
        if (handle == null || handle.IsInvalid || handle.IsClosed)
            return null;
        return new EncVideo(handle);
    }

    /* unTimeStamp
         avi: ignored, timestamp is always set to fps
         mp4: ignored when timestamp is set to fps or tick
    */
    public int Write(IntPtr inputData, uint unTimeStamp)
    {
        return Toupnam_WriteVideo(handle_, inputData, unTimeStamp);
    }

    public int Write(byte[] inputData, uint unTimeStamp)
    {
        return Toupnam_WriteVideo(handle_, inputData, unTimeStamp);
    }

    /* Return value:
          <0 indicates an HRESULT error code;
          =0 means no data is returned;
          >0 indicates the length of the returned data.
       outputData: please ensure that the output buffer is large enough to accommodate the compressed video data.
    */
    public int Encode(IntPtr inputData, IntPtr outputData)
    {
        return Toupnam_EncodeVideo(handle_, inputData, outputData);
    }

    public int Encode(byte[] inputData, byte[] outputData)
    {
        return Toupnam_EncodeVideo(handle_, inputData, outputData);
    }

    public void Close()
    {
        Dispose();
    }

    public void Dispose()  // Follow the Dispose pattern - public nonvirtual.
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    private SafeEncHandle handle_;

    /*
        the object of EncVideo must be obtained by static mothod Open, it cannot be obtained by obj = new EncVideo (The constructor is private on purpose)
    */
    private EncVideo(SafeEncHandle h)
    {
        handle_ = h;
    }

    ~EncVideo()
    {
        Dispose(false);
    }

#if !(NETFX_CORE || WINDOWS_UWP)
    [SecurityPermission(SecurityAction.Demand, UnmanagedCode = true)]
#endif
    protected virtual void Dispose(bool disposing)
    {
        // Note there are three interesting states here:
        // 1) CreateFile failed, _handle contains an invalid handle
        // 2) We called Dispose already, _handle is closed.
        // 3) _handle is null, due to an async exception before
        //    calling CreateFile. Note that the finalizer runs
        //    if the constructor fails.
        if (handle_ != null && !handle_.IsInvalid) {
            // Free the handle
            handle_.Dispose();
        }
        // SafeHandle records the fact that we've called Dispose.
    }

#if !(WINDOWS_UWP)
    public class SafeEncHandle : SafeHandleZeroOrMinusOneIsInvalid
    {
        [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
        private static extern void Toupnam_CloseVideo(IntPtr h);

        public SafeEncHandle()
            : base(true)
        {
        }

        [ReliabilityContract(Consistency.WillNotCorruptState, Cer.MayFail)]
        override protected bool ReleaseHandle()
        {
            // Here, we must obey all rules for constrained execution regions.
            Toupnam_CloseVideo(handle);
            return true;
        }
    };
#else
    public class SafeEncHandle : SafeHandle
    {
        [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
        private static extern void Toupnam_CloseVideo(IntPtr h);
        
        public SafeEncHandle()
            : base(IntPtr.Zero, true)
        {
        }
        
        override protected bool ReleaseHandle()
        {
            Toupnam_CloseVideo(handle);
            return true;
        }
        
        public override bool IsInvalid
        {
            get { return base.handle == IntPtr.Zero || base.handle == (IntPtr)(-1); }
        }
    };
#endif
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern SafeEncHandle Toupnam_OpenVideo(int width, int height, int fps, int bitrate, int quality, byte[] outputFile, [MarshalAs(UnmanagedType.LPStr)] string codec);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_WriteVideo(SafeEncHandle h, IntPtr inputData, uint unTimeStamp);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_WriteVideo(SafeEncHandle h, byte[] inputData, uint unTimeStamp);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_EncodeVideo(SafeEncHandle h, IntPtr inputData, IntPtr outputData);
    [DllImport("toupnam", ExactSpelling = true, CallingConvention = CallingConvention.Winapi)]
    private static extern int Toupnam_EncodeVideo(SafeEncHandle h, byte[] inputData, byte[] outputData);
}