#include <atlbase.h>
#include <atlwin.h>
#include <atlapp.h>
CAppModule _Module;
#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>
#include <stdio.h>
#include <stdlib.h>
#include <atlctrls.h>
#include <atlframe.h>
#include <atlcrack.h>
#include <atldlgs.h>
#include <atlstr.h>
#include "toupnam.h"
#include "resource.h"
#include <InitGuid.h>
#include <wincodec.h>

#define MSG_CAMEVENT			(WM_APP + 1)
#define MSG_CAMENUM 			(WM_APP + 2)

class CMainFrame;

static BOOL SaveImageBmp(const wchar_t* strFilename, const void* pData, const BITMAPINFOHEADER* pHeader)
{
	FILE* fp = _wfopen(strFilename, L"wb");
	if (fp)
	{
		BITMAPFILEHEADER fheader = { 0 };
		fheader.bfType = 0x4d42;
		fheader.bfSize = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER) + pHeader->biSizeImage;
		fheader.bfOffBits = (DWORD)(sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER));
		fwrite(&fheader, sizeof(fheader), 1, fp);
		fwrite(pHeader, 1, sizeof(BITMAPINFOHEADER), fp);
		fwrite(pData, 1, pHeader->biSizeImage, fp);
		fclose(fp);
		return TRUE;
	}
	return FALSE;
}

static BOOL SaveImageByWIC(const wchar_t* strFilename, const void* pData, const BITMAPINFOHEADER* pHeader)
{
	GUID guidContainerFormat;
	if (PathMatchSpec(strFilename, L"*.jpg"))
		guidContainerFormat = GUID_ContainerFormatJpeg;
	else if (PathMatchSpec(strFilename, L"*.png"))
		guidContainerFormat = GUID_ContainerFormatPng;
	else
		return FALSE;

	CComPtr<IWICImagingFactory> spIWICImagingFactory;
	HRESULT hr = CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER, __uuidof(IWICImagingFactory), (LPVOID*)&spIWICImagingFactory);
	if (FAILED(hr))
		return FALSE;

	CComPtr<IWICBitmapEncoder> spIWICBitmapEncoder;
	hr = spIWICImagingFactory->CreateEncoder(guidContainerFormat, NULL, &spIWICBitmapEncoder);
	if (FAILED(hr))
		return FALSE;

	CComPtr<IWICStream> spIWICStream;
	spIWICImagingFactory->CreateStream(&spIWICStream);
	if (FAILED(hr))
		return FALSE;

	hr = spIWICStream->InitializeFromFilename(strFilename, GENERIC_WRITE);
	if (FAILED(hr))
		return FALSE;

	hr = spIWICBitmapEncoder->Initialize(spIWICStream, WICBitmapEncoderNoCache);
	if (FAILED(hr))
		return FALSE;

	CComPtr<IWICBitmapFrameEncode> spIWICBitmapFrameEncode;
	CComPtr<IPropertyBag2> spIPropertyBag2;
	hr = spIWICBitmapEncoder->CreateNewFrame(&spIWICBitmapFrameEncode, &spIPropertyBag2);
	if (FAILED(hr))
		return FALSE;

	if (GUID_ContainerFormatJpeg == guidContainerFormat)
	{
		PROPBAG2 option = { 0 };
		option.pstrName = L"ImageQuality"; /* jpg quality, you can change this setting */
		CComVariant varValue(0.75f);
		spIPropertyBag2->Write(1, &option, &varValue);
	}
	hr = spIWICBitmapFrameEncode->Initialize(spIPropertyBag2);
	if (FAILED(hr))
		return FALSE;

	hr = spIWICBitmapFrameEncode->SetSize(pHeader->biWidth, pHeader->biHeight);
	if (FAILED(hr))
		return FALSE;

	WICPixelFormatGUID formatGUID = GUID_WICPixelFormat24bppBGR;
	hr = spIWICBitmapFrameEncode->SetPixelFormat(&formatGUID);
	if (FAILED(hr))
		return FALSE;

	const LONG nWidthBytes = TDIBWIDTHBYTES(pHeader->biWidth * pHeader->biBitCount);
	for (LONG i = 0; i < pHeader->biHeight; ++i)
	{
		hr = spIWICBitmapFrameEncode->WritePixels(1, nWidthBytes, nWidthBytes, ((BYTE*)pData) + nWidthBytes * (pHeader->biHeight - i - 1));
		if (FAILED(hr))
			return FALSE;
	}

	hr = spIWICBitmapFrameEncode->Commit();
	if (FAILED(hr))
		return FALSE;
	hr = spIWICBitmapEncoder->Commit();
	if (FAILED(hr))
		return FALSE;
	
	return TRUE;
}

