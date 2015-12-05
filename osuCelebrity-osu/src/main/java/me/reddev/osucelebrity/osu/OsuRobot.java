package me.reddev.osucelebrity.osu;

import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_TM_CCOEFF_NORMED;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.color.ColorSpace;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuRobot {

  private IplImage downloadButton;
  private IplImage downloadButton2;
  private IplImage downloadButton3;
  private IplImage redownloadButton;
  private Robot robot;

  private final OsuApplicationSettings settings;

  List<Object> doNotGarbageCollect = new ArrayList<>();
  
  boolean loaded = false;

  {
    try {
      downloadButton = convertAndKeepInMemory(toGrayScale(ImageIO.read(new File("download.png"))));
      downloadButton2 =
          convertAndKeepInMemory(toGrayScale(ImageIO.read(new File("download2.png"))));
      downloadButton3 =
          convertAndKeepInMemory(toGrayScale(ImageIO.read(new File("download3.png"))));
      redownloadButton =
          convertAndKeepInMemory(toGrayScale(ImageIO.read(new File("redownload.png"))));
      robot = new Robot();
      robot.setAutoDelay(100);
      loaded = true;
    } catch (Exception e) {
      log.warn("Error while initializing image processing", e);
    }
  }

  private IplImage convertAndKeepInMemory(BufferedImage original) {
    Java2DFrameConverter converter1 = new Java2DFrameConverter();
    ToIplImage converter2 = new ToIplImage();
    // we need to keep these from being garbage collected
    Frame frame = converter1.convert(original);
    doNotGarbageCollect.add(frame);
    return converter2.convert(frame);
  }

  static final ColorConvertOp CONVERT_TO_GRAYSCALE = new ColorConvertOp(
      ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

  static BufferedImage toGrayScale(BufferedImage colored) {
    BufferedImage gray =
        new BufferedImage(colored.getWidth(), colored.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    CONVERT_TO_GRAYSCALE.filter(colored, gray);
    return gray;
  }

  void findAndClick(IplImage input1, IplImage target1) {
    int iwidth = input1.width() - target1.width() + 1;
    int iheight = input1.height() - target1.height() + 1;

    double detectionScore;
    try (DoublePointer min = new DoublePointer(1);
        DoublePointer max = min;
        CvPoint minLoc = new CvPoint(2);
        CvPoint detectionLoc = new CvPoint(2);
        CvSize cvSize = cvSize(iwidth, iheight);) {

      IplImage map = IplImage.create(cvSize, 32, 1);
      opencv_imgproc.cvMatchTemplate(input1, target1, map, CV_TM_CCOEFF_NORMED);
      opencv_core.cvMinMaxLoc(map, min, max, minLoc, detectionLoc, null);
      map.release();

      detectionScore = max.get(0);

      if (detectionScore < 0.9) {
        return;
      }

      int clickX = detectionLoc.x() + target1.width() / 2 + settings.getOsuClientXOffset();
      int clickY = detectionLoc.y() + target1.height() / 2 + settings.getOsuClientYOffset();

      robot.mouseMove(clickX, clickY);

      robot.mousePress(InputEvent.BUTTON1_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_MASK);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes a screenshot and tries to find all images.
   */
  public void findImages() {
    if (!loaded) {
      return;
    }
    try {
      Rectangle region =
          new Rectangle(settings.getOsuClientXOffset(), settings.getOsuClientYOffset(),
              settings.getOsuClientWidth(), settings.getOsuClientHeight());
      BufferedImage bufferedScreenshot = toGrayScale(robot.createScreenCapture(region));
      // writing this image is very fast, so I'd like to keep it for adjusting offsets
      ImageIO.write(bufferedScreenshot, "BMP", new File("screenshot.bmp"));

      Java2DFrameConverter converter1 = new Java2DFrameConverter();
      ToIplImage converter2 = new ToIplImage();
      Frame frame = converter1.convert(bufferedScreenshot);
      IplImage screenshot = converter2.convert(frame);

      findAndClick(screenshot, downloadButton);
      findAndClick(screenshot, downloadButton2);
      findAndClick(screenshot, downloadButton3);
      findAndClick(screenshot, redownloadButton);
      screenshot.release();
    } catch (Exception e) {
      log.error("exception while trying to find");
    }
  }

}
