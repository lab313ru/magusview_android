#ifndef __toupnam_h__
#define __toupnam_h__

/* Version: 5.33015.20261024 */
/*
   Platform & Architecture:
       (1) Win32:
             (a) x86: XP SP3 or above; CPU supports SSE2 instruction set or above
             (b) x64: Win7 or above
       (2) macOS: x64; macOS 11.0 or above
       (3) Linux: kernel 2.6.27 or above
             (a) x86: CPU supports SSE3 instruction set or above; GLIBC 2.17 or above
             (b) x64: GLIBC 2.17 or above
             (c) armel: GLIBC 2.17 or above
             (d) armhf: GLIBC 2.17 or above
             (e) arm64: GLIBC 2.28 or above
*/

#ifdef _WIN32
#ifndef _INC_WINDOWS
#include <windows.h>
#endif
#endif

#ifdef __cplusplus
extern "C" {
#endif

#ifdef _WIN32
#pragma pack(push, 8)
#ifdef TOUPNAM_EXPORTS
#define TOUPNAM_API(x)    __declspec(dllexport)   x   __stdcall  /* in Windows, we use __stdcall calling convention, see https://docs.microsoft.com/en-us/cpp/cpp/stdcall */
#elif !defined(TOUPNAM_NOIMPORTS)
#define TOUPNAM_API(x)    __declspec(dllimport)   x   __stdcall
#else
#define TOUPNAM_API(x)    x   __stdcall
#endif

#else

#define TOUPNAM_API(x)    x

#ifndef HRESULT
#define HRESULT int
#endif

#ifndef SUCCEEDED
#define SUCCEEDED(hr)   (((HRESULT)(hr)) >= 0)
#endif
#ifndef FAILED
#define FAILED(hr)      (((HRESULT)(hr)) < 0)
#endif

#ifndef __stdcall
#define __stdcall
#endif

#ifndef __BITMAPINFOHEADER_DEFINED__
#define __BITMAPINFOHEADER_DEFINED__
typedef struct {
    unsigned        biSize;
    int             biWidth;
    int             biHeight;
    unsigned short  biPlanes;
    unsigned short  biBitCount;
    unsigned        biCompression;
    unsigned        biSizeImage;
    int             biXPelsPerMeter;
    int             biYPelsPerMeter;
    unsigned        biClrUsed;
    unsigned        biClrImportant;
} BITMAPINFOHEADER;
#endif

#endif

/* handle */
typedef struct ToupnamT { int unused; } *HToupnam, *HToupNam;

#ifndef TDIBWIDTHBYTES
#define TDIBWIDTHBYTES(bits)    ((unsigned)(((bits) + 31) & (~31)) / 8)
#endif

#ifndef __TOUPNAMDEVIC_DEFINED__
#define __TOUPNAMDEVIC_DEFINED__

#define TOUPNAM_MAX                         64

typedef struct {
    int idisable;   /* 0 = "support this feature", 1 = "not support" */
    int imin;       /* minimum value */
    int imax;       /* maximum value */
    int idef;       /* default value */
} ToupnamRange;

#define TOUPNAM_PARA_UNKNOWN                0x00
#define TOUPNAM_PARA_EXPOTIME               0x01    /* exposure time */
#define TOUPNAM_PARA_AGAIN                  0x02    /* gain */
#define TOUPNAM_PARA_AEXPOTARGET            0x03    /* auto exposure target */
#define TOUPNAM_PARA_TEMP                   0x04    /* color temperature */
#define TOUPNAM_PARA_TINT                   0x05
#define TOUPNAM_PARA_CONTRAST               0x06    /* contrast */
#define TOUPNAM_PARA_HUE                    0x07    /* hue */
#define TOUPNAM_PARA_SATURATION             0x08    /* saturation */
#define TOUPNAM_PARA_BRIGHTNESS             0x09    /* brightness */
#define TOUPNAM_PARA_GAMMA                  0x0a    /* gamma */
#define TOUPNAM_PARA_AEXPO                  0x0b    /* auto exposure */
#define TOUPNAM_PARA_AWB                    0x0c    /* XCAM1080P:once;  XCAM4K:(0:manual;1:global auto;2:roi) */
#define TOUPNAM_PARA_BINSKIP                0x0d    /* bin / skip */
#define TOUPNAM_PARA_HZ                     0x0e    /* power supply: 0 -> 60HZ AC;  1 -> 50Hz AC;   2 -> DC */
#define TOUPNAM_PARA_BPS                    0x0f    /* bits per second, kbps */
#define TOUPNAM_PARA_KEYFRAME               0x10    /* key frame interval */
#define TOUPNAM_PARA_LOWLIGHTCOMPENSATION   0x11    /* low light compensation */
#define TOUPNAM_PARA_SHARPNESS              0x12    /* sharpness */
#define TOUPNAM_PARA_WBREDGAIN              0x13    /* white balance red gain */
#define TOUPNAM_PARA_WBGREENGAIN            0x14    /* white balance green gain */
#define TOUPNAM_PARA_WBBLUEGAIN             0x15    /* white balance blue gain */
#define TOUPNAM_PARA_DENOISE                0x16    /* denoise */
#define TOUPNAM_PARA_APSTA                  0x17    /* ap/sta */
#define TOUPNAM_PARA_CODEC                  0x18    /* codec, H264, H265, etc */
#define TOUPNAM_PARA_AFPOSITION             0x19    /* auto focus sensor board positon (applicable to both AFDM and EFL) */
#define TOUPNAM_PARA_AFMODE                 0x1a    /* auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration) */
#define TOUPNAM_PARA_AFZONE                 0x1b    /* auto focus zone:
                                                       the whole resolution is divided in w * h zones:
                                                         w = imax >> 16
                                                         h = imax & 0xffff
                                                       then:
                                                            zone row:    value / w
                                                            zone column: value % w
                                                    */
#define TOUPNAM_PARA_AFFEEDBACK             0x1c    /* auto focus information feedback
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
#define TOUPNAM_PARA_AFPOSITION_ABSOLUTE    0x1d    /* absolute auto focus sensor board positon */
#define TOUPNAM_PARA_STATUS                 0x1e    /* status */
#define TOUPNAM_PARA_EVENT                  0x1f    /* event */
#define TOUPNAM_PARA_WBROILEFT              0x20    /* white balance roi left */
#define TOUPNAM_PARA_WBROITOP               0x21    /* white balance roi top */
#define TOUPNAM_PARA_WBROIWIDTH             0x22    /* white balance roi width */
#define TOUPNAM_PARA_WBROIHEIGHT            0x23    /* white balance roi height */
#define TOUPNAM_PARA_VFLIP                  0x24    /* vertical flip */
#define TOUPNAM_PARA_HFLIP                  0x25    /* horizontal flip */
#define TOUPNAM_PARA_CHROME                 0x26    /* monochromatic mode */
#define TOUPNAM_PARA_SIZE                   0x27    /* video width & height */
#define TOUPNAM_PARA_LIGHTADJUSTMENT        0x28    /* light source brightness adjustment */
#define TOUPNAM_PARA_ZOOM                   0x29    /* zoom */
#define TOUPNAM_PARA_EF_MODE                0x2a    /* EFL, auto focus mode, 0: lens calibration mode; 1: manual focus; 2: single focus; 3: auto focus; */
#define TOUPNAM_PARA_EF_FL                  0x2b    /* EFL, focal length */
#define TOUPNAM_PARA_EF_APERTURE            0x2c    /* EFL, F#, 24~16bit:Cur, 15~8bit:Min, 7~0bit:Max */
#define TOUPNAM_PARA_EF_FOCUS_MAX           0x2d    /* EFL, maximum focal length */
#define TOUPNAM_PARA_EF_LENS_ID             0x2e    /* EFL, lens ID */
#define TOUPNAM_PARA_EF_AFMF                0x2f    /* EFL, lens focus mode, 0: AF; 1: MF; */
#define TOUPNAM_PARA_EF_WD_ENABLE           0x30    /* EFL, enable working distance function */
#define TOUPNAM_PARA_EF_WD_NEAR             0x31    /* EFL, near working distance point */
#define TOUPNAM_PARA_EF_WD_FAR              0x32    /* EFL, far working distance point */

#define TOUPNAM_PARA_CHROME_LOCAL           0x80    /* local monochromatic mode */
#define TOUPNAM_PARA_VFLIP_LOCAL            0x81    /* local vertical flip */
#define TOUPNAM_PARA_HFLIP_LOCAL            0x82    /* local horizontal flip */
#define TOUPNAM_PARA_NEGATIVE_LOCAL         0x83    /* local negative film */
#define TOUPNAM_PARA_FORMAT_LOCAL           0x84    /* output format: 0 => BGR888, 1 => BGRA8888, 2 => RGB888, 3 => RGBA8888, 4 => RAW; default: 0
                                                       MUST be set BEFORE StartXXXX
                                                    */

#define TOUPNAM_PARA_STATUS_RECORDING       0x00000001      /* recording */
#define TOUPNAM_PARA_STATUS_SD              0x00000002      /* sd card available */
#define TOUPNAM_PARA_STATUS_SD_FULL         0x00000004      /* sd card full */

#define TOUPNAM_PARA_EVENT_FAT4G            0x00000001      /* file size limit 4g in FAT32 */

#define TOUPNAM_STATE_INITING               0x00    /* initialization */
#define TOUPNAM_STATE_NORMAL                0x01    /* normal */
#define TOUPNAM_STATE_UNREACHABLE           0x02    /* network not reachable */

#define TOUPNAM_FLAG_WIFI_AP                0x00000001  /* wifi ap */
#define TOUPNAM_FLAG_WIFI_STA               0x00000002  /* wifi sta */
#define TOUPNAM_FLAG_ETHERNET               0x00000004  /* eth network */
#define TOUPNAM_FLAG_CAPTURE                0x00000008  /* support the ability of capture image from camera */
#define TOUPNAM_FLAG_AWBCHECKMODE           0x00000010  /* auto white balance: check mode vs 'once' mode */
#define TOUPNAM_FLAG_UVC                    0x00000020  /* uvc camera */
#define TOUPNAM_FLAG_WBGAIN                 0x00000040  /* white balance gain mode or temp tint mode */
#define TOUPNAM_FLAG_MULTICAST              0x00000080  /* RTSP/RTP multicast */
#define TOUPNAM_FLAG_AF                     0x00000100  /* support auto focus */
#define TOUPNAM_FLAG_SD_LIST                0x00000200  /* support to list sd card */
#define TOUPNAM_FLAG_SD                     0x00000400  /* support sd card */
#define TOUPNAM_FLAG_WBROI                  0x00000800  /* white balance: 0:manual;1:global auto;2:roi */
#define TOUPNAM_FLAG_STA_SUPPORT            0x00001000  /* wifi camera has sta mode, app should have sta ssid & password function */
#define TOUPNAM_FLAG_RTP_OVER_RTSP          0x00002000  /* rtp over rtsp */
#define TOUPNAM_FLAG_HZ_AUTOEXPO            0x00004000  /* enable auto exposure when 50/60 hz */
#define TOUPNAM_FLAG_AFDM                   0x00008000
#define TOUPNAM_FLAG_EFL                    0x00010000
#define TOUPNAM_FLAG_CAPTURERAW             0x00020000  /* capture raw image */

typedef struct {
    char            id[64];         /* unique camera id, used for Toupnam_Open */
    char            sn[64];         /* serial number */
    char            name[64];
    char            model[64];
    char            version[64];
    char            addr[64];       /* ip */
    char            url[256];       /* playback url, such as rtsp://xxxx/yyyy */
    unsigned        state;          /* TOUPNAM_STATE_xxx */
    unsigned        flag;           /* TOUPNAM_FLAG_xxx */
    ToupnamRange    range[TOUPNAM_MAX];
} ToupnamDevice;
#endif

typedef struct {
    char    ssid[64];     /* utf8 */
    char    password[64]; /* utf8 */
} ToupnamWifi;

/*
    get the version of this dll/so/dylib, which is: 5.33015.20261024
*/
TOUPNAM_API(const char*)      Toupnam_Version();

#define TOUPNAM_EVENT_ENUM        0x01    /* enum */
#define TOUPNAM_EVENT_WIFI        0x02    /* wifi */
#define TOUPNAM_EVENT_PARA        0x03    /* parameter change TOUPNAM_PARA_xxxx */
#define TOUPNAM_EVENT_IMAGE       0x04    /* image */
#define TOUPNAM_EVENT_LISTWIFI    0x05    /* list wifi finished */
#define TOUPNAM_EVENT_LISTDIR     0x07    /* list dir */
#define TOUPNAM_EVENT_THUMBNAIL   0x08    /* thumbnail */
#define TOUPNAM_EVENT_DIRCHANGE   0x09    /* dir change notify */
#define TOUPNAM_EVENT_RECORDSTART 0x0a    /* record start */
#define TOUPNAM_EVENT_RECORDSTOP  0x0b    /* record stop */
#define TOUPNAM_EVENT_DATETIME    0x0c    /* date time */
#define TOUPNAM_EVENT_ERROR       0x80    /* error */
#define TOUPNAM_EVENT_EOF         0x81    /* end of file */

typedef struct {
    int         result;
    unsigned    length;
    void*       ptr;
    void*       ctx;
} ToupnamEventExtra;

typedef void (__stdcall* PTOUPNAM_EVENT_CALLBACK)(unsigned nEvent, unsigned nPara, void* pCallbackCtx, ToupnamEventExtra* pExtra);
/*
    when toupnam.dll/libtoupnam.so discovery new camera, pCallback will be called.
    pCallback can be NULL if the application does not interest this.
    Toupnam_Init: call only once when application startup
    Toupnam_Fini: call only once when application exit
*/
TOUPNAM_API(void)     Toupnam_Init(PTOUPNAM_EVENT_CALLBACK pCallback, void* pCallbackCtx);
TOUPNAM_API(void)     Toupnam_Fini();

/* enumerate the cameras discovered by the computer, return the number
    sz: size of the array
    when sz is too small, return value will be greater than sz, means that the caller must use bigger array
*/
TOUPNAM_API(unsigned) Toupnam_Enum(ToupnamDevice arr[], unsigned sz);

TOUPNAM_API(HToupnam) Toupnam_Open(const char* camId);
TOUPNAM_API(HToupnam) Toupnam_Open_ByIndex(unsigned index);
TOUPNAM_API(void)     Toupnam_Close(HToupnam h); /* close the handle */

/*
    when frame arrive, pDataCallback is callbacked. (NULL == pData) means that something is error.
    when TOUPNAM_PARA_xxx value is changed, pParaCallback is callbacked.
    pCallbackCtx is the callback context which is passed by Start.
    pDataCallback and pParaCallback are callbacked by an internal thread of toupnam.dll, so please pay attention to multithread problem.
*/
typedef void (__stdcall* PTOUPNAM_PARA_CALLBACK)(unsigned para, int value, void* pCallbackCtx);
typedef void (__stdcall* PTOUPNAM_DATA_CALLBACK)(const void* pData, const BITMAPINFOHEADER* pHeader, void* pCallbackCtx);
typedef void (__stdcall* PTOUPNAM_CAPTURE_CALLBACK)(int result, const void* pData, size_t nLength, const BITMAPINFOHEADER* pHeader, void* pCallbackCtx);

TOUPNAM_API(HRESULT)  Toupnam_StartPushMode(HToupnam h, PTOUPNAM_DATA_CALLBACK pDataCallback, PTOUPNAM_PARA_CALLBACK pParaCallback, void* pCallbackCtx);

#ifdef _WIN32
TOUPNAM_API(HRESULT)  Toupnam_StartPullModeWithWndMsg(HToupnam h, HWND hWnd, UINT nMsg);
#endif

/*
    bits: 24 (RGB24), 32 (RGB32), or 8 (Grey), see: TOUPNAM_PARA_FORMAT_LOCAL
    if RAW format, pnWidth = data size, pnHeight = not used
*/
TOUPNAM_API(HRESULT)  Toupnam_PullImage(HToupnam h, void* pImageData, int bits, unsigned* pnWidth, unsigned* pnHeight);
TOUPNAM_API(HRESULT)  Toupnam_StartPullModeWithCallback(HToupnam h, void* pCallbackContext);

typedef struct {
    unsigned id;        /* 0 is reserved as an invalid id */
    unsigned width;     /* image width */
    unsigned height;    /* image height */
    unsigned length;    /* buf length */
    unsigned char* buf;
} ToupnamImagePtr;

/* Obtains a pointer to the frame buffer directly from the SDK, eliminating the need to copy frame data and thus improving performance */
TOUPNAM_API(HRESULT)  Toupnam_PullImagePtr(HToupnam h, ToupnamImagePtr* ptrImage);

/* After a frame buffer has been used, it must be returned to the SDK for reuse. Please note the following:
    (a) The frame buffer must only be returned back after it is no longer in use. Any access after returning it is unsafe, as the SDK may have already reused the buffer and overwritten the memory with new data.
    (b) If a frame buffer is not returned, the pool of available buffers will gradually decrease.
    (c) Each frame buffer obtained via Pull (identified by its ID) may be returned only once; duplicate returns are not allowed.
    (d) The return order does not need to match the Pull order; buffers may be returned out of order.
    (e) Frame buffers that have not been returned become invalid immediately after Close.
*/
TOUPNAM_API(HRESULT)  Toupnam_PushImagePtr(HToupnam h, unsigned ptrId);

TOUPNAM_API(HRESULT)  Toupnam_Stop(HToupnam h);
TOUPNAM_API(HRESULT)  Toupnam_Pause(HToupnam h, int bPause);

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
TOUPNAM_API(HRESULT)  Toupnam_Capture(HToupnam h, const char* outputFile /* uft8 */, PTOUPNAM_CAPTURE_CALLBACK pCaptureCallback, void* pCallbackCtx);
TOUPNAM_API(HRESULT)  Toupnam_Capture_ById(const char* camId, const char* outputFile /* uft8 */, PTOUPNAM_CAPTURE_CALLBACK pCaptureCallback, void* pCallbackCtx);

TOUPNAM_API(HRESULT)  Toupnam_get_Size_ById(const char* camId, int res, int* pWidth, int* pHeight);

TOUPNAM_API(ToupnamDevice)  Toupnam_get_Device(HToupnam h);
TOUPNAM_API(ToupnamDevice)  Toupnam_get_Inst(HToupnam h);
TOUPNAM_API(HRESULT)  Toupnam_query_Device(HToupnam h, ToupnamDevice* pDevice);

TOUPNAM_API(HRESULT)  Toupnam_get_Size(HToupnam h, int* pWidth, int* pHeight);
TOUPNAM_API(HRESULT)  Toupnam_get_CapSize(HToupnam h, int* pWidth, int* pHeight);
TOUPNAM_API(HRESULT)  Toupnam_get_FourCC(HToupnam h, unsigned* pFourCC); /* see http://en.wikipedia.org/wiki/FourCC, http://www.fourcc.org */

/*
    (outputFile == NULL) means to stop record.
    support file extension: *.asf, *.mp4, *.mkv
*/
TOUPNAM_API(HRESULT)  Toupnam_Record(HToupnam h, const char* outputFile /* utf8 */);
#if defined(_WIN32)
TOUPNAM_API(HRESULT)  Toupnam_RecordW(HToupnam h, const wchar_t* outputFile);
TOUPNAM_API(HRESULT)  Toupnam_RecordA(HToupnam h, const char* outputFile /* ansi */);
#endif

TOUPNAM_API(HRESULT)  Toupnam_put_Para(HToupnam h, unsigned para, int value); /* para is one of TOUPNAM_PARA_xxx */
TOUPNAM_API(HRESULT)  Toupnam_get_Para(HToupnam h, unsigned para, int* value);

TOUPNAM_API(HRESULT)  Toupnam_put_Wifi(const char* camId, ToupnamWifi wifi);
TOUPNAM_API(HRESULT)  Toupnam_get_Wifi(const char* camId);
TOUPNAM_API(HRESULT)  Toupnam_list_Wifi(const char* camId, void* pExtraCtx);

typedef struct {
    unsigned    type;       /* 0 => file, 1 => directory */
    char        name[256];  /* utf8, download the file with the url http://addr/path/name
                                For example, Camera's ip is 192.168.1.2, and file in the sd card directory abc/xyz.mp4, then the url is http://192.168.1.2/abc/xyz.mp4
                                So, it can be downloaded from this url with libcurl or WinInet.
                            */
} ToupnamDirItem;
TOUPNAM_API(HRESULT)  Toupnam_list_Dir(const char* camId, const char* path /* uft8 */, void* pExtraCtx);
TOUPNAM_API(HRESULT)  Toupnam_get_Thumbnail(const char* camId, const char* path /* uft8 */, void* pExtraCtx);
typedef struct {
    unsigned    type; /* 0 => add, 1 => del, 2 => rename */
    char        name[256];    /* utf8 */
    char        newname[256]; /* utf8 */
} ToupnamDirChange;
TOUPNAM_API(HRESULT)  Toupnam_change_Dir(const char* camId, const char* path /* uft8 */, const ToupnamDirChange* dc, unsigned dclength); /* del file or directory, rename file or directory */
TOUPNAM_API(HRESULT)  Toupnam_RecordStart(const char* camId, const char* outputFile /* uft8 */, unsigned recordtime, void* pExtraCtx); /* record to the sd card */
TOUPNAM_API(HRESULT)  Toupnam_RecordStop(const char* camId, void* pExtraCtx); /* stop record to the sd card */

TOUPNAM_API(HRESULT)  Toupnam_get_DateTime(const char* camId, void* pExtraCtx);
TOUPNAM_API(HRESULT)  Toupnam_put_DateTime(const char* camId, time_t t, void* pExtraCtx);

TOUPNAM_API(HRESULT)  Toupnam_put_Para_ById(const char* camId, unsigned para, int value); /* para is one of TOUPNAM_PARA_XXX */
TOUPNAM_API(HRESULT)  Toupnam_get_Para_ById(const char* camId, unsigned para, int* value);

typedef struct HToupnamVideoT { int unused; } *HToupnamVideo;

/* outputFile: file format is based on the file extension, .mp4 used for H264/H265, while .avi used for MJPEG or uncompressed video
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
TOUPNAM_API(HToupnamVideo)    Toupnam_OpenVideo(int width, int height, int fps, int bitrate, int quality, const char* outputFile /* utf8 */, const char* codec);
#if defined(_WIN32)
TOUPNAM_API(HToupnamVideo)    Toupnam_OpenVideoW(int width, int height, int fps, int bitrate, int quality, const wchar_t* outputFile, const char* codec);
TOUPNAM_API(HToupnamVideo)    Toupnam_OpenVideoA(int width, int height, int fps, int bitrate, int quality, const char* outputFile /* ansi */, const char* codec);
#endif
TOUPNAM_API(void)             Toupnam_CloseVideo(HToupnamVideo h);