class CExposureTimeDlg : public CDialogImpl<CExposureTimeDlg>
{
	HToupnam	m_hcam;

	BEGIN_MSG_MAP(CExposureTimeDlg)
		MESSAGE_HANDLER(WM_INITDIALOG, OnInitDialog)
		COMMAND_HANDLER(IDOK, BN_CLICKED, OnOK)
		COMMAND_HANDLER(IDCANCEL, BN_CLICKED, OnCancel)
	END_MSG_MAP()
public:
	enum { IDD = IDD_EXPOSURETIME };

	CExposureTimeDlg(HToupnam hcam)
	: m_hcam(hcam)
	{
	}

private:
	LRESULT OnInitDialog(UINT /*uMsg*/, WPARAM /*wParam*/, LPARAM /*lParam*/, BOOL& /*bHandled*/)
	{
		CenterWindow(GetParent());

		const ToupnamDevice tdev = Toupnam_get_Device(m_hcam);
		CTrackBarCtrl ctrl(GetDlgItem(IDC_SLIDER1));
		ctrl.SetRangeMin(tdev.range[TOUPNAM_PARA_EXPOTIME].imin);
		ctrl.SetRangeMax(tdev.range[TOUPNAM_PARA_EXPOTIME].imax);

		int nTime = 0;
		if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_EXPOTIME, &nTime)))
			ctrl.SetPos(nTime);
		
		return TRUE;
	}

	LRESULT OnOK(WORD /*wNotifyCode*/, WORD wID, HWND /*hWndCtl*/, BOOL& /*bHandled*/)
	{
		CTrackBarCtrl ctrl(GetDlgItem(IDC_SLIDER1));
		Toupnam_put_Para(m_hcam, TOUPNAM_PARA_EXPOTIME, ctrl.GetPos());

		EndDialog(wID);
		return 0;
	}

	LRESULT OnCancel(WORD /*wNotifyCode*/, WORD wID, HWND /*hWndCtl*/, BOOL& /*bHandled*/)
	{
		EndDialog(wID);
		return 0;
	}
};

class CIpDlg : public CDialogImpl<CIpDlg>
{
	bool		m_bAdd;

	BEGIN_MSG_MAP(CIpDlg)
		MESSAGE_HANDLER(WM_INITDIALOG, OnInitDialog)
		COMMAND_HANDLER(IDOK, BN_CLICKED, OnOK)
		COMMAND_HANDLER(IDCANCEL, BN_CLICKED, OnCancel)
		COMMAND_HANDLER(IDC_EDIT1, EN_CHANGE, OnEnchangeEdit1)
	END_MSG_MAP()
public:
	enum { IDD = IDD_IP };

	CIpDlg(bool bAdd)
		: m_bAdd(bAdd)
	{
	}
private:
	LRESULT OnInitDialog(UINT /*uMsg*/, WPARAM /*wParam*/, LPARAM /*lParam*/, BOOL& /*bHandled*/)
	{
		CenterWindow(GetParent());
		GetDlgItem(IDOK).EnableWindow(FALSE);
		return TRUE;
	}

	LRESULT OnOK(WORD /*wNotifyCode*/, WORD wID, HWND /*hWndCtl*/, BOOL& /*bHandled*/)
	{
		CString str;
		GetDlgItemText(IDC_EDIT1, str);
		str.Trim();
		if (!str.IsEmpty())
		{
			CW2A w2a((LPCTSTR)str);
			const char* arr[2] = { 0 };
			arr[0] = (LPSTR)w2a;
			if (m_bAdd)
				Toupnam_add_Ip(arr);
			else
				Toupnam_del_Ip(arr);
		}
		EndDialog(wID);
		return 0;
	}

