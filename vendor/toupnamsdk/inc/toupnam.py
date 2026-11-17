"""Version: 5.33015.20261024
We use ctypes to call into the toupnam.dll/toupnam.so API,
the python class Toupnam is a thin wrapper class to the native api of toupnam.dll/toupnam.so.
"""
import sys, ctypes, os.path

TOUPNAM_MAX = 64

TOUPNAM_PARA_UNKNOWN                = 0x00
TOUPNAM_PARA_EXPOTIME               = 0x01    # exposure time
TOUPNAM_PARA_AGAIN                  = 0x02    # gain
TOUPNAM_PARA_AEXPOTARGET            = 0x03    # auto exposure target
TOUPNAM_PARA_TEMP                   = 0x04    # color temperature
TOUPNAM_PARA_TINT                   = 0x05
TOUPNAM_PARA_CONTRAST               = 0x06    # contrast
TOUPNAM_PARA_HUE                    = 0x07    # hue
TOUPNAM_PARA_SATURATION             = 0x08    # saturation
TOUPNAM_PARA_BRIGHTNESS             = 0x09    # brightness
TOUPNAM_PARA_GAMMA                  = 0x0a    # gamma
TOUPNAM_PARA_AEXPO                  = 0x0b    # auto exposure
TOUPNAM_PARA_AWB                    = 0x0c    # XCAM1080P:once;  XCAM4K:(0:manual;1:global auto;2:roi)
TOUPNAM_PARA_BINSKIP                = 0x0d    # bin / skip
TOUPNAM_PARA_HZ                     = 0x0e    # power supply: 0 -> 60HZ AC;  1 -> 50Hz AC;   2 -> DC
TOUPNAM_PARA_BPS                    = 0x0f    # bits per second, kbps
TOUPNAM_PARA_KEYFRAME               = 0x10    # key frame interval
TOUPNAM_PARA_LOWLIGHTCOMPENSATION   = 0x11    # low light compensation
TOUPNAM_PARA_SHARPNESS              = 0x12    # sharpness
TOUPNAM_PARA_WBREDGAIN              = 0x13    # white balance red gain
TOUPNAM_PARA_WBGREENGAIN            = 0x14    # white balance green gain
TOUPNAM_PARA_WBBLUEGAIN             = 0x15    # white balance blue gain
TOUPNAM_PARA_DENOISE                = 0x16    # denoise
TOUPNAM_PARA_APSTA                  = 0x17    # ap/sta
TOUPNAM_PARA_CODEC                  = 0x18    # codec, H264, H265, etc
TOUPNAM_PARA_AFPOSITION             = 0x19    # auto focus sensor board positon (applicable to both AFDM and EFL)
TOUPNAM_PARA_AFMODE                 = 0x1a    # auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration)
TOUPNAM_PARA_AFZONE                 = 0x1b    # auto focus zone:
                                              #   the whole resolution is divided in w * h zones:
                                              #     w = imax >> 16
                                              #     h = imax & 0xffff
TOUPNAM_PARA_AFFEEDBACK             = 0x1c    # auto focus information feedback
                                              # AFDM:
                                              #   0: unknown
                                              #   1: focused
                                              #   2: focusing
                                              #   3: defocuse (out of focus)
                                              #   4: up (workbench move up)
                                              #   5: down (workbench move down)
                                              #
                                              # EFL:
                                              #   NA = 0x0,           /* Not available */
                                              #   PEAKPOINT = 0x1,    /* Focus completed, find the focus position */
                                              #   DEFOCUS      = 0x2, /* End of focus, defocus */
                                              #   NEAR         = 0x3, /* Focusing ended, object too close */
                                              #   FAR          = 0x4, /* Focusing ended, object too far */
                                              #   ROICHANGED   = 0x5, /* Focusing ends, roi changes */
                                              #   SCENECHANGED = 0x6, /* Focusing ends, scene changes */
                                              #   MODECHANGED  = 0x7, /* The end of focusing and the change in focusing mode is usually determined by the user moderator */
                                              #   UNFINISH     = 0x8, /* The focus is not complete. At the beginning of focusing, it will be set as incomplete */
