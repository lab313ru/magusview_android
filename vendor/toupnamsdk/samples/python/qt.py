import sys, toupnam
from PyQt5.QtCore import pyqtSignal, pyqtSlot, QTimer, QSignalBlocker, Qt
from PyQt5.QtGui import QPixmap, QImage
from PyQt5.QtWidgets import QLabel, QApplication, QWidget, QCheckBox, QMessageBox, QPushButton, QComboBox, QSlider, QGroupBox, QGridLayout, QBoxLayout, QHBoxLayout, QVBoxLayout, QMenu, QAction

class MainWidget(QWidget):
    evtCallback = pyqtSignal(int, int)

    @staticmethod
    def makeLayout(lbl1, sli1, val1, lbl2, sli2, val2, lbl3, sli3, val3):
        vlyt = QVBoxLayout()
        hlyt1 = QHBoxLayout()
        hlyt1.addWidget(lbl1)
        hlyt1.addStretch()
        hlyt1.addWidget(val1)
        vlyt.addLayout(hlyt1)
        vlyt.addWidget(sli1)
        hlyt2 = QHBoxLayout()
        hlyt2.addWidget(lbl2)
        hlyt2.addStretch()
        hlyt2.addWidget(val2)
        vlyt.addLayout(hlyt2)
        vlyt.addWidget(sli2)
        if lbl3 is not None:
            hlyt3 = QHBoxLayout()
            hlyt3.addWidget(lbl3)
            hlyt3.addStretch()
            hlyt3.addWidget(val3)
            vlyt.addLayout(hlyt3)
            vlyt.addWidget(sli3)
        return vlyt

    def __init__(self):
        super().__init__()
        self.setMinimumSize(1024, 768)
        self.hcam = None
        self.cur = None
        self.timer = QTimer(self)
        self.pData = None   # video buffer
        self.imgWidth = 0   # video width
        self.imgHeight = 0  # video height
        self.frame = 0
        self.count = 0

        gboxexp = QGroupBox("Exposure")
        self.cbox_auto = QCheckBox("Auto exposure")
        self.cbox_auto.setEnabled(False)
        self.lbl_expoTime = QLabel("0")
        self.lbl_expoGain = QLabel("0")
        self.slider_expoTime = QSlider(Qt.Horizontal)
        self.slider_expoGain = QSlider(Qt.Horizontal)
        self.slider_expoTime.setEnabled(False)
        self.slider_expoGain.setEnabled(False)
        self.cbox_auto.stateChanged.connect(self.onAutoExpo)
        self.slider_expoTime.valueChanged.connect(self.onExpoTime)
        self.slider_expoGain.valueChanged.connect(self.onExpoGain)
        vlytexp = QVBoxLayout()
        vlytexp.addWidget(self.cbox_auto)
        vlytexp.addLayout(self.makeLayout(QLabel("Time"), self.slider_expoTime, self.lbl_expoTime, QLabel("Gain"), self.slider_expoGain, self.lbl_expoGain, None, None, None))
        gboxexp.setLayout(vlytexp)

        gboxwb = QGroupBox("White balance")
        self.cbox_autoWB = QCheckBox("White balance")
        self.cbox_autoWB.setEnabled(False)
        self.cbox_autoWB.stateChanged.connect(self.onAutoWB)
        self.lbl_r = QLabel()
        self.lbl_g = QLabel()
        self.lbl_b = QLabel()
        self.slider_r = QSlider(Qt.Horizontal)
        self.slider_g = QSlider(Qt.Horizontal)
        self.slider_b = QSlider(Qt.Horizontal)
        self.slider_r.setEnabled(False)
        self.slider_g.setEnabled(False)
        self.slider_b.setEnabled(False)
        self.slider_r.valueChanged.connect(self.onWBR)
        self.slider_g.valueChanged.connect(self.onWBG)
        self.slider_b.valueChanged.connect(self.onWBB)
        vlytwb = QVBoxLayout()
        vlytwb.addLayout(self.makeLayout(QLabel("Red:"), self.slider_r, self.lbl_r, QLabel("Green:"), self.slider_g, self.lbl_g, QLabel("Blue:"), self.slider_b, self.lbl_b))
        vlytwb.addWidget(self.cbox_autoWB)
        gboxwb.setLayout(vlytwb)

        self.btn_open = QPushButton("Open")
        self.btn_open.clicked.connect(self.onBtnOpen)
        self.btn_snap = QPushButton("Snap")
        self.btn_snap.setEnabled(False)
        self.btn_snap.clicked.connect(self.onBtnSnap)

        vlytctrl = QVBoxLayout()
        vlytctrl.addWidget(gboxexp)
        vlytctrl.addWidget(gboxwb)
        vlytctrl.addWidget(self.btn_open)
        vlytctrl.addWidget(self.btn_snap)
        vlytctrl.addStretch()
        wgctrl = QWidget()
        wgctrl.setLayout(vlytctrl)

        self.lbl_frame = QLabel()
        self.lbl_video = QLabel()
        vlytshow = QVBoxLayout()
        vlytshow.addWidget(self.lbl_video, 1)
        vlytshow.addWidget(self.lbl_frame)
        wgshow = QWidget()
        wgshow.setLayout(vlytshow)

        gmain = QGridLayout()
        gmain.setColumnStretch(0, 1)
        gmain.setColumnStretch(1, 4)
        gmain.addWidget(wgctrl)
        gmain.addWidget(wgshow)
        self.setLayout(gmain)

        self.evtCallback.connect(self.onevtCallback)
        self.timer.timeout.connect(self.onTimer)

        toupnam.Toupnam.Init(None, None)

    @staticmethod
    def eventCallback(nEvent, nPara, self):
        '''callbacks come from toupnam.dll/so internal threads, so we use qt signal to post this event to the UI thread'''
        self.evtCallback.emit(nEvent, nPara)

    def onevtCallback(self, nEvent, nPara):
        '''this run in the UI thread'''
        if self.hcam is not None:
            if toupnam.TOUPNAM_EVENT_IMAGE == nEvent:
                self.onEventImage()
            elif toupnam.TOUPNAM_EVENT_ERROR == nEvent:
                self.onEventError()
            elif toupnam.TOUPNAM_EVENT_PARA == nEvent:
                self.onEventPara(nPara)

    def onTimer(self):
        if self.hcam:
            self.lbl_frame.setText(str(self.frame))

    def onAutoExpo(self, state):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_AEXPO, 1 if state else 0)

    def onExpoTime(self, value):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_EXPOTIME, value)

    def onExpoGain(self, value):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_AGAIN, value)

    def onAutoWB(self, state):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_AWB, 1 if state else 0)

    def onWBR(self, value):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_WBREDGAIN, value)

    def onWBG(self, value):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_WBGREENGAIN, value)

    def onWBB(self, value):
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_WBBLUEGAIN, value)

    def openCamera(self):
        self.hcam = toupnam.Toupnam.Open(self.cur.id)
        if self.hcam:
            self.hcam.put_Para(toupnam.TOUPNAM_PARA_FORMAT_LOCAL, 2) #QImage use RGB byte order
            self.imgWidth, self.imgHeight = self.hcam.get_Size()
            self.pData = bytes(toupnam.TDIBWIDTHBYTES(self.imgWidth * 24) * self.imgHeight)
            self.frame = 0
            self.slider_expoTime.setRange(self.cur.range[toupnam.TOUPNAM_PARA_EXPOTIME].imin, self.cur.range[toupnam.TOUPNAM_PARA_EXPOTIME].imax)
            self.slider_expoGain.setRange(self.cur.range[toupnam.TOUPNAM_PARA_AGAIN].imin, self.cur.range[toupnam.TOUPNAM_PARA_AGAIN].imax)
            self.slider_r.setRange(self.cur.range[toupnam.TOUPNAM_PARA_WBREDGAIN].imin, self.cur.range[toupnam.TOUPNAM_PARA_WBREDGAIN].imax)
            self.slider_g.setRange(self.cur.range[toupnam.TOUPNAM_PARA_WBGREENGAIN].imin, self.cur.range[toupnam.TOUPNAM_PARA_WBGREENGAIN].imax)
            self.slider_b.setRange(self.cur.range[toupnam.TOUPNAM_PARA_WBBLUEGAIN].imin, self.cur.range[toupnam.TOUPNAM_PARA_WBBLUEGAIN].imax)

            try:
                self.hcam.Start(self.eventCallback, self)
            except toupnam.HRESULTException:
                self.closeCamera()
                QMessageBox.warning(self, "Warning", "Failed to start camera.")
            else:
                self.cbox_auto.setEnabled(True)
                self.cbox_autoWB.setEnabled(True)
                self.btn_open.setText("Close")
                self.btn_snap.setEnabled(True)
                self.onEventPara(toupnam.TOUPNAM_PARA_AEXPO)
                self.onEventPara(toupnam.TOUPNAM_PARA_EXPOTIME)
                self.onEventPara(toupnam.TOUPNAM_PARA_AGAIN)
                self.onEventPara(toupnam.TOUPNAM_PARA_AWB)
                self.onEventPara(toupnam.TOUPNAM_PARA_WBREDGAIN)
                self.onEventPara(toupnam.TOUPNAM_PARA_WBGREENGAIN)
                self.onEventPara(toupnam.TOUPNAM_PARA_WBBLUEGAIN)
                self.timer.start(1000)

    def onBtnOpen(self):
        if self.hcam:
            self.closeCamera()
        else:
            arr = toupnam.Toupnam.Enum()
            if 0 == len(arr):
                QMessageBox.warning(self, "Warning", "No camera found.")
            elif 1 == len(arr):
                self.cur = arr[0]
                self.openCamera()
            else:
                menu = QMenu()
                for i in range(0, len(arr)):
                    action = QAction(arr[i].name, self)
                    action.setData(i)
                    menu.addAction(action)
                action = menu.exec(self.mapToGlobal(self.btn_open.pos()))
                if action:
                    self.cur = arr[action.data()]
                    self.openCamera()

    @staticmethod
    def snapCallback(result, bData, nWidth, nHeight, self):
        if bData:
            self.count += 1
            with open("pyqt{}.jpg".format(self.count), "wb") as file:
                file.write(bData)

    def onBtnSnap(self):
        if self.hcam:
            if (self.cur.flag & toupnam.TOUPNAM_FLAG_CAPTURE) == 0:
                if self.pData:
                    image = QImage(self.pData, self.imgWidth, self.imgHeight, QImage.Format_RGB888)
                    self.count += 1
                    image.save("pyqt{}.bmp".format(self.count))
            else:
                self.hcam.Capture(None, self.snapCallback, self)

    def closeCamera(self):
        if self.hcam:
            self.hcam.Close()
        self.hcam = None
        self.pData = None

        self.btn_open.setText("Open")
        self.timer.stop()
        self.lbl_frame.clear()
        self.cbox_auto.setEnabled(False)
        self.slider_expoGain.setEnabled(False)
        self.slider_expoTime.setEnabled(False)
        self.cbox_autoWB.setEnabled(False)
        self.slider_r.setEnabled(False)
        self.slider_g.setEnabled(False)
        self.slider_b.setEnabled(False)
        self.btn_snap.setEnabled(False)

    def onEventImage(self):
        try:
            self.hcam.PullImage(self.pData, 24)
        except toupnam.HRESULTException:
            pass
        else:
            self.frame += 1
            image = QImage(self.pData, self.imgWidth, self.imgHeight, QImage.Format_RGB888)
            newimage = image.scaled(self.lbl_video.width(), self.lbl_video.height(), Qt.KeepAspectRatio, Qt.FastTransformation)
            self.lbl_video.setPixmap(QPixmap.fromImage(newimage))

    def onEventPara(self, nPara):
        if nPara == toupnam.TOUPNAM_PARA_EXPOTIME:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.slider_expoTime):
                self.slider_expoTime.setValue(val)
            self.lbl_expoTime.setText(str(val))
        elif nPara == toupnam.TOUPNAM_PARA_AGAIN:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.slider_expoGain):
                self.slider_expoGain.setValue(val)
            self.lbl_expoGain.setText(str(val))
        elif nPara == toupnam.TOUPNAM_PARA_WBREDGAIN:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.slider_r):
                self.slider_r.setValue(val)
            self.lbl_r.setText(str(val))
        elif nPara == toupnam.TOUPNAM_PARA_WBGREENGAIN:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.slider_g):
                self.slider_g.setValue(val)
            self.lbl_g.setText(str(val))
        elif nPara == toupnam.TOUPNAM_PARA_WBBLUEGAIN:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.slider_b):
                self.slider_b.setValue(val)
            self.lbl_b.setText(str(val))
        elif nPara == toupnam.TOUPNAM_PARA_AWB:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.cbox_autoWB):
                self.cbox_autoWB.setChecked(val == 1)
            self.slider_r.setEnabled(val == 0)
            self.slider_g.setEnabled(val == 0)
            self.slider_b.setEnabled(val == 0)
        elif nPara == toupnam.TOUPNAM_PARA_AEXPO:
            val = self.hcam.get_Para(nPara)
            with QSignalBlocker(self.cbox_auto):
                self.cbox_auto.setChecked(val == 1)
            self.slider_expoTime.setEnabled(val == 0)
            self.slider_expoGain.setEnabled(val == 0)

    def onEventError(self):
        self.closeCamera()
        QMessageBox.warning(self, "Warning", "Generic Error.")

    def closeEvent(self, event):
        self.closeCamera()
        toupnam.Toupnam.Fini()

if __name__ == '__main__':
    app = QApplication(sys.argv)
    mw = MainWidget()
    mw.show()
    sys.exit(app.exec_())