	LRESULT OnEnchangeEdit1(WORD /*wNotifyCode*/, WORD wID, HWND /*hWndCtl*/, BOOL& /*bHandled*/)
	{
		CString str;
		GetDlgItemText(IDC_EDIT1, str);
		str.Trim();
		GetDlgItem(IDOK).EnableWindow(!str.IsEmpty());
		return 0;
	}

	LRESULT OnCancel(WORD /*wNotifyCode*/, WORD wID, HWND /*hWndCtl*/, BOOL& /*bHandled*/)
	{
		EndDialog(wID);
		return 0;
	}
};

class CMainView : public CWindowImpl<CMainView>
{
	CMainFrame*	m_pMainFrame;

	BEGIN_MSG_MAP(CMainView)
		MESSAGE_HANDLER(WM_PAINT, OnWmPaint)
		MESSAGE_HANDLER(WM_ERASEBKGND, OnEraseBkgnd)
	END_MSG_MAP()

	static ATL::CWndClassInfo& GetWndClassInfo()
	{
		static ATL::CWndClassInfo wc =
		{
			{ sizeof(WNDCLASSEX), CS_HREDRAW | CS_VREDRAW, StartWindowProc,
			  0, 0, NULL, NULL, NULL, (HBRUSH)NULL_BRUSH, NULL, NULL, NULL },
			NULL, NULL, IDC_ARROW, TRUE, 0, _T("")
		};
		return wc;
	}
public:
	CMainView(CMainFrame* pMainFrame)
	: m_pMainFrame(pMainFrame)
	{
	}
private:
	LRESULT OnWmPaint(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
	LRESULT OnEraseBkgnd(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled)
	{
		return 1;
	}
};

static void __stdcall OnEventCallback(unsigned nEvent, unsigned nPara, void* pCtx, ToupnamEventExtra* pExtra)
{
	HWND hWnd = (HWND)pCtx;
	if (TOUPNAM_EVENT_ENUM == nEvent)
		PostMessage(hWnd, MSG_CAMENUM, 0, 0);
}

class CMainFrame : public CFrameWindowImpl<CMainFrame>, public CUpdateUI<CMainFrame>
{
	HToupnam	m_hcam;
	CMainView	m_view;
	ToupnamDevice m_tdev, m_ti[TOUPNAM_MAX];
	unsigned	m_nFrameCount;
	DWORD		m_dwStartTick, m_dwLastTick;

	BYTE*				m_pData;
	BITMAPINFOHEADER	m_header;

	BEGIN_MSG_MAP_EX(CMainFrame)
		MSG_WM_CREATE(OnCreate)
		MESSAGE_HANDLER(WM_DESTROY, OnWmDestroy)
		MESSAGE_HANDLER(MSG_CAMEVENT, OnMsgCamEvent)
		MESSAGE_HANDLER(MSG_CAMENUM, OnMsgCamEnum)
		COMMAND_RANGE_HANDLER_EX(ID_DEVICE_DEVICE0, ID_DEVICE_DEVICEF, OnOpenDevice)
		COMMAND_ID_HANDLER_EX(ID_CONFIG_WHITEBALANCE, OnWhiteBalance)
		COMMAND_ID_HANDLER_EX(ID_CONFIG_AUTOEXPOSURE, OnAutoExposure)
		COMMAND_ID_HANDLER_EX(ID_CONFIG_VERTICALFLIP, OnVerticalFlip)
		COMMAND_ID_HANDLER_EX(ID_CONFIG_HORIZONTALFLIP, OnHorizontalFlip)
		COMMAND_ID_HANDLER_EX(ID_ACTION_SNAP, OnSnap)
		COMMAND_ID_HANDLER_EX(ID_CONFIG_EXPOSURETIME, OnExposureTime)
		COMMAND_ID_HANDLER_EX(ID_ACTION_STARTRECORD, OnStartRecord)
		COMMAND_ID_HANDLER_EX(ID_ACTION_STOPRECORD, OnStopRecord)
		COMMAND_ID_HANDLER_EX(ID_ADD_IP, OnAddIp)
		COMMAND_ID_HANDLER_EX(ID_DELETE_IP, OnDeleteIp)
		CHAIN_MSG_MAP(CUpdateUI<CMainFrame>)
		CHAIN_MSG_MAP(CFrameWindowImpl<CMainFrame>)
	END_MSG_MAP()