TOUPNAM_PARA_AFPOSITION_ABSOLUTE    = 0x1d    ## absolute auto focus sensor board positon
TOUPNAM_PARA_STATUS                 = 0x1e    ## status
TOUPNAM_PARA_EVENT                  = 0x1f    ## event
TOUPNAM_PARA_WBROILEFT              = 0x20    ## white balance roi left
TOUPNAM_PARA_WBROITOP               = 0x21    ## white balance roi top
TOUPNAM_PARA_WBROIWIDTH             = 0x22    # white balance roi width
TOUPNAM_PARA_WBROIHEIGHT            = 0x23    # white balance roi height
TOUPNAM_PARA_VFLIP                  = 0x24    # vertical flip
TOUPNAM_PARA_HFLIP                  = 0x25    # horizontal flip
TOUPNAM_PARA_CHROME                 = 0x26    # monochromatic mode
TOUPNAM_PARA_SIZE                   = 0x27    # video width & height
TOUPNAM_PARA_LIGHTADJUSTMENT        = 0x28    # light source brightness adjustment
TOUPNAM_PARA_ZOOM                   = 0x29    # zoom
TOUPNAM_PARA_EF_MODE                = 0x2a    # EFL, auto focus mode, 0: lens calibration mode; 1: manual focus; 2: single focus; 3: auto focus;
TOUPNAM_PARA_EF_FL                  = 0x2b    # EFL, focal length
TOUPNAM_PARA_EF_APERTURE            = 0x2c    # EFL, F#, 24~16bit:Cur, 15~8bit:Min, 7~0bit:Max
TOUPNAM_PARA_EF_FOCUS_MAX           = 0x2d    # EFL, maximum focal length
TOUPNAM_PARA_EF_LENS_ID             = 0x2e    # EFL, lens ID
TOUPNAM_PARA_EF_AFMF                = 0x2f    # EFL, lens focus mode, 0: AF; 1: MF;
TOUPNAM_PARA_EF_WD_ENABLE           = 0x30    # EFL, enable working distance function
TOUPNAM_PARA_EF_WD_NEAR             = 0x31    # EFL, near working distance point
TOUPNAM_PARA_EF_WD_FAR              = 0x32    # EFL, far working distance point

TOUPNAM_PARA_CHROME_LOCAL           = 0x80    # local monochromatic mode
TOUPNAM_PARA_VFLIP_LOCAL            = 0x81    # local vertical flip
TOUPNAM_PARA_HFLIP_LOCAL            = 0x82    # local horizontal flip
TOUPNAM_PARA_NEGATIVE_LOCAL         = 0x83    # local negative film
TOUPNAM_PARA_FORMAT_LOCAL           = 0x84    # output format: 0 => BGR888, 1 => BGRA8888, 2 => RGB888, 3 => RGBA8888, 4 => RAW; default: 0
                                              # MUST be set BEFORE StartXXXX

TOUPNAM_PARA_STATUS_RECORDING       = 0x00000001      # recording
TOUPNAM_PARA_STATUS_SD              = 0x00000002      # sd card available
TOUPNAM_PARA_STATUS_SD_FULL         = 0x00000004      # sd card full

TOUPNAM_PARA_EVENT_FAT4G            = 0x00000001      # file size limit 4g in FAT32

TOUPNAM_STATE_INITING               = 0x00    # initialization
TOUPNAM_STATE_NORMAL                = 0x01    # normal
TOUPNAM_STATE_UNREACHABLE           = 0x02    # network not reachable

TOUPNAM_FLAG_WIFI_AP                = 0x00000001
TOUPNAM_FLAG_WIFI_STA               = 0x00000002
TOUPNAM_FLAG_ETHERNET               = 0x00000004
TOUPNAM_FLAG_CAPTURE                = 0x00000008  # support the ability of capture image from camera
TOUPNAM_FLAG_AWBCHECKMODE           = 0x00000010  # auto white balance: check mode vs 'once' mode
TOUPNAM_FLAG_UVC                    = 0x00000020  # uvc camera */
TOUPNAM_FLAG_WBGAIN                 = 0x00000040  # white balance gain mode or temp tint mode
TOUPNAM_FLAG_MULTICAST              = 0x00000080  # RTSP/RTP multicast
TOUPNAM_FLAG_AF                     = 0x00000100  # support auto focus
TOUPNAM_FLAG_SD_LIST                = 0x00000200  # support to list sd card
TOUPNAM_FLAG_SD                     = 0x00000400  # support sd card
TOUPNAM_FLAG_WBROI                  = 0x00000800  # white balance: 0:manual;1:global auto;2:roi
TOUPNAM_FLAG_STA_SUPPORT            = 0x00001000  # wifi camera has sta mode, app should have sta ssid & password function
TOUPNAM_FLAG_RTP_OVER_RTSP          = 0x00002000  # rtp over rtsp
TOUPNAM_FLAG_HZ_AUTOEXPO            = 0x00004000  # enable auto exposure when 50/60 hz
TOUPNAM_FLAG_AFDM                   = 0x00008000
TOUPNAM_FLAG_EFL                    = 0x00010000
TOUPNAM_FLAG_CAPTURERAW             = 0x00020000  # capture raw image

