package application;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class SampleController {

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private ListView < String > resultListView;

  @FXML
  private ImageView resultImageView;

  @FXML
  private Label status;

  @FXML
  private Button finishStartButton;

  boolean status4button = false;

  private VideoCapture capture;

  private CascadeClassifier faceCascade;

  private int absoluteFaceSize;

  private boolean cameraActive;

  private ScheduledExecutorService timer;

  @FXML
  void initialize() {
    finishStartButton.setText("Başlat");

    this.capture = new VideoCapture();
    this.faceCascade = new CascadeClassifier();
    this.absoluteFaceSize = 0;
  }

  @FXML
  void finishStartButtonClick(ActionEvent event) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    if (!this.cameraActive) {
      this.faceCascade.load("resources/cascade.xml");

      capture.open(1);

      if (capture.isOpened()) {
        this.cameraActive = true;

        Runnable frameGrabber = new Runnable() {

          @Override
          public void run() {
            Mat frame = grabFrame();
            Image imageToShow = Tools.mat2Image(frame);
            Tools.updateImageView(resultImageView, imageToShow);
          }
        };

        this.timer = Executors.newSingleThreadScheduledExecutor();
        this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

        this.finishStartButton.setText("Stop Camera");
      } else {
        System.err.println("Failed to open the camera connection...");
      }
    } else {
      this.cameraActive = false;
      this.finishStartButton.setText("Start Camera");

      this.stopAcquisition();
    }

  }

  private Mat grabFrame() {
    Mat frame = new Mat();

    if (this.capture.isOpened()) {
      try {
        this.capture.read(frame);
        if (!frame.empty()) {
          this.detectAndDisplay(frame);
        }
      } catch (Exception e) {
        System.err.println("Exception during the image elaboration: " + e);
      }
    }
    return frame;
  }

  private void detectAndDisplay(Mat frame) throws InterruptedException {
    MatOfRect faces = new MatOfRect();
    Mat grayFrame = new Mat();

    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
    Imgproc.equalizeHist(grayFrame, grayFrame);

    if (this.absoluteFaceSize == 0) {
      int height = grayFrame.rows();
      if (Math.round(height * 0.2) > 0) {
        this.absoluteFaceSize = (int) Math.round(height * 0.2);
      }
    }

    this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
      new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

    Rect[] facesArray = faces.toArray();
    for (int i = 0; i < facesArray.length; i++) {
      Imgproc.rectangle(frame, new Point(facesArray[i].tl().x, facesArray[i].tl().y), new Point(facesArray[i].br().x, facesArray[i].br().y), new Scalar(255, 0, 0), 5);
      Rect cropRect = new Rect(new Point(facesArray[i].tl().x, facesArray[i].tl().y), new Point(facesArray[i].br().x, facesArray[i].br().y));
      long time = System.currentTimeMillis();

      Mat image_roi = new Mat(frame, cropRect);
      //Core.rotate(image_roi, image_roi,Core.ROTATE_90_COUNTERCLOCKWISE);
      Imgcodecs.imwrite("cekilen/kesilmis/" + time + ".png", image_roi);
      ArrayList < String > resultStringCheck = new ArrayList < String > (); // Create an ArrayList object
      String result = null;
      //Tesseract
      File imageFile = new File("cekilen/kesilmis/" + time + ".png");
      ITesseract instance = new Tesseract();
      instance.setDatapath("tessdata");
      try {
        result = instance.doOCR(imageFile);
        resultStringCheck.add(result);

      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
      System.out.println(result);
      if (result.length() == 8) {
        resultListView.getItems().add(result);
        Thread.sleep(500);

      }

    }
  }

  private void stopAcquisition() {
    if (this.timer != null && !this.timer.isShutdown()) {
      try {
        this.timer.shutdown();
        this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
      }
    }

    if (this.capture.isOpened()) {
      this.capture.release();
    }
  }

  @FXML
  private Button scan;

  @FXML
  void scanclick(ActionEvent event) {
    File imageFile = new File("cekilen/textinjpeg.jpg");
    ITesseract instance = new Tesseract();
    instance.setDatapath("tessdata");

    try {
      String result = instance.doOCR(imageFile);
      resultListView.getItems().add(result);

    } catch (TesseractException e) {
      System.err.println(e.getMessage());
    }

  }

}