	DECLARE_FRAME_WND_CLASS(NULL, IDR_MAIN);

	BEGIN_UPDATE_UI_MAP(CMainFrame)
		UPDATE_ELEMENT(ID_CONFIG_WHITEBALANCE, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_CONFIG_AUTOEXPOSURE, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_CONFIG_VERTICALFLIP, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_CONFIG_HORIZONTALFLIP, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_ACTION_STARTRECORD, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_ACTION_STOPRECORD, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_ACTION_SNAP, UPDUI_MENUPOPUP)
		UPDATE_ELEMENT(ID_CONFIG_EXPOSURETIME, UPDUI_MENUPOPUP)
	END_UPDATE_UI_MAP()
public:
	CMainFrame()
	: m_hcam(NULL), m_nFrameCount(0), m_dwStartTick(0), m_dwLastTick(0), m_pData(NULL), m_view(this)
	{
		memset(&m_tdev, 0, sizeof(m_tdev));
		memset(m_ti, 0, sizeof(m_ti));
		memset(&m_header, 0, sizeof(m_header));

		m_header.biSize = sizeof(BITMAPINFOHEADER);
		m_header.biPlanes = 1;
		m_header.biBitCount = 24;
	}

	bool GetData(BITMAPINFOHEADER** pHeader, BYTE** pData)
	{
		if (m_pData)
		{
			*pData = m_pData;
			*pHeader = &m_header;
			return true;
		}

		return false;
	}
private:
	LRESULT OnMsgCamEvent(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled)
	{
		switch (wParam)
		{
		case TOUPNAM_EVENT_ERROR:
			OnEventError();
			break;
		case TOUPNAM_EVENT_IMAGE:
			OnEventImage();
			break;
		case TOUPNAM_EVENT_PARA:
			OnEventPara(lParam);
			break;
		}
		return 0;
	}

	void OnCamEnum()
	{
		CMenuHandle menu = GetMenu();
		CMenuHandle submenu = menu.GetSubMenu(0);
		while (submenu.GetMenuItemCount() > 0)
			submenu.RemoveMenu(submenu.GetMenuItemCount() - 1, MF_BYPOSITION);

		unsigned cnt = Toupnam_Enum(m_ti, _countof(m_ti));
		if (0 == cnt)
			submenu.AppendMenu(MF_GRAYED | MF_STRING, ID_DEVICE_DEVICE0, L"No Device");
		else
		{
			for (unsigned i = 0; i < cnt; ++i)
				submenu.AppendMenu(MF_STRING, ID_DEVICE_DEVICE0 + i, CA2W(m_ti[i].name));
		}
	}

	LRESULT OnMsgCamEnum(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled)
	{
		OnCamEnum();
		return 0;
	}

	int OnCreate(LPCREATESTRUCT /*lpCreateStruct*/)
	{
		//uncomment the next line to disable broadcast discovery
		//Toupnam_PriFlag(0, TOUPNAM_PRIFLAG_BROADCAST);

		Toupnam_Init(OnEventCallback, m_hWnd);
		OnCamEnum();

		CreateSimpleStatusBar();
		{
			int iWidth[] = { 100, 400, 600, -1 };
			CStatusBarCtrl statusbar(m_hWndStatusBar);
			statusbar.SetParts(_countof(iWidth), iWidth);
		}

		m_hWndClient = m_view.Create(m_hWnd, rcDefault, NULL, WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS | WS_CLIPCHILDREN, WS_EX_CLIENTEDGE);
		
		OnDeviceChanged();
		return 0;
	}

	void OnWhiteBalance(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam && (!m_tdev.range[TOUPNAM_PARA_AWB].idisable))
			Toupnam_put_Para(m_hcam, TOUPNAM_PARA_AWB, 1);
	}