TOUPNAM_EVENT_ENUM                  = 0x01    # enum
TOUPNAM_EVENT_WIFI                  = 0x02    # wifi
TOUPNAM_EVENT_PARA                  = 0x03    # parameter change TOUPNAM_PARA_xxxx
TOUPNAM_EVENT_IMAGE                 = 0x04    # image
TOUPNAM_EVENT_LISTWIFI              = 0x05    # list wifi finished
TOUPNAM_EVENT_LISTDIR               = 0x07    # list dir
TOUPNAM_EVENT_THUMBNAIL             = 0x08    # thumbnail
TOUPNAM_EVENT_DIRCHANGE             = 0x09    # dir change notify
TOUPNAM_EVENT_RECORDSTART           = 0x0a    # record start
TOUPNAM_EVENT_RECORDSTOP            = 0x0b    # record stop
TOUPNAM_EVENT_DATETIME              = 0x0c    # date time
TOUPNAM_EVENT_ERROR                 = 0x80    # error
TOUPNAM_EVENT_EOF                   = 0x81    # end of file

def TDIBWIDTHBYTES(bits):
    return ((bits + 31) // 32 * 4)
    
class toupnam_range:
    def __init__(self, val):
        self.idisable = val.idisable                # 0 = "support this feature", 1 = "not support"
        self.imin = val.imin                        # minimum value
        self.imax = val.imax                        # maximum value
        self.idef = val.idef                        # default value

class toupnam_device:
    def __init__(self, val):
        self.id = val.id                            # unique camera id, used for Toupnam_Open
        self.sn = val.sn.decode('ascii')            # serial number
        self.name = val.name.decode('ascii')
        self.model = val.model.decode('ascii')
        self.version = val.version.decode('ascii')
        self.addr = val.addr.decode('ascii')        # ip
        self.url = val.url.decode('ascii')          # playback url, such as rtsp://xxxx/yyyy
        self.state = val.state                      # TOUPNAM_STATE_xxx
        self.flag = val.flag                        # TOUPNAM_FLAG_xxx
        self.range = []
        for i in range(0, len(val.range)):
            self.range.append(toupnam_range(val.range[i]))

class toupnam_imageptr:
    def __init__(self):
        self.id = 0        # 0 is reserved as an invalid id
        self.width = 0     # image width
        self.height = 0    # image height
        self.length = 0    # buf length
        self.buf = None

class toupnam_wifi:
    def __init__(self, ssid, password):
        self.ssid = ssid
        self.password = val.password

class toupnam_dirchange:
    def __init__(self, type, name, newname):
        self.type = type                            # 0 => add, 1 => del, 2 => rename
        self.name = name
        self.newname = newname

if sys.platform == 'win32':
    class HRESULTException(OSError):
        def __init__(self, hr):
            OSError.__init__(self, None, ctypes.FormatError(hr).strip(), None, hr)
            self.hr = hr
else:
    class HRESULTException(Exception):
        def __init__(self, hr):
            self.hr = hr

class CLib:
    class _device(ctypes.Structure):
        pass

    class _range(ctypes.Structure):
        _fields_ = [("idisable", ctypes.c_int),
                    ("imin", ctypes.c_int),
                    ("imax", ctypes.c_int),
                    ("idef", ctypes.c_int)]

    class _imageptr(ctypes.Structure):
        _fields_ = [("id", ctypes.c_uint),
                    ("width", ctypes.c_uint),
                    ("height", ctypes.c_uint),
                    ("length", ctypes.c_uint),
                    ("buf", ctypes.POINTER(ctypes.c_ubyte))]

    class _wifi(ctypes.Structure):
        _fields_ = [("ssid", ctypes.c_char * 64),
                    ("password", ctypes.c_char * 64)]

    class _eventextra(ctypes.Structure):
        _fields_ = [("result", ctypes.c_int),
                    ("ctx", ctypes.c_void_p),
                    ("ptr", ctypes.c_void_p),
                    ("length", ctypes.c_uint)]

    class _dirchange(ctypes.Structure):
        _fields_ = [("type", ctypes.c_uint),
                    ("name", ctypes.c_char * 256),
                    ("newname", ctypes.c_char * 256)]

    class _BITMAPINFOHEADER(ctypes.Structure):
        _fields_ = [("biSize", ctypes.c_uint),
                    ("biWidth", ctypes.c_int),
                    ("biHeight", ctypes.c_int),
                    ("biPlanes", ctypes.c_ushort),
                    ("biBitCount", ctypes.c_ushort),
                    ("biCompression", ctypes.c_uint),
                    ("biSizeImage", ctypes.c_uint),
                    ("biXPelsPerMeter", ctypes.c_int),
                    ("biYPelsPerMeter", ctypes.c_int),
                    ("biClrUsed", ctypes.c_uint),
                    ("biClrImportant", ctypes.c_uint)]

    if sys.platform == 'win32':
        _EVENT_CALLBACK = ctypes.WINFUNCTYPE(None, ctypes.c_uint, ctypes.c_uint, ctypes.c_void_p, ctypes.POINTER(_eventextra))
        _CAPTURE_CALLBACK = ctypes.WINFUNCTYPE(None, ctypes.c_int, ctypes.c_void_p, ctypes.c_size_t, ctypes.POINTER(_BITMAPINFOHEADER), ctypes.c_void_p)
    else:
        _EVENT_CALLBACK = ctypes.CFUNCTYPE(None, ctypes.c_uint, ctypes.c_uint, ctypes.c_void_p, ctypes.POINTER(_eventextra))
        _CAPTURE_CALLBACK = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_void_p, ctypes.c_size_t, ctypes.POINTER(_BITMAPINFOHEADER), ctypes.c_void_p)
    _lib = None

    @staticmethod
    def __errcheck(result, fun, args):
        if result < 0:
            raise HRESULTException(result)
        return args

    @classmethod
    def initlib(cls):
        if cls._lib is None:
            try: # Firstly try to load the library in the directory where this file is located
                dir = os.path.dirname(os.path.realpath(__file__))
                if sys.platform == 'win32':
                    cls._lib = ctypes.windll.LoadLibrary(os.path.join(dir, 'toupnam.dll'))
                elif sys.platform.startswith('linux'):
                    cls._lib = ctypes.cdll.LoadLibrary(os.path.join(dir, 'libtoupnam.so'))
                else:
                    cls._lib = ctypes.cdll.LoadLibrary(os.path.join(dir, 'libtoupnam.dylib'))
            except OSError:
                pass

            if cls._lib is None:
                if sys.platform == 'win32':
                    cls._lib = ctypes.windll.LoadLibrary('toupnam.dll')
                elif sys.platform.startswith('linux'):
                    cls._lib = ctypes.cdll.LoadLibrary('libtoupnam.so')
                else:
                    cls._lib = ctypes.cdll.LoadLibrary('libtoupnam.dylib')

            cls._device._fields_ = [
                    ("id", ctypes.c_char * 64),
                    ("sn", ctypes.c_char * 64),
                    ("name", ctypes.c_char * 64),
                    ("model", ctypes.c_char * 64),
                    ("version", ctypes.c_char * 64),
                    ("addr", ctypes.c_char * 64),
                    ("url", ctypes.c_char * 256),
                    ("state", ctypes.c_uint),
                    ("flag", ctypes.c_uint),
                    ("range", cls._range * 64)]

            cls._lib.Toupnam_Version.restype = ctypes.c_char_p
            cls._lib.Toupnam_Init.restype = None
            cls._lib.Toupnam_Fini.restype = None
            cls._lib.Toupnam_Enum.restype = ctypes.c_uint
            cls._lib.Toupnam_Open.restype = ctypes.c_void_p
            cls._lib.Toupnam_Open_ByIndex.restype = ctypes.c_void_p
            cls._lib.Toupnam_Close.restype = None
            cls._lib.Toupnam_PullImagePtr.restype = ctypes.c_int
            cls._lib.Toupnam_PushImagePtr.restype = ctypes.c_int
            cls._lib.Toupnam_PullImage.restype = ctypes.c_int
            cls._lib.Toupnam_StartPullModeWithCallback.restype = ctypes.c_int
            cls._lib.Toupnam_Stop.restype = ctypes.c_int
            cls._lib.Toupnam_Pause.restype = ctypes.c_int
            cls._lib.Toupnam_Capture.restype = ctypes.c_int
            cls._lib.Toupnam_Capture_ById.restype = ctypes.c_int
            cls._lib.Toupnam_get_Device.restype = cls._device
            cls._lib.Toupnam_get_Size.restype = ctypes.c_int
            cls._lib.Toupnam_get_CapSize.restype = ctypes.c_int
            cls._lib.Toupnam_get_FourCC.restype = ctypes.c_int
            cls._lib.Toupnam_Record.restype = ctypes.c_int
            cls._lib.Toupnam_put_Para.restype = ctypes.c_int
            cls._lib.Toupnam_get_Para.restype = ctypes.c_int
            cls._lib.Toupnam_put_Wifi.restype = ctypes.c_int
            cls._lib.Toupnam_get_Wifi.restype = ctypes.c_int
            cls._lib.Toupnam_list_Wifi.restype = ctypes.c_int
            cls._lib.Toupnam_list_Dir.restype = ctypes.c_int
            cls._lib.Toupnam_get_Thumbnail.restype = ctypes.c_int
            cls._lib.Toupnam_change_Dir.restype = ctypes.c_int
            cls._lib.Toupnam_RecordStart.restype = ctypes.c_int
            cls._lib.Toupnam_RecordStop.restype = ctypes.c_int
            cls._lib.Toupnam_get_DateTime.restype = ctypes.c_int
            cls._lib.Toupnam_put_DateTime.restype = ctypes.c_int
            cls._lib.Toupnam_put_Para_ById.restype = ctypes.c_int
            cls._lib.Toupnam_get_Para_ById.restype = ctypes.c_int
            cls._lib.Toupnam_get_Size_ById.restype = ctypes.c_int
            cls._lib.Toupnam_PriFlag.restype = None
            cls._lib.Toupnam_add_Ip.restype = None
            cls._lib.Toupnam_del_Ip.restype = None
            cls._lib.Toupnam_OpenVideo.restype = ctypes.c_void_p
            cls._lib.Toupnam_CloseVideo.restype = None
            cls._lib.Toupnam_WriteVideo.restype = ctypes.c_int
            cls._lib.Toupnam_EncodeVideo.restype = ctypes.c_int

            cls._lib.Toupnam_Enum.errcheck = cls.__errcheck
            cls._lib.Toupnam_PullImagePtr.errcheck = cls.__errcheck
            cls._lib.Toupnam_PushImagePtr.errcheck = cls.__errcheck
            cls._lib.Toupnam_PullImage.errcheck = cls.__errcheck
            cls._lib.Toupnam_StartPullModeWithCallback.errcheck = cls.__errcheck
            cls._lib.Toupnam_Stop.errcheck = cls.__errcheck
            cls._lib.Toupnam_Pause.errcheck = cls.__errcheck
            cls._lib.Toupnam_Capture.errcheck = cls.__errcheck
            cls._lib.Toupnam_Capture_ById.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Device.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Size.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_CapSize.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_FourCC.errcheck = cls.__errcheck
            cls._lib.Toupnam_Record.errcheck = cls.__errcheck
            cls._lib.Toupnam_put_Para.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Para.errcheck = cls.__errcheck
            cls._lib.Toupnam_put_Wifi.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Wifi.errcheck = cls.__errcheck
            cls._lib.Toupnam_list_Wifi.errcheck = cls.__errcheck
            cls._lib.Toupnam_list_Dir.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Thumbnail.errcheck = cls.__errcheck
            cls._lib.Toupnam_change_Dir.errcheck = cls.__errcheck
            cls._lib.Toupnam_RecordStart.errcheck = cls.__errcheck
            cls._lib.Toupnam_RecordStop.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_DateTime.errcheck = cls.__errcheck
            cls._lib.Toupnam_put_DateTime.errcheck = cls.__errcheck
            cls._lib.Toupnam_put_Para_ById.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Para_ById.errcheck = cls.__errcheck
            cls._lib.Toupnam_get_Size_ById.errcheck = cls.__errcheck
            cls._lib.Toupnam_WriteVideo.errcheck = cls.__errcheck
            cls._lib.Toupnam_EncodeVideo.errcheck = cls.__errcheck

            cls._lib.Toupnam_Version.argtypes = None
            cls._lib.Toupnam_Init.argtypes = [cls._EVENT_CALLBACK, ctypes.c_void_p]
            cls._lib.Toupnam_Fini.argtypes = None
            cls._lib.Toupnam_Enum.argtypes = [ctypes.POINTER(cls._device), ctypes.c_int]
            cls._lib.Toupnam_Open.argtypes = [ctypes.c_char_p]
            cls._lib.Toupnam_Open_ByIndex.argtypes = [ctypes.c_uint]
            cls._lib.Toupnam_Close.argtypes = [ctypes.c_void_p]

            cls._lib.Toupnam_PullImagePtr.argtypes = [ctypes.c_void_p, ctypes.POINTER(cls._imageptr)]
            cls._lib.Toupnam_PushImagePtr.argtypes = [ctypes.c_void_p, ctypes.c_uint]
            cls._lib.Toupnam_PullImage.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_int, ctypes.POINTER(ctypes.c_uint), ctypes.POINTER(ctypes.c_uint)]
            cls._lib.Toupnam_StartPullModeWithCallback.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
            cls._lib.Toupnam_Stop.argtypes = [ctypes.c_void_p]
            cls._lib.Toupnam_Pause.argtypes = [ctypes.c_void_p, ctypes.c_int]
            cls._lib.Toupnam_Capture.argtypes = [ctypes.c_void_p, ctypes.c_char_p, cls._CAPTURE_CALLBACK, ctypes.c_void_p]
            cls._lib.Toupnam_Capture_ById.argtypes = [ctypes.c_char_p, ctypes.c_char_p, cls._CAPTURE_CALLBACK, ctypes.c_void_p]
            cls._lib.Toupnam_get_Device.argtypes = [ctypes.c_void_p]
            cls._lib.Toupnam_get_Size.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_int), ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_get_CapSize.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_int), ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_get_FourCC.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_Record.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
            cls._lib.Toupnam_put_Para.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int]
            cls._lib.Toupnam_get_Para.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_put_Wifi.argtypes = [ctypes.c_char_p, cls._wifi]
            cls._lib.Toupnam_get_Wifi.argtypes = [ctypes.c_char_p]
            cls._lib.Toupnam_list_Wifi.argtypes = [ctypes.c_char_p, ctypes.c_void_p]
            cls._lib.Toupnam_list_Dir.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_void_p]
            cls._lib.Toupnam_get_Thumbnail.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_void_p]
            cls._lib.Toupnam_change_Dir.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.POINTER(cls._dirchange), ctypes.c_uint]
            cls._lib.Toupnam_RecordStart.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_uint, ctypes.c_void_p]
            cls._lib.Toupnam_RecordStop.argtypes = [ctypes.c_char_p, ctypes.c_void_p]
            cls._lib.Toupnam_get_DateTime.argtypes = [ctypes.c_char_p, ctypes.c_void_p]
            cls._lib.Toupnam_put_DateTime.argtypes = [ctypes.c_char_p, ctypes.c_uint, ctypes.c_void_p]
            cls._lib.Toupnam_put_Para_ById.argtypes = [ctypes.c_char_p, ctypes.c_uint, ctypes.c_int]
            cls._lib.Toupnam_get_Para_ById.argtypes = [ctypes.c_char_p, ctypes.c_uint, ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_get_Size_ById.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.POINTER(ctypes.c_int), ctypes.POINTER(ctypes.c_int)]
            cls._lib.Toupnam_PriFlag.argtypes = [ctypes.c_uint, ctypes.c_uint]
            cls._lib.Toupnam_add_Ip.argtypes = [ctypes.c_void_p]
            cls._lib.Toupnam_del_Ip.argtypes = [ctypes.c_void_p]
            
            cls._lib.Toupnam_OpenVideo.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_char_p, ctypes.c_char_p]
            cls._lib.Toupnam_CloseVideo.argtypes = [ctypes.c_void_p]
            cls._lib.Toupnam_WriteVideo.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_uint]
            cls._lib.Toupnam_EncodeVideo.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_void_p]

