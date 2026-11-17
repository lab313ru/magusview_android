import time, toupnam

class App:
    def __init__(self):
        self.hcam = None
        self.buf = None
        self.total = 0

# callbacks come from toupnam.dll/so internal threads
    @staticmethod
    def eventCallback(nEvent, nPara, ctx):
        ctx.EventCallback(nEvent, nPara)

    def EventCallback(self, nEvent, nPara):
        if nEvent == toupnam.TOUPNAM_EVENT_IMAGE:
            if self.hcam is not None:
                try:
                    self.hcam.PullImage(self.buf, 24)
                    self.total += 1
                    print('image callback ok, total = {}'.format(self.total))
                except toupnam.HRESULTException:
                    print('pull image failed')

    def run(self):
        toupnam.Toupnam.Init(None, None)
        print('wait 5 seconds to discover the online cameras')
        time.sleep(5)
        a = toupnam.Toupnam.Enum()
        if len(a) > 0:
            print(a[0].model)
            self.hcam = toupnam.Toupnam.Open(a[0].id)
            if self.hcam:
                try:
                    width, height = self.hcam.get_Size()
                    bufsize = toupnam.TDIBWIDTHBYTES(width * 24) * height
                    print('image size: {} x {}, bufsize = {}'.format(width, height, bufsize))
                    self.buf = bytes(bufsize)
                    try:
                        self.hcam.Start(self.eventCallback, self)
                    except toupcam.HRESULTException as ex:
                        print('failed to start camera, hr=0x{:x}'.format(ex.hr & 0xffffffff))
                    input('press ENTER to exit')
                except toupnam.HRESULTException:
                    print('failed to start camera')
                finally:
                    self.hcam.Close()
                    self.hcam = None
            else:
                print('failed to open camera')
        else:
            print('no camera found')

if __name__ == '__main__':
    app = App()
    app.run()