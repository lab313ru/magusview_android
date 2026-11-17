import time, toupnam

class App:
    def __init__(self):
        self.hcam = None
        self.total = 0

# callbacks come from toupnam.dll/so internal threads
    @staticmethod
    def eventCallback(nEvent, nPara, ctx):
        ctx.EventCallback(nEvent, nPara)

    def EventCallback(self, nEvent, nPara):
        if nEvent == toupnam.TOUPNAM_EVENT_IMAGE:
            if self.hcam is not None:
                try:
                    ptr = self.hcam.PullImagePtr()
                    self.total += 1
                    print('image callback ok, total = {}, {} x {}'.format(self.total, ptr.width, ptr.height))
                    self.hcam.PushImagePtr(ptr.id)
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