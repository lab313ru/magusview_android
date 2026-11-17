Imports System.Runtime.InteropServices
Imports Microsoft.Win32.SafeHandles
#If Not (NETFX_CORE OrElse WINDOWS_UWP) Then
Imports System.Security.Permissions
Imports System.Runtime.ConstrainedExecution
#End If
Imports System.Threading
Imports System.Text

'
'    Version: 5.33015.20261024
'    For Microsoft dotNET Framework & dotNet Core
'    We use P/Invoke to call into the toupnam.dll/so API, the vb.net class Toupnam is a thin wrapper class to the native api of toupnam.dll/so.
'

Public Class Toupnam
    Implements IDisposable

    Public Const MAX As UInteger = 64

    Public Const PARA_UNKNOWN As UInteger = &H0
    Public Const PARA_EXPOTIME As UInteger = &H1    ' exposure time
    Public Const PARA_AGAIN As UInteger = &H2    ' gain
    Public Const PARA_AEXPOTARGET As UInteger = &H3    ' auto exposure target
    Public Const PARA_TEMP As UInteger = &H4    ' color temperature
    Public Const PARA_TINT As UInteger = &H5
    Public Const PARA_CONTRAST As UInteger = &H6    ' contrast
    Public Const PARA_HUE As UInteger = &H7    ' hue
    Public Const PARA_SATURATION As UInteger = &H8    ' saturation
    Public Const PARA_BRIGHTNESS As UInteger = &H9    ' brightness
    Public Const PARA_GAMMA As UInteger = &HA    ' gamma
    Public Const PARA_AEXPO As UInteger = &HB    ' auto exposure
    Public Const PARA_AWB As UInteger = &HC    ' XCAM1080P:once;  XCAM4K:(0:manual;1:global auto;2:roi)
    Public Const PARA_BINSKIP As UInteger = &HD    ' bin / skip
    Public Const PARA_HZ As UInteger = &HE    ' power supply: 0 -> 60HZ AC;  1 -> 50Hz AC;   2 -> DC
    Public Const PARA_BPS As UInteger = &HF    ' bits per second, kbps
    Public Const PARA_KEYFRAME As UInteger = &H10    ' key frame interval
    Public Const PARA_LOWLIGHTCOMPENSATION As UInteger = &H11    ' low light compensation
    Public Const PARA_SHARPNESS As UInteger = &H12    ' sharpness
    Public Const PARA_WBREDGAIN As UInteger = &H13    ' white balance red gain
    Public Const PARA_WBGREENGAIN As UInteger = &H14    ' white balance green gain
    Public Const PARA_WBBLUEGAIN As UInteger = &H15    ' white balance blue gain
    Public Const PARA_DENOISE As UInteger = &H16    ' denoise
    Public Const PARA_APSTA As UInteger = &H17    ' ap/sta
    Public Const PARA_CODEC As UInteger = &H18    ' codec, H264, H265, etc
    Public Const PARA_AFPOSITION As UInteger = &H19    ' auto focus sensor board positon (applicable to both AFDM and EFL)
    Public Const PARA_AFMODE As UInteger = &H1A    ' auto focus mode (0:manul focus; 1:auto focus; 2:once focus; 3:conjugate calibration)
    ' auto focus zone:
    '   the whole resolution is divided in w * h zones:
    '     w = imax >> 16
    '     h = imax & 0xffff
    Public Const PARA_AFZONE As UInteger = &H1B
    ' auto focus information feedback
    '    AFDM:
    '        0: unknown
    '        1: focused
    '        2: focusing
    '        3: defocuse (out of focus)
    '        4: up (workbench move up)
    '        5: down (workbench move down)
    '    EFL:
    '        NA           = 0x0, Not available
    '        PEAKPOINT    = 0x1, Focus completed, find the focus position
    '        DEFOCUS      = 0x2, End of focus, defocus
    '        NEAR         = 0x3, Focusing ended, object too close
    '        FAR          = 0x4, Focusing ended, object too far
    '        ROICHANGED   = 0x5, Focusing ends, roi changes
    '        SCENECHANGED = 0x6, Focusing ends, scene changes
    '        MODECHANGED  = 0x7, The end of focusing and the change in focusing mode is usually determined by the user moderator
    '        UNFINISH     = 0x8, The focus is not complete. At the beginning of focusing, it will be set as incomplete
    Public Const PARA_AFFEEDBACK As UInteger = &H1C
    Public Const PARA_AFPOSITION_ABSOLUTE As UInteger = &H1D    ' absolute auto focus sensor board positon
    Public Const PARA_STATUS As UInteger = &H1E    ' status
    Public Const PARA_EVENT As UInteger = &H1F    ' event
    Public Const PARA_WBROILEFT As UInteger = &H20    ' white balance roi left
    Public Const PARA_WBROITOP As UInteger = &H21    ' white balance roi top
    Public Const PARA_WBROIWIDTH As UInteger = &H22    ' white balance roi width
    Public Const PARA_WBROIHEIGHT As UInteger = &H23    ' white balance roi height
    Public Const PARA_VFLIP As UInteger = &H24    ' vertical flip
    Public Const PARA_HFLIP As UInteger = &H25    ' horizontal flip
    Public Const PARA_CHROME As UInteger = &H26    ' monochromatic mode
    Public Const PARA_SIZE As UInteger = &H27    ' video width & height
    Public Const PARA_LIGHTADJUSTMENT As UInteger = &H28    ' light source brightness adjustment
    Public Const PARA_ZOOM As UInteger = &H29    ' zoom
    Public Const PARA_EF_MODE As UInteger = &H2A    ' EFL, auto focus mode, 0: lens calibration mode; 1: manual focus; 2: single focus; 3: auto focus;
    Public Const PARA_EF_FL As UInteger = &H2B    ' EFL, focal length
    Public Const PARA_EF_APERTURE As UInteger = &H2C ' EFL, F#, 24~16bit:Cur, 15~8bit:Min, 7~0bit:Max
    Public Const PARA_EF_FOCUS_MAX As UInteger = &H2D    ' EFL, maximum focal length
    Public Const PARA_EF_LENS_ID As UInteger = &H2E    ' EFL, lens ID
    Public Const PARA_EF_AFMF As UInteger = &H2F    ' EFL, lens focus mode, 0: AF; 1: MF;
    Public Const PARA_EF_WD_ENABLE As UInteger = &H30    ' EFL, enable working distance function
    Public Const PARA_EF_WD_NEAR As UInteger = &H31    ' EFL, near working distance point
    Public Const PARA_EF_WD_FAR As UInteger = &H32    ' EFL, far working distance point

    Public Const PARA_CHROME_LOCAL As UInteger = &H80    ' local monochromatic mode
    Public Const PARA_VFLIP_LOCAL As UInteger = &H81    ' local vertical flip
    Public Const PARA_HFLIP_LOCAL As UInteger = &H82    ' local horizontal flip
    Public Const PARA_NEGATIVE_LOCAL As UInteger = &H83    ' local negative film
    ' output format: 0 => BGR888, 1 => BGRA8888, 2 => RGB888, 3 => RGBA8888, 4 => RAW; default: 0
    ' MUST be set BEFORE StartXXXX
    Public Const PARA_FORMAT_LOCAL As UInteger = &H84

    Public Const PARA_STATUS_RECORDING As UInteger = &H1      ' recording
    Public Const PARA_STATUS_SD As UInteger = &H2      ' sd card available
    Public Const PARA_STATUS_SD_FULL As UInteger = &H4      ' sd card full

    Public Const PARA_EVENT_FAT4G As UInteger = &H1      ' file size limit 4g in FAT32

    Public Const STATE_INITING As UInteger = &H0    ' initialization
    Public Const STATE_NORMAL As UInteger = &H1    ' normal
    Public Const STATE_UNREACHABLE As UInteger = &H2    ' network not reachable

    Public Const FLAG_WIFI_AP As UInteger = &H1
    Public Const FLAG_WIFI_STA As UInteger = &H2
    Public Const FLAG_ETHERNET As UInteger = &H4
    Public Const FLAG_CAPTURE As UInteger = &H8  ' support the ability of capture image from camera
    Public Const FLAG_AWBCHECKMODE As UInteger = &H10  ' auto white balance: check mode vs 'once' mode
    Public Const FLAG_UVC As UInteger = &H20  ' uvc camera
    Public Const FLAG_WBGAIN As UInteger = &H40  ' white balance gain mode or temp tint mode
    Public Const FLAG_MULTICAST As UInteger = &H80  ' RTSP/RTP multicast
    Public Const FLAG_AF As UInteger = &H100  ' support auto focus
    Public Const FLAG_SD_LIST As UInteger = &H200  ' support to list sd card
    Public Const FLAG_SD As UInteger = &H400  ' support sd card
    Public Const FLAG_WBROI As UInteger = &H800  ' white balance: 0:manual;1:global auto;2:roi
    Public Const FLAG_STA_SUPPORT As UInteger = &H1000  ' wifi camera has sta mode, app should have sta ssid & password function
    Public Const FLAG_RTP_OVER_RTSP As UInteger = &H2000  ' rtp over rtsp
    Public Const FLAG_HZ_AUTOEXPO As UInteger = &H4000  ' enable auto exposure when 50/60 hz
    Public Const FLAG_AFDM As UInteger = &H8000
    Public Const FLAG_FLAG_EFL As UInteger = &H10000
    Public Const FLAG_CAPTURERAW As UInteger = &H20000  ' capture raw image

    Public Const EVENT_ENUM As UInteger = &H1    ' enum
    Public Const EVENT_WIFI As UInteger = &H2    ' wifi
    Public Const EVENT_PARA As UInteger = &H3    ' parameter change TOUPNAM_PARA_xxxx
    Public Const EVENT_IMAGE As UInteger = &H4    ' image
    Public Const EVENT_LISTWIFI As UInteger = &H5    ' list wifi finished
    Public Const EVENT_LISTDIR As UInteger = &H7    ' list dir
    Public Const EVENT_THUMBNAIL As UInteger = &H8    ' thumbnail
    Public Const EVENT_DIRCHANGE As UInteger = &H9    ' dir change notify
    Public Const EVENT_RECORDSTART As UInteger = &HA    ' record start
    Public Const EVENT_RECORDSTOP As UInteger = &HB    ' record stop
    Public Const EVENT_DATETIME As UInteger = &HC    ' date time
    Public Const EVENT_ERROR As UInteger = &H80    ' error
    Public Const EVENT_EOF As UInteger = &H81    ' end of file

    <StructLayout(LayoutKind.Sequential)>
    Public Structure range
        Public idisable As Integer   ' 0 = "support this feature", 1 = "not support"
        Public imin As Integer       ' minimum value
        Public imax As Integer       ' maximum value
        Public idef As Integer       ' default value
    End Structure

    <StructLayout(LayoutKind.Sequential, CharSet:=CharSet.Ansi)>
    Public Structure device
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public id As String          ' unique camera id, used for Open
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public sn As String          ' serial number
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public name As String
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public model As String
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public version As String
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public addr As String        ' ip
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=256)>
        Public url As String         ' playback url, such as rtsp://xxxx/yyyy
        Public state As UInteger     ' STATE_xxx
        Public flag As UInteger      ' FLAG_xxx
        <MarshalAs(UnmanagedType.ByValArray, SizeConst:=CInt(MAX))>
        Public r As range()
    End Structure

    <StructLayout(LayoutKind.Sequential)>
    Public Structure imageptr
        Public id As UInteger        ' 0 Is reserved As an invalid id
        Public width As UInteger     ' image width
        Public height As UInteger    ' image height
        Public length As UInteger    ' buf length
        Public buf As IntPtr
    End Structure

    <StructLayout(LayoutKind.Sequential, CharSet:=CharSet.Ansi)>
    Public Structure wifi
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public ssid As String
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=64)>
        Public password As String
    End Structure

    <StructLayout(LayoutKind.Sequential)>
    Public Structure eventextra
        Public result As Integer
        Public length As UInteger
        Public ptr As IntPtr
        Public ctx As IntPtr
    End Structure

    <StructLayout(LayoutKind.Sequential, CharSet:=CharSet.Ansi)>
    Public Structure diritem
        Public type As UInteger         ' 0 => file, 1 => directory
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=256)>
        Public name As String           ' download the file with the url http://addr/path/name
        '   For example, Camera's ip is 192.168.1.2, and file in the sd card directory abc/xyz.mp4, then the url is http://192.168.1.2/abc/xyz.mp4
        '   So, it can be downloaded from this url with libcurl or WinInet.
    End Structure

    <StructLayout(LayoutKind.Sequential, CharSet:=CharSet.Ansi)>
    Public Structure dirchange
        Public type As UInteger         ' 0 => add, 1 => del, 2 => rename
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=256)>
        Public name As String
        <MarshalAs(UnmanagedType.ByValTStr, SizeConst:=256)>
        Public newname As String
    End Structure

    Public Structure bitmapinfo
        Public width As Integer
        Public height As Integer
        Public Sub New(w As Integer, h As Integer)
            width = w
            height = h
        End Sub
    End Structure

    <StructLayout(LayoutKind.Sequential)>
    Public Structure BITMAPINFOHEADER
        Public biSize As UInteger
        Public biWidth As Integer
        Public biHeight As Integer
        Public biPlanes As UShort
        Public biBitCount As UShort
        Public biCompression As UInteger
        Public biSizeImage As UInteger
        Public biXPelsPerMeter As Integer
        Public biYPelsPerMeter As Integer
        Public biClrUsed As UInteger
        Public biClrImportant As UInteger
    End Structure

    '
    '   when frame arrive, DelegateEventCallback is callbacked. (NULL == pData) means that something is error.
    '   when PARA_xxx value is changed, DelegateParaCallback is callbacked.
    '   pCallbackCtx is the callback context which is passed by Start.
    '   DelegateEventCallback and DelegateParaCallback are callbacked by an internal thread of toupnam.dll, so please pay attention to multithread problem.
    '
    Public Delegate Sub DelegateEventCallback(nEvent As UInteger, nPara As UInteger, ByRef pExtra As eventextra)
    Public Delegate Sub DelegateParaCallback(para As UInteger, val As Integer)
    Public Delegate Sub DelegateDataCallback(pData As IntPtr, ByRef info As bitmapinfo)
    Public Delegate Sub DelegateCaptureCallback(result As Integer, pData As IntPtr, nLength As Integer, ByRef info As bitmapinfo)

    <UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)>
    Private Delegate Sub PFUN_EVENT_CALLBACK(nEvent As UInteger, nPara As UInteger, pCallbackCtx As IntPtr, ByRef pExtra As eventextra)
    <UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)>
    Private Delegate Sub PFUN_PARA_CALLBACK(para As UInteger, val As Integer, pCallbackCtx As IntPtr)
    <UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)>
    Private Delegate Sub PFUN_DATA_CALLBACK(pData As IntPtr, ByRef pHeader As BITMAPINFOHEADER, pCallbackCtx As IntPtr)
    <UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)>
    Private Delegate Sub PFUN_CAPTURE_CALLBACK(result As Integer, pData As IntPtr, nLength As IntPtr, ByRef pHeader As BITMAPINFOHEADER, pCallbackCtx As IntPtr)

