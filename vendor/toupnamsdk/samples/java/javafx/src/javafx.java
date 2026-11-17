import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.nio.ByteBuffer;

public class javafx extends Application {
    private static toupnam _cam;
    private static String _name;
    private static byte[] _buf;
    private static Canvas _canvas;
    private static int _total;
    private static Stage _stage;

    @Override
    public void start(Stage stage) {
        _stage = stage;
        _stage.setOnCloseRequest(event -> {
            if (_cam != null) {
                _cam.close();
                _cam = null;
            }
            toupnam.Fini();
        });

        BorderPane b = new BorderPane();
        Scene scene = new Scene(b);
        _stage.setWidth(800);
        _stage.setHeight(600);
        b.prefHeightProperty().bind(scene.heightProperty());
        b.prefWidthProperty().bind(scene.widthProperty());
        createAndShow(b);
        _stage.setScene(scene);
        _stage.show();
    }

    /* this run in the UI thread */
    private void OnEvent(int nEvent, int nPara) {
        if ((_cam != null) && (nEvent == toupnam.EVENT_IMAGE)) {
            /* https://docs.oracle.com/javase/8/javafx/graphics-tutorial/image_ops.htm */
            GraphicsContext gc = _canvas.getGraphicsContext2D();
            PixelWriter pw = gc.getPixelWriter();
            PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();
            toupnam.bitmapinfo info = new toupnam.bitmapinfo(0, 0);
            try {
                _cam.PullImage(_buf, 24, info);
                _total += 1;
            } catch (toupnam.HRESULTException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING, ex.toString());
                alert.showAndWait();
                return;
            }
            pw.setPixels(0, 0, info.width, info.height, pixelFormat, _buf, 0, (info.width * 24 + 31) / 32 * 4);
            _stage.setTitle(_name + ": " + _total);
        }
    }

    private class EventRunnable implements Runnable {
        private int nEvent_;
        private int nPara_;
        public EventRunnable(int nEvent, int nPara) {
            nEvent_ = nEvent;
            nPara_ = nPara;
        }
        @Override
        public void run() {
            OnEvent(nEvent_, nPara_);
        }
    }

    private void createAndShow(BorderPane b) {
        CheckBox checkAutoExposure = new CheckBox("Auto Exposure");
        checkAutoExposure.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                if (_cam != null) {
                    try {
                        _cam.put_Para(toupnam.PARA_EXPOTIME, checkAutoExposure.isSelected() ? 1 : 0);
                    } catch (toupnam.HRESULTException ex) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, ex.toString());
                        alert.showAndWait();
                    }
                }
            }
        });
        b.setTop(checkAutoExposure);

        _canvas = new Canvas(800, 560);
        b.setCenter(_canvas);

        try {
            toupnam.Init(null);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            toupnam.device[] arr = toupnam.Enum();
            if (arr.length <= 0)
                _stage.setTitle("no camera found");
            else {
                _name = arr[0].model;
                _stage.setTitle(_name + ": 0");
                _cam = toupnam.Open(arr[0].id);
                if (_cam != null) {
                    checkAutoExposure.setSelected(_cam.get_Para(toupnam.PARA_AEXPO) != 0);

                    toupnam.bitmapinfo info = _cam.get_Size();
                    _buf = new byte[toupnam.TDIBWIDTHBYTES(info.width * 24) * info.height];
                    _cam.StartPullModeWithCallback(new toupnam.IEventCallback() {
                        /* callbacks come from toupnam.dll/so internal threads */
                        @Override
                        public void onEvent(int nEvent, int nPara) {
                            /* Run the specified Runnable on the JavaFX Application Thread at some unspecified time in the future */
                            Platform.runLater(new EventRunnable(nEvent, nPara));
                        }
                    });
                }
            }
        } catch (toupnam.HRESULTException ex) {
            Alert alert = new Alert(Alert.AlertType.WARNING, ex.toString());
            alert.showAndWait();
        }

        checkAutoExposure.setDisable(_cam == null);
    }

    public static void main(String[] args) {
        launch();
    }
}