package application;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
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

    if (!this.cameraActive)
	{
		this.checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");

		capture.open(1);
		
		if (capture.isOpened())
		{
			this.cameraActive = true;
			
			Runnable frameGrabber = new Runnable() {
				
				@Override
				public void run()
				{
					Mat frame = grabFrame();
					Image imageToShow = Tools.mat2Image(frame);
					Tools.updateImageView(resultImageView, imageToShow);
				}
			};
			
			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
			
			this.finishStartButton.setText("Stop Camera");
		}
		else
		{
			System.err.println("Failed to open the camera connection...");
		}
	}
	else
	{
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
  private void detectAndDisplay(Mat frame) {
    MatOfRect faces = new MatOfRect();
    Mat grayFrame = new Mat();

    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
    Imgproc.equalizeHist(grayFrame, grayFrame);

    if (this.absoluteFaceSize == 0) {
      int height = grayFrame.rows();
      if (Math.round(height * 0.2 ) > 0) {
        this.absoluteFaceSize = (int) Math.round(height * 0.2 );
      }
    }

    this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
      new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

    Rect[] facesArray = faces.toArray();
    for (int i = 0; i < facesArray.length; i++)
    { Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(255,0,0), 3);
    Rect cropRect = new Rect(facesArray[i].tl(), facesArray[i].br());

    Mat image_roi = new Mat(frame,cropRect);       
    Imgcodecs.imwrite("cekilen/"+System.currentTimeMillis()+"yuz.png", image_roi);

    	System.out.println("yüz bulundu.");

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
  private void checkboxSelection(String classifierPath)
	{
		this.faceCascade.load(classifierPath);
	}
	


}