class EncVideo(CLib):
    def __init__(self, h):
        """the object of EncVideo must be obtained by classmethod Open, it cannot be obtained by obj = toupnam.EncVideo()"""
        self.__h = h

    def __del__(self):
        self.Close()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.Close()

    def __nonzero__(self):
        return self.__h is not None

    def __bool__(self):
        return self.__h is not None

    @classmethod
    def Open(cls, width, height, fps, bitrate, quality, outputFile, codec):
        """the object of toupnam must be obtained by classmethod Open or OpenByIndex, it cannot be obtained by obj = toupnam.toupnam()"""
        """
        outputFile: file format is based on the file extension, .mp4 used for H264/H265, while .avi used for MJPEG or uncompressed video
            If outputFile is null, the compressed video data will not be written to a file.
            Instead, the encoded output will be written to the memory buffer pointed to by outputData through the Toupnam_EncodeVideo function.
        codec:
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

        always use Constant Quality Mode
            quality = [1, 100]
            bitrate = 0
        """
        CLib.initlib()
        h = cls._lib.Toupnam_OpenVideo(width, height, fps, bitrate, quality, outputFile.encode('utf-8') if outputFile is not None else None, codec.encode("ascii"))
        if h is None:
            return None
        return __class__(h)

    def Close(self):
        if self.__h:
            self._lib.Toupnam_CloseVideo(self.__h)
            self.__h = None

    def Write(self, inputData, unTimeStamp):
        """
        unTimeStamp
            avi: ignored, timestamp is always set to fps
            mp4: ignored when timestamp is set to fps or tick
        """
        self._lib.Toupnam_WriteVideo(self.__h, inputData, unTimeStamp)

    def Encode(self, inputData, outputData):
        """
        Return value:
            <0 indicates an HRESULT error code;
            =0 means no data is returned;
            >0 indicates the length of the returned data.
        outputData: please ensure that the output buffer is large enough to accommodate the compressed video data.
        """
        self._lib.Toupnam_EncodeVideo(self.__h, inputData, outputData)

