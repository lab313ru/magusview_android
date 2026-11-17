#include <QApplication>
#include "demoqt.h"

MainWidget::MainWidget(QWidget* parent)
    : QWidget(parent)
    , m_hcam(nullptr)
    , m_timer(new QTimer(this))
    , m_imgWidth(0), m_imgHeight(0), m_pData(nullptr)
    , m_frame(0), m_count(0)
{
    setMinimumSize(1024, 768);

    QGridLayout* gmain = new QGridLayout();

    QGroupBox* gboxexp = new QGroupBox("Exposure");
    {
        m_cbox_auto = new QCheckBox("Auto exposure");
        m_cbox_auto->setEnabled(false);
        m_lbl_expoTime = new QLabel("0");
        m_lbl_expoGain = new QLabel("0");
        m_slider_expoTime = new QSlider(Qt::Horizontal);
        m_slider_expoGain = new QSlider(Qt::Horizontal);
        m_slider_expoTime->setEnabled(false);
        m_slider_expoGain->setEnabled(false);
        connect(m_cbox_auto, &QCheckBox::stateChanged, this, [this](bool state)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_AEXPO, state ? 1 : 0);
        });
        connect(m_slider_expoTime, &QSlider::valueChanged, this, [this](int value)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_EXPOTIME, value);
        });
        connect(m_slider_expoGain, &QSlider::valueChanged, this, [this](int value)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_AGAIN, value);
        });

        QVBoxLayout* v = new QVBoxLayout();
        v->addWidget(m_cbox_auto);
        v->addLayout(makeLayout(new QLabel("Time"), m_slider_expoTime, m_lbl_expoTime, new QLabel("Gain"), m_slider_expoGain, m_lbl_expoGain, nullptr, nullptr, nullptr));
        gboxexp->setLayout(v);
    }

    QGroupBox* gboxwb = new QGroupBox("White balance");
    {
        m_cbox_autoWB = new QCheckBox("White balance");
        m_cbox_autoWB->setEnabled(false);
        connect(m_cbox_autoWB, &QCheckBox::stateChanged, this, [this](bool state)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_AWB, state ? 1 : 0);
        });
        m_lbl_r = new QLabel();
        m_lbl_g = new QLabel();
        m_lbl_b = new QLabel();
        m_slider_r = new QSlider(Qt::Horizontal);
        m_slider_g = new QSlider(Qt::Horizontal);
        m_slider_b = new QSlider(Qt::Horizontal);
        m_slider_r->setEnabled(false);
        m_slider_g->setEnabled(false);
        m_slider_b->setEnabled(false);
        connect(m_slider_r, &QSlider::valueChanged, this, [this](int value)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_WBREDGAIN, value);
        });
        connect(m_slider_g, &QSlider::valueChanged, this, [this](int value)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_WBGREENGAIN, value);
        });
        connect(m_slider_b, &QSlider::valueChanged, this, [this](int value)
        {
            Toupnam_put_Para(m_hcam, TOUPNAM_PARA_WBBLUEGAIN, value);
        });

        QVBoxLayout* v = new QVBoxLayout();
        v->addLayout(makeLayout(new QLabel("Red:"), m_slider_r, m_lbl_r, new QLabel("Green:"), m_slider_g, m_lbl_g, new QLabel("Blue:"), m_slider_b, m_lbl_b));
        v->addWidget(m_cbox_autoWB);
        gboxwb->setLayout(v);
    }

    {
        m_btn_open = new QPushButton("Open");
        connect(m_btn_open, &QPushButton::clicked, this, &MainWidget::onBtnOpen);
        m_btn_snap = new QPushButton("Snap");
        m_btn_snap->setEnabled(false);
        connect(m_btn_snap, &QPushButton::clicked, this, &MainWidget::onBtnSnap);

        QVBoxLayout* v = new QVBoxLayout();
        v->addWidget(gboxexp);
        v->addWidget(gboxwb);
        v->addWidget(m_btn_open);
        v->addWidget(m_btn_snap);
        v->addStretch();
        gmain->addLayout(v, 0, 0);
    }

    {
        m_lbl_frame = new QLabel();
        m_lbl_video = new QLabel();

        QVBoxLayout* v = new QVBoxLayout();
        v->addWidget(m_lbl_video, 1);
        v->addWidget(m_lbl_frame);
        gmain->addLayout(v, 0, 1);
    }

    gmain->setColumnStretch(0, 1);
    gmain->setColumnStretch(1, 4);
    setLayout(gmain);

    connect(this, &MainWidget::evtCallback, this, [this](unsigned nEvent, unsigned nPara)
    {
        /* this run in the UI thread */
        if (m_hcam)
        {
            if (TOUPNAM_EVENT_IMAGE == nEvent)
                onEventImage();
            else if (TOUPNAM_EVENT_ERROR == nEvent)
                onEventError();
            else if (TOUPNAM_EVENT_PARA == nEvent)
                onEventPara(nPara);
        }
    });

    connect(m_timer, &QTimer::timeout, this, [this]()
    {
        if (m_hcam)
            m_lbl_frame->setText(QString::number(m_frame));
    });

    Toupnam_Init(eventCallBack, this);
}

