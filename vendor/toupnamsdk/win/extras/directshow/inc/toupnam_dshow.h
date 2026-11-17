#ifndef __toupnam_dshow_h__
#define __toupnam_dshow_h__

/* Version: 5.33015.20261024 */

#ifdef _WIN32
#ifndef _INC_WINDOWS
#include <windows.h>
#endif
#endif

#ifndef __TOUPNAMDEVIC_DEFINED__
#define __TOUPNAMDEVIC_DEFINED__

#define TOUPNAM_MAX             64

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
#define TOUPNAM_PARA_AFPOSITION             0x19    /* auto focus sensor board positon */
#define TOUPNAM_PARA_AFMODE                 0x1a    /* auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration) */
#define TOUPNAM_PARA_AFZONE                 0x1b    /* auto focus zone:
                                                       the whole resolution is divided in w * h zones:
                                                         w = imax >> 16
                                                         h = imax & 0xffff
                                                    */
#define TOUPNAM_PARA_AFFEEDBACK             0x1c    /* auto focus information feedback; 0:unknown; 1:focused; 2:focusing; 3:defocus; 4:up; 5:down */
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

#define TOUPNAM_PARA_CHROME_LOCAL           0x80    /* local monochromatic mode */
#define TOUPNAM_PARA_VFLIP_LOCAL            0x81    /* local vertical flip */
#define TOUPNAM_PARA_HFLIP_LOCAL            0x82    /* local horizontal flip */
#define TOUPNAM_PARA_NEGATIVE_LOCAL         0x83    /* local negative film */

#define TOUPNAM_PARA_STATUS_RECORDING       0x00000001      /* recording */
#define TOUPNAM_PARA_STATUS_SD              0x00000002      /* sd card available */
#define TOUPNAM_PARA_STATUS_SD_FULL         0x00000004      /* sd card full */

#define TOUPNAM_PARA_EVENT_FAT4G            0x00000001      /* file size limit 4g in FAT32 */

#define TOUPNAM_STATE_INITING               0x00    /* initialization */
#define TOUPNAM_STATE_NORMAL                0x01    /* normal */
#define TOUPNAM_STATE_UNREACHABLE           0x02    /* network not reachable */

#define TOUPNAM_FLAG_WIFI_AP                0x0001  /* wifi ap */
#define TOUPNAM_FLAG_WIFI_STA               0x0002  /* wifi sta */
#define TOUPNAM_FLAG_ETHERNET               0x0004  /* eth network */
#define TOUPNAM_FLAG_CAPTURE                0x0008  /* support the ability of capture image from camera */
#define TOUPNAM_FLAG_AWBCHECKMODE           0x0010  /* auto white balance: check mode vs 'once' mode */
#define TOUPNAM_FLAG_UVC                    0x0020  /* uvc camera */
#define TOUPNAM_FLAG_WBGAIN                 0x0040  /* white balance gain mode or temp tint mode */
#define TOUPNAM_FLAG_MULTICAST              0x0080  /* RTSP/RTP multicast */
#define TOUPNAM_FLAG_AF                     0x0100  /* support auto focus */
#define TOUPNAM_FLAG_SD_LIST                0x0200  /* support to list sd card */
#define TOUPNAM_FLAG_SD                     0x0400  /* support sd card */
#define TOUPNAM_FLAG_WBROI                  0x0800  /* white balance: 0:manual;1:global auto;2:roi */
#define TOUPNAM_FLAG_RTP_OVER_RTSP          0x1000  /* rtp over rtsp */

typedef struct {
    char            id[64];         /* unique id, used for Toupnam_Open */
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

/* Both the DirectShow source filter and the output pin support this interface.
    That is to say, you can use QueryInterface on the filter or the pin to get the IToupnam interface. */

// {F593CBB6-B326-4A55-9C99-B482EB1A104C}
DEFINE_GUID(IID_IToupnam, 0xf593cbb6, 0xb326, 0x4a55, 0x9c, 0x99, 0xb4, 0x82, 0xeb, 0x1a, 0x10, 0x4c);

DECLARE_INTERFACE_(IToupnam, IUnknown)
{
    STDMETHOD(get_Para) (THIS_ unsigned para, int* value) PURE;
    STDMETHOD(put_Para) (THIS_ unsigned para, int value) PURE;
    STDMETHOD(get_Device) (THIS_ ToupnamDevice* p) PURE;
};

#endif
