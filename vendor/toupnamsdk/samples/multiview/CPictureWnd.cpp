#include "pch.h"
#include "CPictureWnd.h"

CPictureWnd::CPictureWnd()
	: m_bMouseTrack(true)
{
}

void CPictureWnd::Init(int width, int height)
{
	memset(&m_header, 0, sizeof(m_header));
	m_header.biSize = sizeof(BITMAPINFOHEADER);
	m_header.biPlanes = 1;
	m_header.biBitCount = 24;
	m_header.biWidth = width;
	m_header.biHeight = height;
	m_header.biSizeImage = TDIBWIDTHBYTES(m_header.biWidth * m_header.biBitCount) * m_header.biHeight;

	m_pData.reset(new UCHAR[m_header.biSizeImage]);
}

UCHAR* CPictureWnd::GetData()
{
	return m_pData.get();
}

void CPictureWnd::Reset()
{
	m_pData.reset();
	m_camName = "";
}

BEGIN_MESSAGE_MAP(CPictureWnd, CStatic)
	ON_WM_MOUSEHOVER()
	ON_WM_LBUTTONDOWN()
	ON_WM_MOUSELEAVE()
	ON_WM_MOUSEMOVE()
	ON_WM_PAINT()
END_MESSAGE_MAP()

void CPictureWnd::OnMouseHover(UINT nFlags, CPoint point)
{
	m_bMouseTrack = false;

	CStatic::OnMouseHover(nFlags, point);
}

void CPictureWnd::OnMouseLeave()
{
	m_bMouseTrack = true;

	CStatic::OnMouseLeave();
}

void CPictureWnd::OnMouseMove(UINT nFlags, CPoint point)
{
	if (m_bMouseTrack)
	{
		TRACKMOUSEEVENT csTME = { 0 };
		csTME.cbSize = sizeof(csTME);
		csTME.dwFlags = TME_LEAVE | TME_HOVER;
		csTME.hwndTrack = m_hWnd;
		csTME.dwHoverTime = 10;
		TrackMouseEvent(&csTME);
		m_bMouseTrack = false;
	}

	CStatic::OnMouseMove(nFlags, point);
}

void CPictureWnd::OnPaint()
{
	if (m_pData)
	{
		CPaintDC dc(this);
		CRect rc;
		GetClientRect(rc);

		CDC MemDCIndicator;
		CBitmap MemBitmapIndicator;
		MemDCIndicator.CreateCompatibleDC(&dc);
		MemBitmapIndicator.CreateCompatibleBitmap(&dc, rc.Width(), rc.Height());
		MemDCIndicator.SelectObject(&MemBitmapIndicator);
		MemDCIndicator.FillSolidRect(rc, GetSysColor(COLOR_WINDOW));

		double xRatio = static_cast<double>(m_header.biWidth) / rc.Width();
		double yRatio = static_cast<double>(m_header.biHeight) / rc.Height();
		double ratio = xRatio > yRatio ? xRatio : yRatio;
		int width = static_cast<int>(m_header.biWidth / ratio);
		int height = static_cast<int>(m_header.biHeight / ratio);

		CPoint centerPt = rc.CenterPoint();

		int m = MemDCIndicator.SetStretchBltMode(COLORONCOLOR);
		StretchDIBits(MemDCIndicator, centerPt.x - width / 2, centerPt.y - height / 2, width, height, 0, 0,
			m_header.biWidth, m_header.biHeight, m_pData.get(), (BITMAPINFO*)&m_header, DIB_RGB_COLORS, SRCCOPY);
		MemDCIndicator.SetStretchBltMode(m);

		MemDCIndicator.SetBkMode(TRANSPARENT);
		MemDCIndicator.SetTextColor(RGB(255, 255, 0));
		MemDCIndicator.DrawText(m_camName, CRect(centerPt.x - width / 2, centerPt.y - height / 2, width, height), DT_TOP | DT_LEFT);

		dc.BitBlt(rc.left, rc.top, rc.Width(), rc.Height(), &MemDCIndicator, 0, 0, SRCCOPY);

		MemDCIndicator.DeleteDC();
		MemBitmapIndicator.DeleteObject();
	}
	else
	{
		CStatic::OnPaint();
	}
}