void MainWidget::closeCamera()
{
    if (m_hcam)
    {
        Toupnam_Close(m_hcam);
        m_hcam = nullptr;
    }
    delete[] m_pData;
    m_pData = nullptr;

    m_btn_open->setText("Open");
    m_timer->stop();
    m_lbl_frame->clear();
    m_cbox_auto->setEnabled(false);
    m_slider_expoGain->setEnabled(false);
    m_slider_expoTime->setEnabled(false);
    m_cbox_autoWB->setEnabled(false);
    m_slider_r->setEnabled(false);
    m_slider_g->setEnabled(false);
    m_slider_b->setEnabled(false);
    m_btn_snap->setEnabled(false);
}

void MainWidget::closeEvent(QCloseEvent*)
{
    closeCamera();
    Toupnam_Fini();
}

void MainWidget::startCamera()
{
    m_hcam = Toupnam_Open(m_cur.id);
    if (nullptr == m_hcam)
        return;
    Toupnam_put_Para(m_hcam, TOUPNAM_PARA_FORMAT_LOCAL, 2); //QImage use RGB byte order

    if ((Toupnam_get_Size(m_hcam, &m_imgWidth, &m_imgHeight) < 0) || (m_imgWidth <= 0) || (m_imgHeight <= 0))
    {
        onEventError();
        return;
    }
    if (m_pData)
    {
        delete[] m_pData;
        m_pData = nullptr;
    }
    m_pData = new uchar[TDIBWIDTHBYTES(m_imgWidth * 24) * m_imgHeight];

    m_frame = 0;
    m_slider_expoTime->setRange(m_cur.range[TOUPNAM_PARA_EXPOTIME].imin, m_cur.range[TOUPNAM_PARA_EXPOTIME].imax);
    m_slider_expoGain->setRange(m_cur.range[TOUPNAM_PARA_AGAIN].imin, m_cur.range[TOUPNAM_PARA_AGAIN].imax);
    m_slider_r->setRange(m_cur.range[TOUPNAM_PARA_WBREDGAIN].imin, m_cur.range[TOUPNAM_PARA_WBREDGAIN].imax);
    m_slider_g->setRange(m_cur.range[TOUPNAM_PARA_WBGREENGAIN].imin, m_cur.range[TOUPNAM_PARA_WBGREENGAIN].imax);
    m_slider_b->setRange(m_cur.range[TOUPNAM_PARA_WBBLUEGAIN].imin, m_cur.range[TOUPNAM_PARA_WBBLUEGAIN].imax);

    if (SUCCEEDED(Toupnam_StartPullModeWithCallback(m_hcam, this)))
    {
        m_cbox_auto->setEnabled(true);
        m_cbox_autoWB->setEnabled(true);
        m_btn_open->setText("Close");
        m_btn_snap->setEnabled(true);
        onEventPara(TOUPNAM_PARA_AEXPO);
        onEventPara(TOUPNAM_PARA_EXPOTIME);
        onEventPara(TOUPNAM_PARA_AGAIN);
        onEventPara(TOUPNAM_PARA_AWB);
        onEventPara(TOUPNAM_PARA_WBREDGAIN);
        onEventPara(TOUPNAM_PARA_WBGREENGAIN);
        onEventPara(TOUPNAM_PARA_WBBLUEGAIN);
        
        m_timer->start(1000);
    }
    else
    {
        closeCamera();
        QMessageBox::warning(this, "Warning", "Failed to start camera.");
    }
}

void MainWidget::onBtnOpen()
{
    if (m_hcam)
        closeCamera();
    else
    {
        ToupnamDevice arr[TOUPNAM_MAX] = { 0 };
        unsigned count = Toupnam_Enum(arr, _countof(arr));
        if (0 == count)
            QMessageBox::warning(this, "Warning", "No camera found.");
        else if (1 == count)
        {
            m_cur = arr[0];
            startCamera();
        }
        else
        {
            QMenu menu;
            for (unsigned i = 0; i < count; ++i)
            {
                menu.addAction(arr[i].name, this, [this, i, arr](bool)
                {
                    m_cur = arr[i];
                    startCamera();
                });
            }
            menu.exec(mapToGlobal(m_btn_snap->pos()));
        }
    }
}

void MainWidget::snapCallback(int, const void* pData, size_t nLength, const BITMAPINFOHEADER*, void* pCallbackCtx)
{
    MainWidget* pThis = (MainWidget*)pCallbackCtx;
    if (pData && nLength)
    {
        FILE* fp = fopen(QString::asprintf("demoqt_%u.jpg", ++(pThis->m_count)).toStdString().c_str(), "wb");
        if (fp)
        {
            fwrite(pData, 1, nLength, fp);
            fclose(fp);
        }
    }
}