	void OnAutoExposure(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam && (!m_tdev.range[TOUPNAM_PARA_AEXPO].idisable))
		{
			int bAutoExposure = 0;
			if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_AEXPO, &bAutoExposure)))
			{
				bAutoExposure = !bAutoExposure;
				Toupnam_put_Para(m_hcam, TOUPNAM_PARA_AEXPO, bAutoExposure);
				UISetCheck(ID_CONFIG_AUTOEXPOSURE, bAutoExposure ? 1 : 0);
				UIEnable(ID_CONFIG_EXPOSURETIME, !bAutoExposure);
			}
		}
	}

	void OnVerticalFlip(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam)
		{
			int b = 0;
			if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_VFLIP, &b)))
			{
				b = !b;
				Toupnam_put_Para(m_hcam, TOUPNAM_PARA_VFLIP, b);
				UISetCheck(ID_CONFIG_VERTICALFLIP, b ? 1 : 0);
			}
		}
	}

	void OnHorizontalFlip(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam)
		{
			int b = 0;
			if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_HFLIP, &b)))
			{
				b = !b;
				Toupnam_put_Para(m_hcam, TOUPNAM_PARA_HFLIP, b);
				UISetCheck(ID_CONFIG_HORIZONTALFLIP, b ? 1 : 0);
			}
		}
	}

	void OnExposureTime(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam)
		{
			CExposureTimeDlg dlg(m_hcam);
			if (IDOK == dlg.DoModal())
				UpdateExposureTimeText();
		}
	}

	void OnAddIp(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		CIpDlg dlg(true);
		dlg.DoModal();
	}

	void OnDeleteIp(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		CIpDlg dlg(false);
		dlg.DoModal();
	}

	static void __stdcall CaptureCallback(int result, const void* pData, size_t nLength, const BITMAPINFOHEADER* pHeader, void* pCallbackCtx)
	{
		TCHAR* str = (TCHAR*)pCallbackCtx;
		if (pData && nLength)
		{
			FILE* fp = _tfopen(str, _T("wb"));
			if (fp)
			{
				fwrite(pData, 1, nLength, fp);
				fclose(fp);
			}
		}
		free(str);
	}

	void OnSnap(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (NULL == m_hcam)
			return;

		CFileDialog dlg(FALSE, L"jpg");
		if (IDOK == dlg.DoModal())
		{
			if (m_tdev.flag & TOUPNAM_FLAG_CAPTURE)
				Toupnam_Capture(m_hcam, NULL, CaptureCallback, _tcsdup(dlg.m_szFileName));
			else
				SaveImageByWIC(dlg.m_szFileName, m_pData, &m_header);
		}
	}

	void OnOpenDevice(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		CloseDevice();

		m_nFrameCount = 0;
		m_dwStartTick = m_dwLastTick = 0;
		m_hcam = Toupnam_Open(m_ti[nID - ID_DEVICE_DEVICE0].id);
		if (m_hcam)
		{
			m_tdev = m_ti[nID - ID_DEVICE_DEVICE0];
			OnDeviceChanged();
			UpdateFrameText();

			if (SUCCEEDED(Toupnam_get_Size(m_hcam, (int*)&m_header.biWidth, (int*)&m_header.biHeight)))
			{
				m_header.biSizeImage = TDIBWIDTHBYTES(m_header.biWidth * m_header.biBitCount) * m_header.biHeight;
				m_pData = (BYTE*)malloc(m_header.biSizeImage);
				if (SUCCEEDED(Toupnam_StartPullModeWithWndMsg(m_hcam, m_hWnd, MSG_CAMEVENT)))
				{
					UIEnable(ID_ACTION_SNAP, TRUE);
					UIEnable(ID_ACTION_STARTRECORD, TRUE);
				}
			}
		}
	}

	void OnStartRecord(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		if (m_hcam)
		{
			unsigned nFourCC = 0;
			if (SUCCEEDED(Toupnam_get_FourCC(m_hcam, &nFourCC)))
			{
				CFileDialog dlg(FALSE, (nFourCC == MAKEFOURCC('H', '2', '6', '4')) ? L"mp4" : L"avi");
				if (IDOK == dlg.DoModal())
				{
					if (SUCCEEDED(Toupnam_Record(m_hcam, CW2A(dlg.m_szFileName))))
					{
						UIEnable(ID_ACTION_STARTRECORD, FALSE);
						UIEnable(ID_ACTION_STOPRECORD, TRUE);
					}
				}
			}
		}
	}

	void OnStopRecord(UINT uNotifyCode, int nID, HWND wndCtl)
	{
		StopRecord();

		UIEnable(ID_ACTION_STARTRECORD, m_hcam ? TRUE : FALSE);
		UIEnable(ID_ACTION_STOPRECORD, FALSE);
	}

	void OnEventImage()
	{
		const HRESULT hr = Toupnam_PullImage(m_hcam, m_pData, 24, NULL, NULL);
		if (FAILED(hr))
			return;

		++m_nFrameCount;
		if (0 == m_dwStartTick)
			m_dwLastTick = m_dwStartTick = GetTickCount();
		else
			m_dwLastTick = GetTickCount();
		m_view.Invalidate();

		UpdateFrameText();
	}

	void OnEventPara(unsigned nPara)
	{
		switch (nPara)
		{
		case TOUPNAM_PARA_EXPOTIME:
			UpdateExposureTimeText();
			break;
		case TOUPNAM_PARA_TEMP:
		case TOUPNAM_PARA_TINT:
			UpdateTempTintText();
			break;
		case TOUPNAM_PARA_AEXPO:
			UpdateAE();
			break;
		default:
			break;
		}
	}

	LRESULT OnWmDestroy(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled)
	{
		CloseDevice();

		CFrameWindowImpl<CMainFrame>::OnDestroy(uMsg, wParam, lParam, bHandled);

		Toupnam_Fini();
		return 0;
	}

	void OnEventError()
	{
		CloseDevice();
		AtlMessageBox(m_hWnd, _T("Generic error."));
	}

	void CloseDevice()
	{
		OnStopRecord(0, 0, NULL);

		if (m_hcam)
		{
			Toupnam_Close(m_hcam);
			m_hcam = NULL;

			if (m_pData)
			{
				free(m_pData);
				m_pData = NULL;
			}
		}
		OnDeviceChanged();
	}

	void OnDeviceChanged()
	{
		CStatusBarCtrl statusbar(m_hWndStatusBar);

		if (NULL == m_hcam)
		{
			statusbar.SetText(0, L"");
			statusbar.SetText(1, L"");
			statusbar.SetText(2, L"");
			statusbar.SetText(3, L"");

			UISetCheck(ID_CONFIG_AUTOEXPOSURE, 0);
			UIEnable(ID_CONFIG_EXPOSURETIME, FALSE);
		}
		else
		{
			UpdateResolutionText();
			UpdateExposureTimeText();
			UpdateTempTintText();
			UpdateAE();
		}

		UIEnable(ID_ACTION_STARTRECORD, FALSE);
		UIEnable(ID_ACTION_STOPRECORD, FALSE);
		UIEnable(ID_ACTION_SNAP, FALSE);
		UIEnable(ID_CONFIG_AUTOEXPOSURE, m_hcam ? TRUE : FALSE);
		UIEnable(ID_CONFIG_HORIZONTALFLIP, m_hcam ? TRUE : FALSE);
		UIEnable(ID_CONFIG_VERTICALFLIP, m_hcam ? TRUE : FALSE);
		UIEnable(ID_CONFIG_WHITEBALANCE, m_hcam ? TRUE : FALSE);
		UISetCheck(ID_CONFIG_HORIZONTALFLIP, 0);
		UISetCheck(ID_CONFIG_VERTICALFLIP, 0);
	}

	void UpdateAE()
	{
		if (m_hcam)
		{
			int bAutoExposure = m_tdev.range[TOUPNAM_PARA_AEXPO].idef;
			if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_AEXPO, &bAutoExposure)))
			{
				UISetCheck(ID_CONFIG_AUTOEXPOSURE, bAutoExposure ? 1 : 0);
				UIEnable(ID_CONFIG_EXPOSURETIME, !bAutoExposure);
			}
		}
	}

	void UpdateTempTintText()
	{
		if (m_hcam)
		{
			int nTemp = m_tdev.range[TOUPNAM_PARA_TEMP].idef, nTint = m_tdev.range[TOUPNAM_PARA_TINT].idef;
			if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_TEMP, &nTemp)) && SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_TINT, &nTint)))
			{
				CStatusBarCtrl statusbar(m_hWndStatusBar);
				wchar_t res[256];
				swprintf(res, L"Temp = %d, Tint = %d", nTemp, nTint);
				statusbar.SetText(2, res);
			}
		}
	}

	void UpdateResolutionText()
	{
		CStatusBarCtrl statusbar(m_hWndStatusBar);
		wchar_t res[128];
		int nWidth = 0, nHeight = 0;
		if (SUCCEEDED(Toupnam_get_Size(m_hcam, &nWidth, &nHeight)))
		{
			swprintf(res, L"%u * %u", nWidth, nHeight);
			statusbar.SetText(0, res);
		}
	}

	void UpdateFrameText()
	{
		CStatusBarCtrl statusbar(m_hWndStatusBar);
		wchar_t str[256];
		if (m_dwLastTick != m_dwStartTick)
			swprintf(str, L"%u, %.2f", m_nFrameCount, m_nFrameCount / ((m_dwLastTick - m_dwStartTick) / 1000.0));
		else
			swprintf(str, L"%u", m_nFrameCount);
		statusbar.SetText(3, str);
	}

	void UpdateExposureTimeText()
	{
		if (m_hcam)
		{
			if ((!m_tdev.range[TOUPNAM_PARA_EXPOTIME].idisable) && (!m_tdev.range[TOUPNAM_PARA_AGAIN].idisable))
			{
				CStatusBarCtrl statusbar(m_hWndStatusBar);
				wchar_t res[128];
				int nTime = 0, nGain = 0;
				if (SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_EXPOTIME, &nTime)) && SUCCEEDED(Toupnam_get_Para(m_hcam, TOUPNAM_PARA_AGAIN, &nGain)))
				{
					swprintf(res, L"ExposureTime = %d, Gain = %d", nTime, nGain);
					statusbar.SetText(1, res);
				}
			}
		}
	}

	void StopRecord()
	{
		if (m_hcam)
			Toupnam_Record(m_hcam, NULL);
	}
};

