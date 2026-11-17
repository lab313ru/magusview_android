#include "pch.h"
#include "multiview.h"
#include "multiviewDlg.h"

#define TIMER_REFRESH 0
#define TIMER_ENUM    1

CMultiViewDlg::CMultiViewDlg(CWnd* pParent /*=nullptr*/)
	: CDialog(IDD_MULTIVIEW_DIALOG, pParent), m_bMin(true)
	, m_camCount(0)
	, m_selectedIndex(-1)
{
	m_hIcon = theApp.LoadIcon(IDR_MAINFRAME);
}

void CMultiViewDlg::DoEventImage(int index)
{
	if (index < MAX_OPEN_CNT)
	{
		unsigned width = 0, height = 0;
		Toupnam_PullImage(m_camVec[index].hCam, m_camVec[index].picWnd.GetData(), 24, &width, &height);
		m_camVec[index].timestamp = GetTickCount64();
		++m_camVec[index].frameCnt;
		m_camVec[index].picWnd.Invalidate(0);
	}
}

unsigned __stdcall OpenThread(void* pArg)
{
	CMultiViewDlg::CallBackCtxSt* pThis = (CMultiViewDlg::CallBackCtxSt*)pArg;
	CMultiViewDlg* pDlg = (CMultiViewDlg*)(pThis->pCtx);
	pDlg->OpenCamera(pThis->index);
	return 0;
}

void CMultiViewDlg::Enum()
{
	m_camCount = Toupnam_Enum(m_cams, _countof(m_cams));
	for (int i = 0; i < m_camCount; ++i)
	{
		int idx = GetFreeIndexOfCamVec(m_cams[i].model);
		if (idx >= 0)
		{
			memcpy(m_camVec[idx].id, m_cams[i].id, sizeof(m_cams[i].id));
			memcpy(m_camVec[idx].name, m_cams[i].name, sizeof(m_cams[i].name));
			memcpy(m_camVec[idx].model, m_cams[i].model, sizeof(m_cams[i].model));
			m_camVec[idx].ctxStVec.index = idx;
			m_camVec[idx].ctxStVec.pCtx = this;

			m_camVec[idx].hThread = (HANDLE)_beginthreadex(nullptr, 0, OpenThread, &m_camVec[idx].ctxStVec, 0, nullptr);
		}
	}
}

void CMultiViewDlg::OpenCamera(int index)
{
	m_camVec[index].hCam = Toupnam_Open(m_camVec[index].id);
	PostMessage(WM_START_CAM, index);
}

void CMultiViewDlg::StartCamera(int index)
{
	m_camVec[index].CloseThread();

	if (m_camVec[index].hCam)
	{
		Toupnam_get_Size(m_camVec[index].hCam, (int*)&m_camVec[index].header.biWidth, (int*)&m_camVec[index].header.biHeight);

		m_camVec[index].header.biSize = sizeof(BITMAPINFOHEADER);
		m_camVec[index].header.biPlanes = 1;
		m_camVec[index].header.biBitCount = 24;
		m_camVec[index].header.biSizeImage = TDIBWIDTHBYTES(m_camVec[index].header.biWidth * m_camVec[index].header.biBitCount) * m_camVec[index].header.biHeight;

		m_camVec[index].picWnd.Init(m_camVec[index].header.biWidth, m_camVec[index].header.biHeight);
		CString str;
		str.Format(_T("%hs"), m_camVec[index].name);
		m_camVec[index].picWnd.SetCamName(str);

		if (FAILED(Toupnam_StartPullModeWithCallback(m_camVec[index].hCam, &m_camVec[index].ctxStVec)))
		{
			CloseCamera(index);
			SetTimer(TIMER_ENUM, 4000, nullptr);
		}
		else
		{
			m_camVec[index].timestamp = GetTickCount64();
			m_camVec[index].bStart = true;
		}
	}
	else
	{
		m_camVec[index].reset();
		SetTimer(TIMER_ENUM, 4000, nullptr);
	}
}

void CMultiViewDlg::CloseCamera(int index)
{
	if (m_camVec[index].hCam)
	{
		Toupnam_Close(m_camVec[index].hCam);
		m_camVec[index].reset();
	}
}

