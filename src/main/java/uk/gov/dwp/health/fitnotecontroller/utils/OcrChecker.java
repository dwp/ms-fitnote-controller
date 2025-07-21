package uk.gov.dwp.health.fitnotecontroller.utils;

import static uk.gov.dwp.health.fitnotecontroller.utils.OcrUtils.ocrApplyImageFilters;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;

public class OcrChecker {
  private static final String TESSERACT_FOLDER_ERROR =
      "the tessdata configuration file could not be found in %s";
  private static final Logger LOG = LoggerFactory.getLogger(OcrChecker.class.getName());
  private final FitnoteControllerConfiguration configuration;

  public OcrChecker(FitnoteControllerConfiguration config) {
    this.configuration = config;
  }

  public ExpectedFitnoteFormat imageContainsReadableText(ImagePayload imagePayload)
      throws IOException {
    byte[] decode = Base64.decodeBase64(imagePayload.getImage());
    String sessionID = imagePayload.getSessionId();
    final long startTime = System.currentTimeMillis();

    BufferedImage read;

    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decode)) {
      read = ImageIO.read(inputStream);
    }

    LOG.debug("Image Base64 decoded from string");
    LOG.info("Start OCR checks :: SID: {}", sessionID);

    ExpectedFitnoteFormat readableImageFormat;
    readableImageFormat = tryImageWithRotations(read, sessionID);
    if (readableImageFormat.getFinalImage() != null) {
      String readableImageString;

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        BufferedImage bufferedImage = readableImageFormat.getFinalImage();
        ImageIO.write(bufferedImage, "jpg", outputStream);
        readableImageString = Base64.encodeBase64String(outputStream.toByteArray());
      }

      imagePayload.setImage(readableImageString);
    }
    ExpectedFitnoteFormat.Status imageStatus = readableImageFormat.getStatus();
    String reason = readableImageFormat.getFailureReason();

    LOG.info("End OCR checks :: SID: {} {} :: Response time (seconds) = {}", sessionID, imageStatus,
        (System.currentTimeMillis() - startTime) / 1000);
    if (imageStatus == ExpectedFitnoteFormat.Status.FAILED) {
      LOG.warn("[HTF-945] OCR Unsuccessful - {}", reason);
    } else if (imageStatus == ExpectedFitnoteFormat.Status.PARTIAL) {
      LOG.warn("[HTF-945] OCR Unsuccessful - {}", reason);
    }
    return readableImageFormat;
  }

  private Callable<ExpectedFitnoteFormat> buildCallable(BufferedImage originalImage,
                                                        String sessionID, int rotation,
                                                        int threadPriority) {
    return () -> {
      Thread.currentThread().setPriority(threadPriority);
      try (TessBaseAPI instance = new TessBaseAPI()) {

        if (instance.Init(configuration.getTesseractFolderPath(), "eng") != 0) {
          throw new IOException(
              String.format(TESSERACT_FOLDER_ERROR, configuration.getTesseractFolderPath()));
        }
        return ocrRotation(originalImage, sessionID, rotation, instance);
      }
    };

  }

  private ExpectedFitnoteFormat ocrRotation(BufferedImage originalImage, String sessionID,
                                            int rotation, TessBaseAPI ocr) throws IOException {
    ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(configuration);

    if (rotation > 0) {
      fitnoteFormat.setMatchAngle(rotation);
      originalImage = ImageUtils.createRotatedCopy(originalImage, rotation);
    }
    fitnoteFormat.setFinalImage(originalImage);

    ocrScanFitnote(ocr, fitnoteFormat, rotation);
    boolean nhs = originalImage.getHeight() != fitnoteFormat.getFinalImage().getHeight();
    logResult(fitnoteFormat, rotation, sessionID, nhs);

    if (!fitnoteFormat.getStatus().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
      fitnoteFormat.setFinalImage(null);
    }

    LOG.debug("Completed thread {} (priority {}) for sessionId {} with rotation {}",
        Thread.currentThread().getName(), Thread.currentThread().getPriority(), sessionID,
        rotation);
    return fitnoteFormat;
  }

  private synchronized ExpectedFitnoteFormat tryImageWithRotations(BufferedImage originalImage,
                                                                   String sessionID)
      throws IOException {

    Map<String, String> errors = new HashMap<>();
    try (TessBaseAPI instance = new TessBaseAPI()) {

      if (instance.Init(configuration.getTesseractFolderPath(), "eng") != 0) {
        throw new IOException(
            String.format(TESSERACT_FOLDER_ERROR, configuration.getTesseractFolderPath()));
      }
      instance.SetVariable("debug_file", "/dev/null");

      ExpectedFitnoteFormat result = ocrRotation(originalImage, sessionID, 0, instance);

      if (result.getFinalImage() != null || result.getStatus()
          .equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
        return result;
      } else {
        errors.put(String.valueOf(0), result.getFailureReason());
      }
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      CompletionService<ExpectedFitnoteFormat> threadStack =
          new ExecutorCompletionService<>(executorService);
      try {
        int threadPriority = Thread.MAX_PRIORITY;
        int[] rotationAngles = {90, 270};

        threadStack.submit(
            buildCallable(originalImage, sessionID, rotationAngles[0], threadPriority));
        threadPriority -= 3;
        threadStack.submit(
            buildCallable(originalImage, sessionID, rotationAngles[1], threadPriority));

        for (int stack = 0; stack < rotationAngles.length; stack++) {
          result = threadStack.take().get();

          if (result != null) {
            if (result.getFinalImage() != null || result.getStatus()
                .equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
              return result;
            } else {
              errors.put(String.valueOf(rotationAngles[stack]), result.getFailureReason());
            }
          } else {
            LOG.info("Result was null");
          }
        }

      } catch (InterruptedException | ExecutionException e) {
        LOG.error("Thread error :: {}", e.getMessage());
        LOG.debug(e.getClass().getName(), e);

        if (e.getCause() instanceof IOException) {
          throw new IOException(e);
        }

      } finally {
        long startTime = System.currentTimeMillis();
        try {
          executorService.shutdownNow();
          executorService.awaitTermination(1, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
          LOG.error("{} :: {}", e.getClass().getName(), e.getMessage());
          LOG.debug(e.getClass().getName(), e);
          Thread.currentThread().interrupt();
        }

        LOG.info("Threads closed from OCR in {} ms", System.currentTimeMillis() - startTime);
      }

      result = ocrRotation(originalImage, sessionID, 180, instance);

      if (result.getFinalImage() != null || result.getStatus()
          .equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
        return result;
      } else {
        errors.put(String.valueOf(180), result.getFailureReason());
      }

      LOG.info("SID: {} Image Failed OCR", sessionID);

      return new ExpectedFitnoteFormat(ExpectedFitnoteFormat.Status.FAILED,
          convertWithStream(errors));
    }
  }

  public String convertWithStream(Map<String, String> map) {
    return map.keySet().stream().map(key -> key + "=" + map.get(key))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  private void logResult(ExpectedFitnoteFormat localFitnoteFormat, int rotation, String sessionID,
                         boolean nhs) {
    int maxChars = configuration.getMaxLogChars();

    String tlChars = cutStringToMaxLength(localFitnoteFormat.getTopLeftStringToLog(), maxChars);
    String trChars = cutStringToMaxLength(localFitnoteFormat.getTopRightStringToLog(), maxChars);
    String blChars = cutStringToMaxLength(localFitnoteFormat.getBaseLeftStringToLog(), maxChars);
    String brChars = cutStringToMaxLength(localFitnoteFormat.getBaseRightStringToLog(), maxChars);

    LOG.info("Running OCR checks :: SID: {} {} @ {}°", sessionID,
        localFitnoteFormat.getLoggingString(nhs, configuration.getOcrVerticalSlice()), rotation);
    LOG.debug("****SID: {} Top Left String :: Rotation {} :: {}", sessionID, rotation, tlChars);
    LOG.debug("****SID: {} Top Right String :: Rotation {} :: {}", sessionID, rotation, trChars);
    LOG.debug("****SID: {} Base Left String :: Rotation {} :: {}", sessionID, rotation, blChars);
    LOG.debug("****SID: {} Base Right String :: Rotation {} :: {}", sessionID, rotation, brChars);
  }

  private String cutStringToMaxLength(String localString, int maxChars) {
    return localString.length() > maxChars ? localString.substring(0, maxChars) : localString;
  }

  private void ocrScanFitnote(TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int rotation)
      throws IOException {
    LOG.debug("OCR :: Brightness target {}, Contrast {}", configuration.getTargetBrightness(),
        configuration.getContrastCutOff());
    int height = fitnoteFormat.getFinalImage().getHeight();
    int width = fitnoteFormat.getFinalImage().getWidth();
    boolean portrait = !ImageUtils.isLandscape(fitnoteFormat.getFinalImage());
    boolean checkNHS = rotation == 0 && portrait;
    BufferedImage origImage = fitnoteFormat.getFinalImage();
    BufferedImage workingImage = origImage;

    ForkJoinTask.invokeAll(
        new OcrApplyImageFilters(workingImage, ocr, fitnoteFormat, "TL",
            configuration.getHighTarget(), configuration));

    if (fitnoteFormat.getTopLeftPercentage() < configuration.getDiagonalTarget()) {
      LOG.info("TL {} < {}, impossible diagonal match, move to BL",
          fitnoteFormat.getTopLeftPercentage(), configuration.getDiagonalTarget());

    } else {
      if (checkNHS && fitnoteFormat.getTopHeight() == 0
          && fitnoteFormat.getTopLeftPercentage() >= configuration.getStrictTarget()) {
        ForkJoinTask.invokeAll(
            new OcrApplyImageFilters(workingImage, ocr, fitnoteFormat, "TR",
                configuration.getHighTarget(), configuration));
        if (fitnoteFormat.getTopRightPercentage() >= configuration.getStrictTarget()
            && fitnoteFormat.getTopHeight() == 0) {
          BufferedImage halfImage =
              fitnoteFormat.getFinalImage().getSubimage(0, 0, width, height / 2);
          fitnoteFormat.setFinalImage(halfImage);
          workingImage = halfImage;
        } else {
          checkNHS = false;
        }
      } else {
        checkNHS = false;
      }
      int targetPercentage = configuration.getHighTarget();
      if (checkNHS && fitnoteFormat.getTopLeftPercentage() >= configuration.getHighTarget()) {
        targetPercentage = configuration.getStrictTarget();
      } else if (fitnoteFormat.getTopLeftPercentage() >= configuration.getHighTarget()) {
        targetPercentage = configuration.getDiagonalTarget();
      }
      if (checkNHS) {
        ocrScanBaseRight(
            ocr,
            fitnoteFormat,
            width,
            height / 2,
            targetPercentage,
            configuration.getOcrVerticalSlice());
      } else {
        ForkJoinTask.invokeAll(new OcrApplyImageFilters(workingImage, ocr, fitnoteFormat, "BR",
            targetPercentage, configuration));
      }

      boolean strictMatch = getStrictMatch(checkNHS, fitnoteFormat, portrait);

      if (fitnoteFormat.validateFitnotePassed(checkNHS, strictMatch,
              configuration.getStrictTarget())
          .equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
        LOG.info("no need to continue scanning, matched on TL/BR");
        return;
      }
    }

    try (TessBaseAPI ocr2 = new TessBaseAPI()) {

      if (ocr2.Init(configuration.getTesseractFolderPath(), "eng") != 0) {
        throw new IOException("Error setting up tesseract. Tesseract folder does not exist");
      }
      fitnoteFormat.resetHeight();
      List<OcrApplyImageFilters> tasks = new ArrayList<>();
      if (checkNHS) {
        ocrScanBaseLeft(
            ocr,
            fitnoteFormat,
            width,
            height / 2,
            configuration.getHighTarget(),
            configuration.getOcrVerticalSlice());

      } else {
        tasks.add(
            new OcrApplyImageFilters(workingImage, ocr, fitnoteFormat, "BL",
                configuration.getHighTarget(), configuration));
        tasks.add(
              new OcrApplyImageFilters(workingImage, ocr2, fitnoteFormat, "TR",
                  configuration.getHighTarget(), configuration));
        ForkJoinTask.invokeAll(tasks);
      }

      boolean strictMatch = getStrictMatch(checkNHS, fitnoteFormat, portrait);

      if (fitnoteFormat.validateFitnotePassed(checkNHS, strictMatch,
              configuration.getStrictTarget())
          .equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
        LOG.info("no need to continue scanning, matched on TR/BL");
        return;
      }
      if (!checkNHS) {
        LOG.info("no need to continue scanning as already failed as non NHS");
        return;
      }
      checkNHS = false;
      fitnoteFormat.setFinalImage(origImage);
      workingImage = origImage;
      tasks.clear();
      fitnoteFormat.resetHeight();
      int targetPercentageBR = configuration.getHighTarget();
      if (fitnoteFormat.getTopLeftPercentage() >= configuration.getHighTarget()) {
        if (fitnoteFormat.getTopHeight() == 0) {
          targetPercentageBR = configuration.getDiagonalTargetStrict();
        } else {
          targetPercentageBR = configuration.getDiagonalTarget();
        }
      }
      int targetPercentageBL = configuration.getHighTarget();
      if (fitnoteFormat.getTopRightPercentage() >= configuration.getHighTarget()) {
        if (fitnoteFormat.getTopHeight() == 0) {
          targetPercentageBL = configuration.getDiagonalTargetStrict();
        } else {
          targetPercentageBL = configuration.getDiagonalTarget();
        }
      }
      tasks.add(
          new OcrApplyImageFilters(workingImage, ocr, fitnoteFormat, "BL", targetPercentageBL,
              configuration));
      tasks.add(
          new OcrApplyImageFilters(workingImage, ocr2, fitnoteFormat, "BR",
              targetPercentageBR,
              configuration));
      ForkJoinTask.invokeAll(tasks);
      strictMatch = getStrictMatch(checkNHS, fitnoteFormat, portrait);
      fitnoteFormat.validateFitnotePassed(checkNHS, strictMatch, configuration.getStrictTarget());
    }

  }

  private void ocrScanBaseLeft(
      TessBaseAPI ocr,
      ExpectedFitnoteFormat fitnoteFormat,
      int width,
      int height,
      int targetPercentage,
      int verticalSlice) {
    int heightDifferential = height / verticalSlice;

    BufferedImage subImage =
        fitnoteFormat
            .getFinalImage()
            .getSubimage(
                0, heightDifferential * (verticalSlice - 1), width / 2, heightDifferential);
    ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "BL", targetPercentage, configuration);
  }

  private void ocrScanBaseRight(
      TessBaseAPI ocr,
      ExpectedFitnoteFormat fitnoteFormat,
      int width,
      int height,
      int targetPercentage,
      int verticalSlice) {
    int heightDifferential = height / verticalSlice;

    BufferedImage subImage =
        fitnoteFormat
            .getFinalImage()
            .getSubimage(
                width / 2, heightDifferential * (verticalSlice - 1), width / 2, heightDifferential);
    ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "BR", targetPercentage, configuration);
  }

  private boolean getStrictMatch(boolean checkNHS, ExpectedFitnoteFormat fitnoteFormat,
                                 boolean portrait) {
    int height = fitnoteFormat.getFinalImage().getHeight();
    int verticalSlice = configuration.getOcrVerticalSlice();
    int heightDifferential = height / verticalSlice;
    int bottomSlice = heightDifferential * (verticalSlice - 1);
    return !checkNHS && (portrait || (fitnoteFormat.getTopHeight() > 0
        || (fitnoteFormat.getBottomHeight() > 0 && fitnoteFormat.getBottomHeight()
        < bottomSlice)));
  }

}