LRESULT CMainView::OnWmPaint(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled)
{
	CPaintDC dc(m_hWnd);

	RECT rc;
	GetClientRect(&rc);
	BITMAPINFOHEADER* pHeader = NULL;
	BYTE* pData = NULL;
	if (m_pMainFrame->GetData(&pHeader, &pData))
	{
		const int m = dc.SetStretchBltMode(COLORONCOLOR);
		StretchDIBits(dc, rc.left, rc.top, rc.right - rc.left, rc.bottom - rc.top, 0, 0, pHeader->biWidth, pHeader->biHeight, pData, (BITMAPINFO*)pHeader, DIB_RGB_COLORS, SRCCOPY);
		dc.SetStretchBltMode(m);
	}
	else
	{
		dc.FillRect(&rc, (HBRUSH)WHITE_BRUSH);
	}

	return 0;
}

static int Run(int nCmdShow = SW_SHOWDEFAULT)
{
	CMessageLoop theLoop;
	_Module.AddMessageLoop(&theLoop);

	CMainFrame frmMain;

	if (frmMain.CreateEx() == NULL)
	{
		ATLTRACE(_T("Main window creation failed!\n"));
		return 0;
	}

	frmMain.ShowWindow(nCmdShow);

	int nRet = theLoop.Run();

	_Module.RemoveMessageLoop();
	return nRet;
}

int WINAPI _tWinMain(HINSTANCE hInstance, HINSTANCE /*hPrevInstance*/, LPTSTR /*pCmdLine*/, int nCmdShow)
{
	INITCOMMONCONTROLSEX iccx;
	iccx.dwSize = sizeof(iccx);
	iccx.dwICC = ICC_COOL_CLASSES | ICC_BAR_CLASSES;
	InitCommonControlsEx(&iccx);

	OleInitialize(NULL);

	_Module.Init(NULL, hInstance);
	int nRet = Run(nCmdShow);
	_Module.Term();
	return nRet;
}