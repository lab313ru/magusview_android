#ifndef __demoqt_H__
#define __demoqt_H__

#include <QPushButton>
#include <QComboBox>
#include <QLabel>
#include <QTimer>
#include <QCheckBox>
#include <QSlider>
#include <QString>
#include <QGroupBox>
#include <QBoxLayout>
#include <QVBoxLayout>
#include <QMenu>
#include <QMessageBox>
#include <toupnam.h>

class MainWidget: public QWidget
{
    Q_OBJECT
    ToupnamDevice    m_cur;
    HToupnam         m_hcam;
    QCheckBox*       m_cbox_auto;
    QSlider*         m_slider_expoTime;
    QSlider*         m_slider_expoGain;
    QSlider*         m_slider_r;
    QSlider*         m_slider_g;
    QSlider*         m_slider_b;
    QLabel*          m_lbl_expoTime;
    QLabel*          m_lbl_expoGain;
    QLabel*          m_lbl_r;
    QLabel*          m_lbl_g;
    QLabel*          m_lbl_b;
    QLabel*          m_lbl_frame;
    QLabel*          m_lbl_video;
    QCheckBox*       m_cbox_autoWB;
    QPushButton*     m_btn_open;
    QPushButton*     m_btn_snap;
    QTimer*          m_timer;
    int              m_imgWidth;
    int              m_imgHeight;
    uchar*           m_pData;
    unsigned         m_frame;
    unsigned         m_count;
public:
    MainWidget(QWidget* parent = nullptr);
protected:
    void closeEvent(QCloseEvent*) override;
signals:
    void evtCallback(unsigned nEvent, unsigned nPara);
private:
    void onBtnOpen();
    void onBtnSnap();
    void closeCamera();
    void startCamera();
    void onEventImage();
    void onEventError();
    void onEventPara(unsigned nPara);
    template<typename Lambda>
    void onEventPara(unsigned nPara, Lambda L);
    static void __stdcall eventCallBack(unsigned nEvent, unsigned nPara, void* pCallbackCtx, ToupnamEventExtra* pExtra);
    static void __stdcall snapCallback(int, const void* pData, size_t nLength, const BITMAPINFOHEADER*, void* pCallbackCtx);
    static QVBoxLayout* makeLayout(QLabel*, QSlider*, QLabel*, QLabel*, QSlider*, QLabel*, QLabel*, QSlider*, QLabel*);
};

#endif