void CMultiViewDlg::ResizePicWnd(int index)
{
	if (m_bMin)
	{
		m_selectedIndex = index;
		for (int i = 0; i < MAX_OPEN_CNT; ++i)
		{
			if (i != m_selectedIndex)
				m_camVec[i].picWnd.ShowWindow(SW_HIDE);
		}
		MaximumPicWnd(index);
	}
	else
	{
		for (int i = 0; i < MAX_OPEN_CNT; ++i)
			m_camVec[i].picWnd.ShowWindow(SW_SHOWNORMAL);
		m_selectedIndex = -1;
		RelayoutCtrls();
	}
	m_bMin = !m_bMin;
}

void CMultiViewDlg::MaximumPicWnd(int index)
{
	CRect rc;
	GetClientRect(rc);
	m_camVec[index].picWnd.MoveWindow(rc);
}

void CMultiViewDlg::RelayoutCtrls()
{
	CRect rc;
	GetClientRect(rc);

	int space = 10;
	int width = (rc.Width() - 5 * space) / 4;
	int height = (rc.Height() - 3 * space) / 2;
	for (int i = 0; i < MAX_OPEN_CNT; ++i)
	{
		int x = space + i * (width + space);
		int y = space;
		if (i > 3)
		{
			x = space + (i - 4) * (width + space);
			y = 2 * space + height;
		}
		m_camVec[i].picWnd.MoveWindow(x, y, width, height);
	}
}

int CMultiViewDlg::GetFreeIndexOfCamVec(char model[64])
{
	for (int i = 0; i < MAX_OPEN_CNT; ++i)
	{
		if (0 == strcmp(model, m_camVec[i].model))
		{
			if (nullptr == m_camVec[i].hCam && nullptr == m_camVec[i].hThread)
				return i;
			return -1;
		}
	}
	for (int i = 0; i < MAX_OPEN_CNT; ++i)
	{
		if (nullptr == m_camVec[i].hCam && nullptr == m_camVec[i].hThread)
			return i;
	}
	return -1;
}

void CMultiViewDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_PICTURE0, m_camVec[0].picWnd);
	DDX_Control(pDX, IDC_PICTURE1, m_camVec[1].picWnd);
	DDX_Control(pDX, IDC_PICTURE2, m_camVec[2].picWnd);
	DDX_Control(pDX, IDC_PICTURE3, m_camVec[3].picWnd);
	DDX_Control(pDX, IDC_PICTURE4, m_camVec[4].picWnd);
	DDX_Control(pDX, IDC_PICTURE5, m_camVec[5].picWnd);
	DDX_Control(pDX, IDC_PICTURE6, m_camVec[6].picWnd);
	DDX_Control(pDX, IDC_PICTURE7, m_camVec[7].picWnd);
}

BEGIN_MESSAGE_MAP(CMultiViewDlg, CDialog)
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_STN_CLICKED(IDC_PICTURE0, &CMultiViewDlg::OnStnClickedPicture0)
	ON_STN_CLICKED(IDC_PICTURE1, &CMultiViewDlg::OnStnClickedPicture1)
	ON_STN_CLICKED(IDC_PICTURE2, &CMultiViewDlg::OnStnClickedPicture2)
	ON_STN_CLICKED(IDC_PICTURE3, &CMultiViewDlg::OnStnClickedPicture3)
	ON_STN_CLICKED(IDC_PICTURE4, &CMultiViewDlg::OnStnClickedPicture4)
	ON_STN_CLICKED(IDC_PICTURE5, &CMultiViewDlg::OnStnClickedPicture5)
	ON_STN_CLICKED(IDC_PICTURE6, &CMultiViewDlg::OnStnClickedPicture6)
	ON_STN_CLICKED(IDC_PICTURE7, &CMultiViewDlg::OnStnClickedPicture7)
	ON_WM_SIZE()
	ON_WM_TIMER()
	ON_MESSAGE(MSG_CALLBACK, &CMultiViewDlg::OnMsgCallback)
	ON_MESSAGE(WM_START_CAM, &CMultiViewDlg::OnStartCamera)
END_MESSAGE_MAP()