/* unTimeStamp
    avi: ignored, timestamp is always set to fps
    mp4: ignored when timestamp is set to fps or tick
*/
TOUPNAM_API(HRESULT)          Toupnam_WriteVideo(HToupnamVideo h, const void* inputData, unsigned unTimeStamp);

/* Return value:
      <0 indicates an HRESULT error code;
      =0 means no data is returned;
      >0 indicates the length of the returned data.
   outputData: please ensure that the output buffer is large enough to accommodate the compressed video data.
*/
TOUPNAM_API(HRESULT)          Toupnam_EncodeVideo(HToupnamVideo h, const void* inputData, void* outputData);

#define TOUPNAM_PRIFLAG_RTPOVERRTSP     0x0001  /* rtp over rtsp */
#define TOUPNAM_PRIFLAG_BROADCAST       0x0002  /* broadcast discovery, change this must be before Toupnam_Init */
TOUPNAM_API(void)     Toupnam_PriFlag(unsigned nFlag, unsigned nMask);

/*
    char* arr[] = {
        "1.2.3.4",
        "1.2.3.5",
        ...,
        NULL   // Use NULL pointer to indicate the end of the array
    };
    Toupnam_add_Ip(arr);
*/
TOUPNAM_API(void)     Toupnam_add_Ip(const char* arr[]);
TOUPNAM_API(void)     Toupnam_del_Ip(const char* arr[]);

#ifdef _WIN32
#pragma pack(pop)
#endif

#ifdef __cplusplus
}
#endif

#endif