void MainWidget::onBtnSnap()
{
    if (m_hcam)
    {
        if ((m_cur.flag & TOUPNAM_FLAG_CAPTURE) == 0)
        {
            if (m_pData)
            {
                QImage image(m_pData, m_imgWidth, m_imgHeight, QImage::Format_RGB888);
                image.save(QString::asprintf("demoqt_%u.bmp", ++m_count));
            }
        }
        else
        {
            Toupnam_Capture(m_hcam, nullptr, snapCallback, this);
        }
    }
}

void MainWidget::eventCallBack(unsigned nEvent, unsigned nPara, void* pCallbackCtx, ToupnamEventExtra* /*pExtra*/)
{
    MainWidget* pThis = reinterpret_cast<MainWidget*>(pCallbackCtx);
    emit pThis->evtCallback(nEvent, nPara);
}

void MainWidget::onEventImage()
{
    unsigned width = 0, height = 0;
    if (Toupnam_PullImage(m_hcam, m_pData, 24, &width, &height) >= 0)
    {
        ++m_frame;
        QImage image(m_pData, width, height, QImage::Format_RGB888);
        QImage newimage = image.scaled(m_lbl_video->width(), m_lbl_video->height(), Qt::KeepAspectRatio, Qt::FastTransformation);
        m_lbl_video->setPixmap(QPixmap::fromImage(newimage));
    }
}

template<typename Lambda>
void MainWidget::onEventPara(unsigned nPara, Lambda L)
{
    int val = 0;
    if (Toupnam_get_Para(m_hcam, nPara, &val) >= 0)
        L(val);
}

void MainWidget::onEventPara(unsigned nPara)
{
    switch (nPara)
    {
    case TOUPNAM_PARA_EXPOTIME:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_slider_expoTime);
                m_slider_expoTime->setValue(val);
            }
            m_lbl_expoTime->setText(QString::number(val));
        });
        break;
    case TOUPNAM_PARA_AGAIN:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_slider_expoGain);
                m_slider_expoGain->setValue(val);
            }
            m_lbl_expoGain->setText(QString::number(val));
        });
        break;
    case TOUPNAM_PARA_WBREDGAIN:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_slider_r);
                m_slider_r->setValue(val);
            }
            m_lbl_r->setText(QString::number(val));
        });
        break;
    case TOUPNAM_PARA_WBGREENGAIN:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_slider_g);
                m_slider_g->setValue(val);
            }
            m_lbl_g->setText(QString::number(val));
        });
        break;
    case TOUPNAM_PARA_WBBLUEGAIN:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_slider_b);
                m_slider_b->setValue(val);
            }
            m_lbl_b->setText(QString::number(val));
        });
        break;
    case TOUPNAM_PARA_AWB:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_cbox_autoWB);
                m_cbox_autoWB->setChecked(val == 1);
            }
            m_slider_r->setEnabled(val == 0);
            m_slider_g->setEnabled(val == 0);
            m_slider_b->setEnabled(val == 0);
        });
        break;
    case TOUPNAM_PARA_AEXPO:
        onEventPara(nPara, [this](int val)
        {
            {
                const QSignalBlocker blocker(m_cbox_auto);
                m_cbox_auto->setChecked(val == 1);
            }
            m_slider_expoTime->setEnabled(val == 0);
            m_slider_expoGain->setEnabled(val == 0);
        });
        break;
    default:
        break;
    }
}

void MainWidget::onEventError()
{
    closeCamera();
    QMessageBox::warning(this, "Warning", "Generic error.");
}

QVBoxLayout* MainWidget::makeLayout(QLabel* lbl1, QSlider* sli1, QLabel* val1, QLabel* lbl2, QSlider* sli2, QLabel* val2, QLabel* lbl3, QSlider* sli3, QLabel* val3)
{
    QVBoxLayout* vlyt = new QVBoxLayout();
    QHBoxLayout* hlyt1 = new QHBoxLayout();
    hlyt1->addWidget(lbl1);
    hlyt1->addStretch();
    hlyt1->addWidget(val1);
    vlyt->addLayout(hlyt1);
    vlyt->addWidget(sli1);
    QHBoxLayout* hlyt2 = new QHBoxLayout();
    hlyt2->addWidget(lbl2);
    hlyt2->addStretch();
    hlyt2->addWidget(val2);
    vlyt->addLayout(hlyt2);
    vlyt->addWidget(sli2);
    if (lbl3)
    {
        QHBoxLayout* hlyt3 = new QHBoxLayout();
        hlyt3->addWidget(lbl3);
        hlyt3->addStretch();
        hlyt3->addWidget(val3);
        vlyt->addLayout(hlyt3);
        vlyt->addWidget(sli3);
    }
    return vlyt;
}

int main(int argc, char* argv[])
{
    QApplication a(argc, argv);
    MainWidget mw;
    mw.show();
    return a.exec();
}
