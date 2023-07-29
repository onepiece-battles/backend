package com.yichi.onepiece.image.service;

import com.google.common.collect.ImmutableSet;
import com.yichi.onepiece.exception.ExceptionUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageUtil {
  private static final ImmutableSet<String> supportedImageFormat =
      ImmutableSet.of("JPG", "PNG", "JPEG", "TIFF");

  public static byte[] loadImageFromDisk(String imagePath, String imageFormat) {
    if (!supportedImageFormat.contains(imageFormat.toUpperCase())) {
      throw ExceptionUtils.unexpectedImageFormat(
          "Image format %s not supported, image location is %s", imageFormat, imagePath);
    }
    try {
      BufferedImage image;
      image = ImageIO.read(new File(imagePath));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, imageFormat, outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[] {};
  }
}
