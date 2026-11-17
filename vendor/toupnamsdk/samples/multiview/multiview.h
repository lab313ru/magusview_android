#pragma once

#ifndef __AFXWIN_H__
	#error "include 'pch.h' before including this file for PCH"
#endif

#include "resource.h"		// main symbols

class CMultiViewApp : public CWinApp
{
public:
	virtual BOOL InitInstance();
	DECLARE_MESSAGE_MAP()
};

extern CMultiViewApp theApp;