#If Not (WINDOWS_UWP) Then
    Public Class SafeCamHandle
        Inherits SafeHandleZeroOrMinusOneIsInvalid
        <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
        Private Shared Sub Toupnam_Close(h As IntPtr)
        End Sub

        Public Sub New()
            MyBase.New(True)
        End Sub

        <ReliabilityContract(Consistency.WillNotCorruptState, Cer.MayFail)>
        Protected Overrides Function ReleaseHandle() As Boolean
            ' Here, we must obey all rules for constrained execution regions.
            Toupnam_Close(handle)
            Return True
        End Function
    End Class
#Else
    Public Class SafeCamHandle
        Inherits SafeHandle
        <DllImport("tounam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
        Private Shared Sub Toupnam_Close(h As IntPtr)
        End Sub
        
        Public Sub New()
            MyBase.New(IntPtr.Zero, True)
        End Sub
        
        Protected Overrides Function ReleaseHandle() As Boolean
            Toupnam_Close(handle)
            Return True
        End Function
        
        Public Overrides ReadOnly Property IsInvalid() As Boolean
            Get
                Return MyBase.handle = IntPtr.Zero
            End Get
        End Property
    End Class
#End If

    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Version() As IntPtr
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Sub Toupnam_Init(pCallback As PFUN_EVENT_CALLBACK, pCallbackCtx As IntPtr)
    End Sub
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Sub Toupnam_Fini()
    End Sub
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Enum(arr As IntPtr, sz As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Open(<MarshalAs(UnmanagedType.LPStr)> camId As String) As SafeCamHandle
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Open_ByIndex(index As UInteger) As SafeCamHandle
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Close(h As SafeCamHandle)
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_StartPushMode(h As SafeCamHandle, pDataCallback As PFUN_DATA_CALLBACK, pParaCallback As PFUN_PARA_CALLBACK, pCallbackCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_StartPullModeWithWndMsg(h As SafeCamHandle, hWnd As IntPtr, nMsg As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_PullImage(h As SafeCamHandle, pImageData As IntPtr, bits As Integer, ByRef pnWidth As UInteger, ByRef pnHeight As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_PullImagePtr(h As SafeCamHandle, ByRef ptrImage As imageptr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_PushImagePtr(h As SafeCamHandle, ptrId As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_StartPullModeWithCallback(h As SafeCamHandle, pCallbackContext As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Stop(h As SafeCamHandle) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Pause(h As SafeCamHandle, bPause As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Capture(h As SafeCamHandle, outputFile As Byte(), pCaptureCallback As PFUN_CAPTURE_CALLBACK, pCallbackCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Capture_ById(<MarshalAs(UnmanagedType.LPStr)> camId As String, outputFile As Byte(), pCaptureCallback As PFUN_CAPTURE_CALLBACK, pCallbackCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_query_Device(h As SafeCamHandle, ptr As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Size(h As SafeCamHandle, ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_CapSize(h As SafeCamHandle, ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_FourCC(h As SafeCamHandle, ByRef pFourCC As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_Record(h As SafeCamHandle, outputFile As Byte()) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_put_Para(h As SafeCamHandle, para As UInteger, val As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Para(h As SafeCamHandle, para As UInteger, ByRef val As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_put_Wifi(<MarshalAs(UnmanagedType.LPStr)> camId As String, wf As wifi) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Wifi(<MarshalAs(UnmanagedType.LPStr)> camId As String) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_list_Wifi(<MarshalAs(UnmanagedType.LPStr)> camId As String, pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_list_Dir(<MarshalAs(UnmanagedType.LPStr)> camId As String, path As Byte(), pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Thumbnail(<MarshalAs(UnmanagedType.LPStr)> camId As String, path As Byte(), pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_change_Dir(<MarshalAs(UnmanagedType.LPStr)> camId As String, path As Byte(), dc As dirchange(), dclength As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_RecordStart(<MarshalAs(UnmanagedType.LPStr)> camId As String, outputFile As Byte(), recordtime As UInteger, pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_RecordStop(<MarshalAs(UnmanagedType.LPStr)> camId As String, pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_DateTime(<MarshalAs(UnmanagedType.LPStr)> camId As String, pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_put_DateTime(<MarshalAs(UnmanagedType.LPStr)> camId As String, t As ULong, pExtraCtx As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_put_Para_ById(<MarshalAs(UnmanagedType.LPStr)> camId As String, para As UInteger, val As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Para_ById(<MarshalAs(UnmanagedType.LPStr)> camId As String, para As UInteger, ByRef val As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_get_Size_ById(<MarshalAs(UnmanagedType.LPStr)> camId As String, res As Integer, ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Sub Toupnam_PriFlag(nFlag As UInteger, nMask As UInteger)
    End Sub
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Sub Toupnam_add_Ip(arr As IntPtr())
    End Sub
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Sub Toupnam_del_Ip(arr As IntPtr())
    End Sub

    Public Shared Function TDIBWIDTHBYTES(ByVal bits As Integer) As Integer
        Return ((bits + 31) And (Not 31)) / 8
    End Function

    Private Class cbobj
        Public de As [Delegate]
        Public id As Integer
        Public Sub New(d As [Delegate], i As Integer)
            de = d
            id = i
        End Sub
    End Class

    Private Shared _sid As Integer = 0
    Private Shared _event_callback As PFUN_EVENT_CALLBACK = New PFUN_EVENT_CALLBACK(AddressOf OnEventCallback)
    Private Shared _capture_callback As PFUN_CAPTURE_CALLBACK
    Private Shared _dict As Dictionary(Of Integer, cbobj) = New Dictionary(Of Integer, cbobj)()
    Private Shared _map As Dictionary(Of Integer, Toupnam) = New Dictionary(Of Integer, Toupnam)()

    Private _handle As SafeCamHandle
    Private _id As Integer
    Private _paradele As DelegateParaCallback
    Private _datadele As DelegateDataCallback
    Private _paracallback As PFUN_PARA_CALLBACK
    Private _datacallback As PFUN_DATA_CALLBACK

    Private Shared Sub OnCaptureCallback(result As Integer, pData As IntPtr, nLength As IntPtr, ByRef pHeader As BITMAPINFOHEADER, pCallbackCtx As IntPtr)
        Dim d As cbobj = Nothing
        If (_dict.TryGetValue(pCallbackCtx.ToInt32(), d) AndAlso (d IsNot Nothing)) Then
            Dim e As DelegateCaptureCallback = TryCast(d.de, DelegateCaptureCallback)
            If e IsNot Nothing Then
                e(result, pData, nLength.ToInt32(), New bitmapinfo(pHeader.biWidth, pHeader.biHeight))
            End If
        End If
    End Sub

    Private Shared Sub OnEventCallback(nEvent As UInteger, nPara As UInteger, pCallbackCtx As IntPtr, ByRef pExtra As eventextra)
        Dim d As cbobj = Nothing
        If (_dict.TryGetValue(pCallbackCtx.ToInt32(), d) AndAlso (d IsNot Nothing)) Then
            Dim e As DelegateEventCallback = TryCast(d.de, DelegateEventCallback)
            If e IsNot Nothing Then
                e(nEvent, nPara, pExtra)
            End If
        End If
    End Sub

    Protected Overrides Sub Finalize()
        Try
            Dispose(False)
        Finally
            MyBase.Finalize()
        End Try
    End Sub

#If Not (NETFX_CORE OrElse WINDOWS_UWP) Then
    <SecurityPermission(SecurityAction.Demand, UnmanagedCode:=True)>
    Protected Overridable Sub Dispose(disposing As Boolean)
#Else
    Protected Overridable Sub Dispose(disposing As Boolean)
#End If
        ' Note there are three interesting states here:
        ' 1) CreateFile failed, _handle contains an invalid handle
        ' 2) We called Dispose already, _handle is closed.
        ' 3) _handle is null, due to an async exception before
        '    calling CreateFile. Note that the finalizer runs
        '    if the constructor fails.
        If _handle IsNot Nothing AndAlso Not _handle.IsInvalid Then
            ' Free the handle
            _handle.Dispose()
        End If
        ' SafeHandle records the fact that we've called Dispose.
    End Sub

    '
    '   the object of Toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = New Toupnam (The constructor is private on purpose)
    '
    Private Sub New(h As SafeCamHandle)
        _handle = h
        _id = Interlocked.Increment(_sid)
        _map.Add(_id, Me)
    End Sub

    Public Sub Dispose() Implements IDisposable.Dispose
        ' Follow the Dispose pattern - Public nonvirtual
        Dispose(True)
        _map.Remove(_id)
        GC.SuppressFinalize(Me)
    End Sub

    Public Sub Close()
        Me.Dispose()
    End Sub

    '
    ' get the version of this dll/so/dylib, which is: 5.33015.20261024
    '
    Public Shared Function Version() As String
        Return Marshal.PtrToStringAnsi(Toupnam_Version())
    End Function

    ' when toupnam.dll/libtoupnam.so discovery new camera, pCallback will be called.
    ' pCallback can be Nothing if the application does not interest this.
    ' Init: call only once when application startup
    ' Fini: call only once when application exit
    Public Shared Sub Init(pCallback As DelegateEventCallback)
        If pCallback IsNot Nothing Then
            _dict.Add(0, New cbobj(pCallback, 0))
        End If
        Toupnam_Init(_event_callback, IntPtr.Zero)
    End Sub

    Public Shared Sub Fini()
        Toupnam_Fini()
    End Sub

    ' enumerate the cameras discovered by the computer, return the number
    ' sz: size of the array
    '   when sz is too small, return value will be greater than sz, means that the caller must use bigger array
    '
    Public Shared Function [Enum]() As device()
        Dim sz As Integer = CInt(MAX)
        Do
            Dim ti As IntPtr = Marshal.AllocHGlobal(Marshal.SizeOf(GetType(device)) * sz)
            Dim n As Integer = Toupnam_Enum(ti, sz)
            If (n > sz) Then
                Marshal.FreeHGlobal(ti)
                sz = CInt(n)
            ElseIf (0 = n) Then
                Marshal.FreeHGlobal(ti)
                Return New device(-1) {}
            Else
                Dim arr As device() = New device(n - 1) {}
                For i As Integer = 0 To n - 1
#If Not (NETFX_CORE OrElse WINDOWS_UWP) Then
                    arr(i) = CType(Marshal.PtrToStructure(ti + Marshal.SizeOf(GetType(device)) * i, GetType(device)), device)
#Else
                    arr(i) = Marshal.PtrToStructure(Of device)(ti + Marshal.SizeOf(typeof(device)) * i)
#End If
                Next
                Marshal.FreeHGlobal(ti)
                Return arr
            End If
        Loop While True
        Return Nothing
    End Function

    '
    '   the object of Toupnam must be obtained by static mothod Open or Open_ByIndex, it cannot be obtained by obj = New Toupnam (The constructor is private on purpose)
    '
    Public Shared Function Open(camId As String) As Toupnam
        Dim h As SafeCamHandle = Toupnam_Open(camId)
        If h Is Nothing OrElse h.IsInvalid OrElse h.IsClosed Then
            Return Nothing
        End If
        Return New Toupnam(h)
    End Function

    Public Shared Function Open_ByIndex(index As UInteger) As Toupnam
        Dim h As SafeCamHandle = Toupnam_Open_ByIndex(index)
        If h.IsInvalid Then
            Return Nothing
        End If

        Return New Toupnam(h)
    End Function

    Private Shared Sub OnParaCallback(para As UInteger, val As Integer, pCallbackCtx As IntPtr)
        Dim pthis As Toupnam = Nothing
        If _map.TryGetValue(pCallbackCtx.ToInt32(), pthis) AndAlso pthis IsNot Nothing AndAlso pthis._paradele IsNot Nothing Then
            pthis._paradele(para, val)
        End If
    End Sub

    Private Shared Sub OnDataCallback(pData As IntPtr, ByRef pHeader As BITMAPINFOHEADER, pCallbackCtx As IntPtr)
        Dim pthis As Toupnam = Nothing
        If _map.TryGetValue(pCallbackCtx.ToInt32(), pthis) AndAlso pthis IsNot Nothing AndAlso pthis._datadele IsNot Nothing Then
            Dim info As bitmapinfo = New bitmapinfo(pHeader.biWidth, pHeader.biHeight)
            pthis._datadele(pData, info)
        End If
    End Sub

    Public Function StartPushMode(pDataCallback As DelegateDataCallback, pParaCallback As DelegateParaCallback) As Integer
        _datadele = pDataCallback
        _paradele = pParaCallback
        _datacallback = New PFUN_DATA_CALLBACK(AddressOf OnDataCallback)
        _paracallback = New PFUN_PARA_CALLBACK(AddressOf OnParaCallback)
        Return Toupnam_StartPushMode(_handle, _datacallback, _paracallback, New IntPtr(_id))
    End Function

    Public Function StartPullModeWithWndMsg(hWnd As IntPtr, nMsg As UInteger) As Integer
        Return Toupnam_StartPullModeWithWndMsg(_handle, hWnd, nMsg)
    End Function

    ' Obtains a pointer to the frame buffer directly from the SDK, eliminating the need to copy frame data and thus improving performance
    Public Function PullImagePtr(ByRef ptrImage As imageptr) As Integer
        Return Toupnam_PullImagePtr(_handle, ptrImage)
    End Function

    ' After a frame buffer has been used, it must be returned to the SDK for reuse. Please note the following:
    '   (a) The frame buffer must only be returned back after it is no longer in use. Any access after returning it is unsafe, as the SDK may have already reused the buffer and overwritten the memory with new data.
    '   (b) If a frame buffer is not returned, the pool of available buffers will gradually decrease.
    '   (c) Each frame buffer obtained via Pull (identified by its ID) may be returned only once; duplicate returns are not allowed.
    '   (d) The return order does not need to match the Pull order; buffers may be returned out of order.
    '   (e) Frame buffers that have not been returned become invalid immediately after Close.
    Public Function PushImagePtr(ptrId As UInteger) As Integer
        Return Toupnam_PushImagePtr(_handle, ptrId)
    End Function

    '
    ' bits: 24 (RGB24), 32 (RGB32), or 8 (Grey), see: TOUPNAM_PARA_FORMAT_LOCAL
    ' if RAW format, pnWidth = data size, pnHeight = not used
    '
    Public Function PullImage(pImageData As IntPtr, bits As Integer, ByRef pnWidth As UInteger, ByRef pnHeight As UInteger) As Integer
        Return Toupnam_PullImage(_handle, pImageData, bits, pnWidth, pnHeight)
    End Function

    Public Function PullImage(pImageData As Byte(), bits As Integer, ByRef pnWidth As UInteger, ByRef pnHeight As UInteger) As Integer
        Dim gch As GCHandle = GCHandle.Alloc(pImageData, GCHandleType.Pinned)
        Try
            Return PullImage(gch.AddrOfPinnedObject(), bits, pnWidth, pnHeight)
        Finally
            gch.Free()
        End Try
    End Function

    Public Function StartPullModeWithCallback(pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, _id))
        End If
        Dim ret As Integer = Toupnam_StartPullModeWithCallback(_handle, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Function [Stop]() As Integer
        Return Toupnam_Stop(_handle)
    End Function

    Public Function Pause(bPause As Boolean) As Integer
        Return Toupnam_Pause(_handle, If(bPause, 1, 0))
    End Function

    ' capture image, compare this to image extracted from video
    '   outputFile:
    '            Nothing     -> capture image and then return by callback
    '           "raw"        -> capture raw image and then return by callback
    '            "abc.jpg"   -> capture image and then save it in the camera sd card with filename 'abc.jpg'
    '            "abc.raw"   -> capture raw image and then save it in the camera sd card with filename 'abc.raw'
    '            "thumbnail" -> capture the thumbnail image and then return by callback
    '            "*"         -> capture image and then save it in the camera sd card with auto generated file name
    '            "*.raw"     -> capture raw image and then save it in the camera sd card with auto generated file name
    '
    Public Function Capture(outputFile As String, pCaptureCallback As DelegateCaptureCallback) As Integer
        Dim sid As Integer = 0
        If pCaptureCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCaptureCallback, _id))
        End If
        If _capture_callback Is Nothing Then
            _capture_callback = New PFUN_CAPTURE_CALLBACK(AddressOf OnCaptureCallback)
        End If
        Dim ret As Integer = Toupnam_Capture(_handle, Encoding.UTF8.GetBytes(outputFile & ChrW(0)), _capture_callback, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCaptureCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Shared Function Capture_ById(camId As String, outputFile As String, pCaptureCallback As DelegateCaptureCallback) As Integer
        Dim sid As Integer = 0
        If pCaptureCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCaptureCallback, 0))
        End If
        If _capture_callback Is Nothing Then
            _capture_callback = New PFUN_CAPTURE_CALLBACK(AddressOf OnCaptureCallback)
        End If
        Dim ret As Integer = Toupnam_Capture_ById(camId, Encoding.UTF8.GetBytes(outputFile & ChrW(0)), _capture_callback, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCaptureCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Function get_Device() As device
        Dim dev As device = New device
        Dim ptr As IntPtr = Marshal.AllocHGlobal(Marshal.SizeOf(GetType(device)))
        If Toupnam_query_Device(_handle, ptr) >= 0 Then
#If Not (NETFX_CORE OrElse WINDOWS_UWP) Then
            dev = CType(Marshal.PtrToStructure(ptr, GetType(device)), device)
#Else
            dev = Marshal.PtrToStructure(Of device)(ptr)
#End If
        End If
        Marshal.FreeHGlobal(ptr)
        Return dev
    End Function

    Public Function get_Size(ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
        Return Toupnam_get_Size(_handle, pWidth, pHeight)
    End Function

    Public Function get_CapSize(ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
        Return Toupnam_get_CapSize(_handle, pWidth, pHeight)
    End Function

    Public Function get_FourCC(ByRef pFourCC As UInteger) As Integer
        Return Toupnam_get_FourCC(_handle, pFourCC)
    End Function

    '
    '    (outputFile == Nothing) means to stop record.
    '    support file extension: *.asf, *.mp4, *.mkv
    '
    Public Function Record(outputFile As String) As Integer
        If String.IsNullOrEmpty(outputFile) Then
            Return Toupnam_Record(_handle, Nothing)
        Else
            Return Toupnam_Record(_handle, Encoding.UTF8.GetBytes(outputFile & ChrW(0)))
        End If
    End Function

    ' para Is one of PARA_xxx
    '
    Public Function put_Para(para As UInteger, val As Integer) As Integer
        Return Toupnam_put_Para(_handle, para, val)
    End Function

    Public Function get_Para(para As UInteger, ByRef val As Integer) As Integer
        Return Toupnam_get_Para(_handle, para, val)
    End Function

    Public Shared Function put_Wifi(camId As String, wf As wifi) As Integer
        Return Toupnam_put_Wifi(camId, wf)
    End Function

    Public Shared Function get_Wifi(camId As String) As Integer
        Return Toupnam_get_Wifi(camId)
    End Function

    Public Shared Function list_Wifi(camId As String, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_list_Wifi(camId, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Shared Function list_Dir(camId As String, path As String, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_list_Dir(camId, Encoding.UTF8.GetBytes(path & ChrW(0)), New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Shared Function get_Thumbnail(camId As String, path As String, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_get_Thumbnail(camId, Encoding.UTF8.GetBytes(path & ChrW(0)), New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    ' del file Or directory, rename file or directory
    '
    Public Shared Function change_Dir(camId As String, path As String, dc As dirchange()) As Integer
        Return Toupnam_change_Dir(camId, Encoding.UTF8.GetBytes(path & ChrW(0)), dc, dc.Length)
    End Function

    ' record to the sd card
    '
    Public Shared Function RecordStart(camId As String, outputFile As String, recordtime As UInteger, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_RecordStart(camId, Encoding.UTF8.GetBytes(outputFile & ChrW(0)), recordtime, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    ' stop record to the sd card
    '
    Public Shared Function RecordStop(camId As String, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_RecordStop(camId, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Shared Function get_DateTime(camId As String, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_get_DateTime(camId, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    Public Shared Function put_DateTime(camId As String, t As Long, pCallback As DelegateEventCallback) As Integer
        Dim sid As Integer = 0
        If pCallback IsNot Nothing Then
            sid = Interlocked.Increment(_sid)
            _dict.Add(sid, New cbobj(pCallback, 0))
        End If
        Dim ret As Integer = Toupnam_put_DateTime(camId, t, New IntPtr(sid))
        If ((ret < 0) AndAlso (pCallback IsNot Nothing)) Then
            _dict.Remove(sid)
        End If
        Return ret
    End Function

    ' para is one of PARA_XXX
    '
    Public Shared Function put_Para_ById(camId As String, para As UInteger, val As Integer) As Integer
        Return Toupnam_put_Para_ById(camId, para, val)
    End Function

    Public Shared Function get_Para_ById(camId As String, para As UInteger, ByRef val As Integer) As Integer
        Return Toupnam_get_Para_ById(camId, para, val)
    End Function

    Public Shared Function get_Size_ById(camId As String, res As Integer, ByRef pWidth As Integer, ByRef pHeight As Integer) As Integer
        Return Toupnam_get_Size_ById(camId, res, pWidth, pHeight)
    End Function

    Public Shared Sub PriFlag(nFlag As UInteger, nMask As UInteger)
        Toupnam_PriFlag(nFlag, nMask)
    End Sub

    ' arr = { "1.2.3.4", "1.2.3.5", ... }
    '
    Public Shared Sub add_Ip(arr() As String)
        Dim newarr As IntPtr() = New IntPtr(arr.Length) {}
        For i As Integer = 0 To arr.Length
            newarr(i) = Marshal.StringToHGlobalAnsi(arr(i))
        Next
        Toupnam_add_Ip(newarr)
        For i As Integer = 0 To arr.Length
            Marshal.FreeHGlobal(newarr(i))
        Next
    End Sub

    Public Shared Sub del_Ip(arr() As String)
        Dim newarr As IntPtr() = New IntPtr(arr.Length) {}
        For i As Integer = 0 To arr.Length
            newarr(i) = Marshal.StringToHGlobalAnsi(arr(i))
        Next
        Toupnam_del_Ip(newarr)
        For i As Integer = 0 To arr.Length
            Marshal.FreeHGlobal(newarr(i))
        Next
    End Sub
End Class

Public Class EncVideo
    Implements IDisposable

    '
    ' the object of EncVideo must be obtained by static mothod Open, it cannot be obtained by obj = New EncVideo (The constructor is private on purpose)
    '
    ' outputFile: file format is based on the file extension, .mp4 used for H264/H265, while .avi used for MJPEG or uncompressed video
    '  If outputFile is null, the compressed video data will not be written to a file.
    '  Instead, the encoded output will be written to the memory buffer pointed to by outData through the Toupnam_EncodeVideo function.
    '
    ' codec:
    '    h264_nvenc, hevc_nvenc: Nvidia GPU
    '    h264_qsv, hevc_qsv: Intel GPU (x64 only)
    '    h264_amf, hevc_amf: AMD GPU (x64 only)
    '    h264_v4l2m2m, hevc_v4l2m2m: V4L2 memory-to-memory device (linux arm64 only)
    '    libx264: software
    '    libx265: software
    '    mjpeg: motion jpeg
    '    rawvideo: uncompressed
    ' use : to split extra parameters, such as:
    '    input format: rgb24, bgr24, rgba, bgra, gray8, such as: "h264_nvenc:rgb24"; default = bgr24
    '    mono: used for uncompressed avi, "rawvideo:mono", reduce the size of video file by 2/3
    '    timestamp: fps(use fps for timestamp), tick(use os tick for timestamp), param(use the input value of the function parameter), such as: "timestamp=fps"; default = tick
    '    stride: 0(padded to a 4-byte boundary, see TDIBWIDTHBYTES), -1(no padding), positive integer(use this specific value); default = 0
    '
    ' always use Constant Quality Mode
    '    quality = [1, 100]
    '    bitrate = 0
    '
    Public Shared Function Open(width As Integer, height As Integer, fps As Integer, bitrate As Integer, quality As Integer, outputFile As String, codec As String) As EncVideo
        Dim handle As SafeEncHandle = Toupnam_OpenVideo(width, height, fps, bitrate, quality, Encoding.UTF8.GetBytes(outputFile & ChrW(0)), codec)
        If handle Is Nothing OrElse handle.IsInvalid OrElse handle.IsClosed Then
            Return Nothing
        End If
        Return New EncVideo(handle)
    End Function

    ' unTimeStamp
    '    avi: ignored, timestamp is always set to fps
    '    mp4: ignored when timestamp is set to fps or tick
    '
    Public Function Write(inputData As IntPtr, unTimeStamp As UInteger) As Integer
        Return Toupnam_WriteVideo(_handle, inputData, unTimeStamp)
    End Function

    Public Function Write(inputData As Byte(), unTimeStamp As UInteger) As Integer
        Return Toupnam_WriteVideo(_handle, inputData, unTimeStamp)
    End Function
    
    ' Return value:
    '    <0 indicates an HRESULT error code;
    '    =0 means no data is returned;
    '    >0 indicates the length of the returned data.
    ' outputData: please ensure that the output buffer is large enough to accommodate the compressed video data.
    '
    Public Function Encode(inputData As IntPtr, outputData As IntPtr) As Integer
        Return Toupnam_EncodeVideo(_handle, inputData, outputData)
    End Function

    Public Function Encode(inputData As Byte(), outputData As Byte()) As Integer
        Return Toupnam_EncodeVideo(_handle, inputData, outputData)
    End Function

    Protected Overrides Sub Finalize()
        Try
            Dispose(False)
        Finally
            MyBase.Finalize()
        End Try
    End Sub

#If Not (NETFX_CORE OrElse WINDOWS_UWP) Then
    <SecurityPermission(SecurityAction.Demand, UnmanagedCode:=True)>
    Protected Overridable Sub Dispose(disposing As Boolean)
#Else
    Protected Overridable Sub Dispose(disposing As Boolean)
#End If
        ' Note there are three interesting states here:
        ' 1) CreateFile failed, _handle contains an invalid handle
        ' 2) We called Dispose already, _handle is closed.
        ' 3) _handle is null, due to an async exception before
        '    calling CreateFile. Note that the finalizer runs
        '    if the constructor fails.
        If _handle IsNot Nothing AndAlso Not _handle.IsInvalid Then
            ' Free the handle
            _handle.Dispose()
        End If
        ' SafeHandle records the fact that we've called Dispose.
    End Sub
    '
    '   the object of EncVideo must be obtained by static mothod Open, it cannot be obtained by obj = New EncVideo (The constructor is private on purpose)
    '
    Private Sub New(h As SafeEncHandle)
        _handle = h
    End Sub

    Public Sub Dispose() Implements IDisposable.Dispose
        ' Follow the Dispose pattern - Public nonvirtual
        Dispose(True)
        GC.SuppressFinalize(Me)
    End Sub

    Public Sub Close()
        Me.Dispose()
    End Sub

    Private _handle As SafeEncHandle

#If Not (WINDOWS_UWP) Then
    Public Class SafeEncHandle
        Inherits SafeHandleZeroOrMinusOneIsInvalid
        <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
        Private Shared Sub Toupnam_CloseVideo(h As IntPtr)
        End Sub

        Public Sub New()
            MyBase.New(True)
        End Sub

        <ReliabilityContract(Consistency.WillNotCorruptState, Cer.MayFail)>
        Protected Overrides Function ReleaseHandle() As Boolean
            ' Here, we must obey all rules for constrained execution regions.
            Toupnam_CloseVideo(handle)
            Return True
        End Function
    End Class
#Else
    Public Class SafeEncHandle
        Inherits SafeHandle
        <DllImport("tounam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
        Private Shared Sub Toupnam_CloseVideo(h As IntPtr)
        End Sub
        
        Public Sub New()
            MyBase.New(IntPtr.Zero, True)
        End Sub
        
        Protected Overrides Function ReleaseHandle() As Boolean
            Toupnam_CloseVideo(handle)
            Return True
        End Function
        
        Public Overrides ReadOnly Property IsInvalid() As Boolean
            Get
                Return MyBase.handle = IntPtr.Zero
            End Get
        End Property
    End Class
#End If
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_OpenVideo(width As Integer, height As Integer, fps As Integer, bitrate As Integer, quality As Integer, outputFile As Byte(), <MarshalAs(UnmanagedType.LPStr)> codec As String) As SafeEncHandle
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_WriteVideo(h As SafeEncHandle, inputData As IntPtr, unTimeStamp As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_WriteVideo(h As SafeEncHandle, inputData As Byte(), unTimeStamp As UInteger) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_EncodeVideo(h As SafeEncHandle, inputData As IntPtr, outputData As IntPtr) As Integer
    End Function
    <DllImport("toupnam.dll", ExactSpelling:=True, CallingConvention:=CallingConvention.StdCall)>
    Private Shared Function Toupnam_EncodeVideo(h As SafeEncHandle, inputData As Byte(), outputData As Byte()) As Integer
    End Function
End Class