static void __stdcall evtCallback(unsigned nEvent, unsigned nPara, void* pCallbackCtx, ToupnamEventExtra* pExtra)
{
	theApp.GetMainWnd()->PostMessage(MSG_CALLBACK, nEvent, (LPARAM)pCallbackCtx);
}

LRESULT CMultiViewDlg::OnMsgCallback(WPARAM wp, LPARAM lp)
{
	if (TOUPNAM_EVENT_IMAGE == wp)
	{
		CMultiViewDlg::CallBackCtxSt* pCtx = (CMultiViewDlg::CallBackCtxSt*)lp;
		CMultiViewDlg* pDlg = (CMultiViewDlg*)(pCtx->pCtx);
		pDlg->DoEventImage(pCtx->index);
	}
	else if (TOUPNAM_EVENT_ENUM == wp)
	{
		CMultiViewDlg* pDlg = (CMultiViewDlg*)(lp);
		pDlg->Enum();
	}
	return 0;
}

LRESULT CMultiViewDlg::OnStartCamera(WPARAM wp, LPARAM lp)
{
	StartCamera((int)wp);
	return 0;
}

BOOL CMultiViewDlg::OnInitDialog()
{
	CDialog::OnInitDialog();

	SetIcon(m_hIcon, TRUE);
	SetIcon(m_hIcon, FALSE);
	SetWindowText(_T("MioCamView8"));

	Toupnam_Init(evtCallback, this);

	SetTimer(TIMER_REFRESH, 300, nullptr);
	ShowWindow(SW_MAXIMIZE);
	return TRUE;
}

BOOL CMultiViewDlg::DestroyWindow()
{
	KillTimer(TIMER_REFRESH);
	for (int i = 0; i < MAX_OPEN_CNT; ++i)
	{
		m_camVec[i].CloseThread();
		CloseCamera(i);
	}
	Toupnam_Fini();

	return CDialog::DestroyWindow();
}

HCURSOR CMultiViewDlg::OnQueryDragIcon()
{
	return static_cast<HCURSOR>(m_hIcon);
}

void CMultiViewDlg::OnSize(UINT nType, int cx, int cy)
{
	CDialog::OnSize(nType, cx, cy);

	if (m_camVec[0].picWnd.GetSafeHwnd())
	{
		if (m_selectedIndex >= 0)
			MaximumPicWnd(m_selectedIndex);
		else
			RelayoutCtrls();
	}
}

void CMultiViewDlg::OnTimer(UINT_PTR nIDEvent)
{
	switch (nIDEvent)
	{
	case TIMER_REFRESH:
		for (int i = 0; i < MAX_OPEN_CNT; ++i)
		{
			if (m_camVec[i].hCam && m_camVec[i].bStart && GetTickCount64() - m_camVec[i].timestamp > 3000)
			{
				CloseCamera(i);
				SetTimer(TIMER_ENUM, 4000, nullptr);
			}
			m_camVec[i].picWnd.Invalidate(0);
		}
		break;
	case TIMER_ENUM:
		KillTimer(TIMER_ENUM);
		Enum();
		break;
	default:
		break;
	}

	CDialog::OnTimer(nIDEvent);
}

void CMultiViewDlg::OnStnClickedPictureN(int n)
{
	int cnt = 0;
	for (int i = 0; i < MAX_OPEN_CNT; ++i)
	{
		if (m_camVec[i].hCam)
			++cnt;
	}
	if (cnt > n)
		ResizePicWnd(n);
}

void CMultiViewDlg::OnStnClickedPicture0()
{
	OnStnClickedPictureN(0);
}

void CMultiViewDlg::OnStnClickedPicture1()
{
	OnStnClickedPictureN(1);
}

void CMultiViewDlg::OnStnClickedPicture2()
{
	OnStnClickedPictureN(2);
}

void CMultiViewDlg::OnStnClickedPicture3()
{
	OnStnClickedPictureN(3);
}

void CMultiViewDlg::OnStnClickedPicture4()
{
	OnStnClickedPictureN(4);
}

void CMultiViewDlg::OnStnClickedPicture5()
{
	OnStnClickedPictureN(5);
}

void CMultiViewDlg::OnStnClickedPicture6()
{
	OnStnClickedPictureN(6);
}

void CMultiViewDlg::OnStnClickedPicture7()
{
	OnStnClickedPictureN(7);
}
