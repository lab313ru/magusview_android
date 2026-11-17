#pragma once

#include "CPictureWnd.h"
#include <vector>
#include <memory>

#define MAX_OPEN_CNT 8

class CMultiViewDlg : public CDialog
{
public:
	struct CallBackCtxSt
	{
		void* pCtx;
		int   index;

		CallBackCtxSt() : pCtx(nullptr), index(-1) {}
	};

	CMultiViewDlg(CWnd* pParent = nullptr);

	void Enum();
	void OpenCamera(int index);
	void StartCamera(int index);
	void CloseCamera(int index);
	void DoEventImage(int index);

	enum { IDD = IDD_MULTIVIEW_DIALOG };

protected:
	virtual void DoDataExchange(CDataExchange* pDX);
	virtual BOOL OnInitDialog();
	virtual BOOL DestroyWindow();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnSize(UINT nType, int cx, int cy);
	afx_msg void OnTimer(UINT_PTR nIDEvent);
	afx_msg void OnStnClickedPicture0();
	afx_msg void OnStnClickedPicture1();
	afx_msg void OnStnClickedPicture2();
	afx_msg void OnStnClickedPicture3();
	afx_msg void OnStnClickedPicture4();
	afx_msg void OnStnClickedPicture5();
	afx_msg void OnStnClickedPicture6();
	afx_msg void OnStnClickedPicture7();
	afx_msg LRESULT OnMsgCallback(WPARAM wp, LPARAM lp);
	afx_msg LRESULT OnStartCamera(WPARAM wp, LPARAM lp);
	DECLARE_MESSAGE_MAP()

private:
	struct CamSt
	{
		HANDLE                  hThread;
		HToupnam                hCam;
		bool					bStart;
		char                    id[64];
		char                    name[64];
		char                    model[64];
		BITMAPINFOHEADER        header;
		CPictureWnd             picWnd;
		CallBackCtxSt           ctxStVec;
		ULONGLONG               frameCnt;
		ULONGLONG               timestamp;

		CamSt() : hThread(nullptr), hCam(nullptr), bStart(false), frameCnt(0), timestamp(0)
		{
			memset(id, 0, sizeof(id));
			memset(name, 0, sizeof(name));
			memset(model, 0, sizeof(model));
			memset(&header, 0, sizeof(header));
		}

		void CloseThread()
		{
			if (hThread)
			{
				WaitForSingleObject(hThread, INFINITE);
				CloseHandle(hThread);
				hThread = nullptr;
			}
		}

		void reset()
		{
			hCam = nullptr;
			bStart = false;
			frameCnt = timestamp = 0;
			memset(&header, 0, sizeof(header));
			memset(id, 0, sizeof(id));
			memset(name, 0, sizeof(name));
			picWnd.Reset();
		}
	};

	HICON                      m_hIcon;
	bool					   m_bMin;

	ToupnamDevice              m_cams[20];
	int                        m_camCount;

	CamSt                      m_camVec[MAX_OPEN_CNT];
	int                        m_openCount;
	int                        m_selectedIndex;

	void ResizePicWnd(int index);
	void MaximumPicWnd(int index);
	void RelayoutCtrls();
	int  GetFreeIndexOfCamVec(char model[64]);
	void OnStnClickedPictureN(int n);
};
