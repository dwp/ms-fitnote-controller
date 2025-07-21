package uk.gov.dwp.health.fitnotecontroller.utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdgeDetection {


  public static Mat bufferedImage2Mat(BufferedImage image) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", byteArrayOutputStream);
    byteArrayOutputStream.flush();
    return byteArray2Mat(byteArrayOutputStream.toByteArray());
  }

  public static Mat byteArray2Mat(byte[] byteArray) throws IOException {
    return Imgcodecs.imdecode(new MatOfByte(byteArray), Imgcodecs.IMREAD_UNCHANGED);
  }

  public static BufferedImage mat2BufferedImage(Mat matrix) throws IOException {
    MatOfByte mob = new MatOfByte();
    Imgcodecs.imencode(".jpg", matrix, mob);
    return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
  }

  public static List<Point> extractPaperEdges(BufferedImage image) throws IOException {
    Mat src = EdgeDetection.bufferedImage2Mat(image);
    // Step 1: Convert to grayscale
    Mat gray = new Mat();
    Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
    // Step 2: Apply Gaussian blur to reduce noise
    Mat blurred = new Mat();
    Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
    // Step 3: Apply Canny edge detection
    Mat edges = new Mat();
    Imgproc.Canny(blurred, edges, 50, 150); // Adjust thresholds as needed
    List<MatOfPoint> contours = new ArrayList<>();
    List<MatOfPoint> largeContours = new ArrayList<>();
    Mat hierarchy = new Mat();
    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL,
        Imgproc.CHAIN_APPROX_SIMPLE);
    MatOfPoint largestContour = null;
    double maxArea = 0;

    double minPerimeter = ((src.height() * 2) + (src.width() * 2)) * 0.4;
    for (MatOfPoint contour : contours) {
      MatOfPoint2f approx = new MatOfPoint2f();
      MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
      double peri = Imgproc.arcLength(contour2f, true);
      Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true);
      if (peri > minPerimeter) {
        largeContours.add(contour);

        if (approx.toArray().length == 4 && largerSquare(approx.toList(), minPerimeter)) {
          double area = Imgproc.contourArea(contour);
          if (area > maxArea && area > 100) {
            maxArea = area;
            largestContour = contour;
          }
        }
      }
    }
    if (largestContour == null) {
      if (largeContours.size() == 1) {
        largestContour = largeContours.get(0);
      } else {
        return Collections.emptyList();
      }

    }
    // Step 5: If a quadrilateral is found, use it; otherwise, use Hough Transform
    List<Point> corners = new ArrayList<>();
    if (largestContour != null) {
      // Use the original method's quadrilateral
      MatOfPoint2f approx = new MatOfPoint2f();
      MatOfPoint2f contour2f = new MatOfPoint2f(largestContour.toArray());
      double peri = Imgproc.arcLength(contour2f, true);
      Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true);
      corners = approx.toList();
    }
    return corners;
  }

  private static boolean largerSquare(List<Point> points, double minArea) {
    List<Double> horizontalPoints = points.stream().map(point -> point.x).toList();
    List<Double> verticalPoints = points.stream().map(point -> point.y).toList();
    int lowestX = Collections.min(horizontalPoints).intValue();
    int lowestY = Collections.min(verticalPoints).intValue();
    int highestX = Collections.max(horizontalPoints).intValue();
    int highestY = Collections.max(verticalPoints).intValue();
    int width = highestX - lowestX;
    int height = highestY - lowestY;
    double contourArea = ((height * 2) + (width * 2));
    return contourArea > minArea;
  }
}
