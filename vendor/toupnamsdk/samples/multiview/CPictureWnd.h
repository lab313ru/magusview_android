#pragma once
#include <memory>

class CPictureWnd : public CStatic
{
public:
	CPictureWnd();

	void Init(int width, int height);
	UCHAR* GetData();
	void SetCamName(const CString& camName) { m_camName = camName; }
	void Reset();

protected:
	bool	m_bMouseTrack;
	CString	m_camName;
	BITMAPINFOHEADER	m_header;
	std::unique_ptr<UCHAR[]> m_pData;

	afx_msg void OnMouseMove(UINT nFlags, CPoint point);
	afx_msg void OnMouseHover(UINT nFlags, CPoint point);
	afx_msg void OnMouseLeave();
	afx_msg void OnPaint();
	DECLARE_MESSAGE_MAP()
};