class Toupnam(CLib):
    class cbobj:
        def __init__(self, f, c, i):
            self.fun = f
            self.ctx = c
            self.id = i

    @staticmethod
    def __eventCallbackFun(nEvent, nPara, pCallbackCtx, pExtra):
        o = __class__.__dic.get(pCallbackCtx)
        if o is not None:
            o.fun(nEvent, nPara, o.ctx)

    @staticmethod
    def __captureCallbackFun(result, pData, nLength, pHeader, pCallbackCtx):
        o = __class__.__dic.get(pCallbackCtx)
        if o is not None:
            if pData == 0 or nLength == 0:
                o.fun(result, None, 0, 0, o.ctx)
            else:
                nWidth = nHeight = 0
                if pHeader != 0:
                    nWidth = pHeader.contents.biWidth
                    nHeight = pHeader.contents.biHeight
                bData = (ctypes.c_char * nLength).from_address(pData)
                o.fun(result, bData, nWidth, nHeight, o.ctx)
    __dic = {}
    __evt = None
    __cap = None
    __sid = 0

    @classmethod
    def Version(cls):
        """get the version of this dll/so/dylib, which is: 5.33015.20261024"""
        CLib.initlib()
        return cls._lib.Toupnam_Version()

    @classmethod
    def Init(cls, fun, ctx):
        """
        call only once when application startup
        when toupnam.dll/libtoupnam.so discovery new camera, pCallback will be called.
        fun can be None if the application does not interest this.
        """
        CLib.initlib()
        if cls.__evt is None:
            cls.__evt = CLib._EVENT_CALLBACK(cls.__eventCallbackFun)
            cls.__cap = CLib._CAPTURE_CALLBACK(cls.__captureCallbackFun)
            cls.__dic[0] = cls.cbobj(fun, ctx, 0)
            cls._lib.Toupnam_Init(cls.__evt, 0)

    @classmethod
    def Fini(cls):
        """call only once when application exit"""
        cls._lib.Toupnam_Fini()

    @classmethod
    def Enum(cls):
        CLib.initlib()
        a = (CLib._device * TOUPNAM_MAX)()
        n = cls._lib.Toupnam_Enum(a, TOUPNAM_MAX)
        if n < 0:
            raise HRESULTException(n)
        arr = []
        for i in range(0, n):
            arr.append(toupnam_device(a[i]))
        return arr

    def __init__(self, h):
        """the object of Toupnam must be obtained by classmethod Open or OpenByIndex, it cannot be obtained by obj = toupnam.Toupnam()"""
        self.__h = h
        self.__sid += 1
        self.__id = self.__sid

    def __del__(self):
        self.Close()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.Close()

    def __nonzero__(self):
        return self.__h is not None

    def __bool__(self):
        return self.__h is not None

    @classmethod
    def Open(cls, camId):
        """the object of toupnam must be obtained by classmethod Open or OpenByIndex, it cannot be obtained by obj = toupnam.toupnam()"""
        CLib.initlib()
        h = cls._lib.Toupnam_Open(camId)
        if h is None:
            return None
        return __class__(h)

    @classmethod
    def Open_ByIndex(cls, index):
        CLib.initlib()
        h = cls._lib.Toupnam_Open_ByIndex(index)
        if h is None:
            return None
        return __class__(h)

    def Close(self):
        for k in list(self.__dic.keys()):
            if self.__dic[k].id == self.__id:
                self.__dic.pop(k)

        if self.__h:
            self._lib.Toupnam_Close(self.__h)
            self.__h = None

    def PullImagePtr(self):
        """Obtains a pointer to the frame buffer directly from the SDK, eliminating the need to copy frame data and thus improving performance"""
        ptr = self._imageptr()
        self._lib.Toupnam_PullImagePtr(self.__h, ctypes.byref(ptr))
        newptr = toupnam_imageptr()
        newptr.id = ptr.id
        newptr.width = ptr.width
        newptr.height = ptr.height
        newptr.length = ptr.length
        newptr.buf = ptr.buf
        return newptr

    def PushImagePtr(self, ptrId):
        """
        After a frame buffer has been used, it must be returned to the SDK for reuse. Please note the following:
            (a) The frame buffer must only be returned back after it is no longer in use. Any access after returning it is unsafe, as the SDK may have already reused the buffer and overwritten the memory with new data.
            (b) If a frame buffer is not returned, the pool of available buffers will gradually decrease.
            (c) Each frame buffer obtained via Pull (identified by its ID) may be returned only once; duplicate returns are not allowed.
            (d) The return order does not need to match the Pull order; buffers may be returned out of order.
            (e) Frame buffers that have not been returned become invalid immediately after Close.
        """
        self._lib.Toupnam_PushImagePtr(self.__h, ptrId)

    def PullImage(self, pImageData, bits):
        """
        bits: 24 (RGB24), 32 (RGB32), or 8 (Grey), see: TOUPNAM_PARA_FORMAT_LOCAL
        if RAW format, pnWidth = data size, pnHeight = not used
        """
        x = ctypes.c_uint(0)
        y = ctypes.c_uint(0)
        self._lib.Toupnam_PullImage(self.__h, pImageData, bits, ctypes.byref(x), ctypes.byref(y))
        return (x.value, y.value)

    def Start(self, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, self.__id)
        ret = self._lib.Toupnam_StartPullModeWithCallback(self.__h, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    def Stop(self):
        self._lib.Toupnam_Stop(self.__h)

    def Pause(self, bPause):
        self._lib.Toupnam_Pause(self.__h, 1 if bPause else 0)

    def Capture(self, outputFile, fun, ctx):
        """
        capture image, compare this to image extracted from video
        outputFile:
                None        -> capture image and then return by callback
                "raw"       -> capture raw image and then return by callback
                "abc.jpg"   -> capture image and then save it in the camera sd card with filename 'abc.jpg'
                "abc.raw"   -> capture raw image and then save it in the camera sd card with filename 'abc.raw'
                "thumbnail" -> capture the thumbnail image and then return by callback
                "*"         -> capture image and then save it in the camera sd card with auto generated file name
                "*.raw"     -> capture raw image and then save it in the camera sd card with auto generated file name
        """
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, self.__id)
        ret = self._lib.Toupnam_Capture(self.__h, outputFile.encode('utf-8'), self.__cap, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def Capture_ById(cls, camId, outputFile, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_Capture_ById(camId, outputFile.encode('utf-8'), cls.__cap, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    def get_Device(self):
        return self._lib.Toupnam_get_Device(self.__h)

    def get_Size(self):
        x = ctypes.c_int(0)
        y = ctypes.c_int(0)
        self._lib.Toupnam_get_Size(self.__h, ctypes.byref(x), ctypes.byref(y))
        return (x.value, y.value)

    def get_CapSize(self):
        x = ctypes.c_int(0)
        y = ctypes.c_int(0)
        self._lib.Toupnam_get_CapSize(self.__h, ctypes.byref(x), ctypes.byref(y))
        return (x.value, y.value)

    def get_FourCC(self):
        """see http://en.wikipedia.org/wiki/FourCC, http://www.fourcc.org"""
        x = ctypes.c_int(0)
        self._lib.Toupnam_get_FourCC(self.__h, ctypes.byref(x))
        return x.value

    def Record(self, outputFile):
        """
        (outputFile == None) means to stop record.
        support file extension: *.asf, *.mp4, *.mkv
        """
        if outputFile is None:
            self._lib.Toupnam_Record(self.__h, None)
        else:
            self._lib.Toupnam_Record(self.__h, outputFile.encode('utf-8'))

    def put_Para(self, para, value):
        """para is one of TOUPNAM_PARA_xxx"""
        self._lib.Toupnam_put_Para(self.__h, para, value)

    def get_Para(self, para):
        x = ctypes.c_int(0)
        self._lib.Toupnam_get_Para(self.__h, para, ctypes.byref(x))
        return x.value

    @classmethod
    def put_Wifi(cls, camId, wifi):
        cls._lib.Toupnam_put_Wifi(camId, wifi)

    @classmethod
    def get_Wifi(cls, camId):
        cls._lib.Toupnam_get_Wifi(camId)

    @classmethod
    def list_Wifi(cls, camId, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_list_Wifi(camId, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def list_Dir(cls, camId, path, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_list_Dir(camId, path.encode('utf-8'), sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def get_Thumbnail(cls, camId, path, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_get_Thumbnail(camId, path.encode('utf-8'), sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def change_Dir(cls, camId, path, dc):
        """del file or directory, rename file or directory"""
        cls._lib.Toupnam_change_Dir(camId, path.encode('utf-8'), dc, len(dc))

    @classmethod
    def RecordStart(cls, camId, outputFile, recordtime, fun, ctx):
        """record to the sd card"""
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_RecordStart(camId, outputFile.encode('utf-8'), recordtime, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def RecordStop(cls, camId, fun, ctx):
        """stop record to the sd card"""
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_RecordStop(camId, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def get_DateTime(cls, camId, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_get_DateTime(camId, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def put_DateTime(cls, camId, t, fun, ctx):
        sid = 0
        if fun is not None:
            self.__sid += 1
            sid = self.__sid
            self.__dic[sid] = self.cbobj(fun, ctx, 0)
        ret = cls._lib.Toupnam_put_DateTime(camId, t, sid)
        if ret < 0 and fun is not None:
            self.__dic.pop(sid)

    @classmethod
    def put_Para_ById(cls, camId, para, value):
        """para is one of TOUPNAM_PARA_XXX"""
        x = ctypes.c_int(0)
        cls._lib.Toupnam_put_Para_ById(camId, para, value)

    @classmethod
    def get_Para_ById(cls, camId, para):
        x = ctypes.c_int(0)
        cls._lib.Toupnam_get_Para_ById(camId, para, ctypes.byref(x))
        return x.value

    @classmethod
    def get_Size_ById(cls, camId, res):
        x = ctypes.c_int(0)
        y = ctypes.c_int(0)
        cls._lib.Toupnam_get_Size_ById(camId, res, ctypes.byref(x), ctypes.byref(y))
        return (x.value, y.value)

    @classmethod
    def PriFlag(cls, nFlag, nMask):
        CLib.initlib()
        cls._lib.Toupnam_PriFlag(nFlag, nMask)

    @classmethod
    def add_Ip(cls, arr):
        """arr = ("1.2.3.4", "1.2.3.5", ...)"""
        type = (ctypes.c_char_p * (len(arr) + 1))
        a = type()
        for i in range(0, len(arr)):
            a[i] = ctypes.c_char_p(arr[i])
        cls._lib.Toupnam_add_Ip(a)

    @classmethod
    def del_Ip(cls, arr):
        type = (ctypes.c_char_p * (len(arr) + 1))
        a = type()
        for i in range(0, len(arr)):
            a[i] = ctypes.c_char_p(arr[i])
        cls._lib.Toupnam_del_Ip(a)