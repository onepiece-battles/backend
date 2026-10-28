package com.yichi.onepiece.data_export.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReportImageLoader {
  private static final Logger logger = LoggerFactory.getLogger(ReportImageLoader.class);
  private static final int MAX_DIMENSION = 256;
  private static final int MAX_REMOTE_BYTES = 2 * 1024 * 1024;
  private static final int MAX_LEGACY_BYTES = 12 * 1024 * 1024;

  ImageData load(CharacterEntity character, String fallbackName) {
    if (character.getImageUri() != null && !character.getImageUri().isBlank()) {
      try (var stream = AliCloudObjectStorage.getReportImageStream(character.getImageUri())) {
        byte[] bytes = stream.readNBytes(MAX_REMOTE_BYTES + 1);
        if (bytes.length > MAX_REMOTE_BYTES) {
          throw new IOException("Report portrait exceeds the download limit");
        }
        return thumbnail(bytes);
      } catch (Exception exception) {
        logger.warn("Failed to load report portrait for character {}, using fallback", character.getId(), exception);
      }
    }
    return placeholder(fallbackName);
  }

  private ImageData thumbnail(byte[] bytes) throws IOException {
    if (bytes.length > 2 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8) {
      var jpeg = ImageDataFactory.create(bytes);
      if (jpeg.getWidth() > 0 && jpeg.getHeight() > 0
          && jpeg.getWidth() <= MAX_DIMENSION && jpeg.getHeight() <= MAX_DIMENSION) {
        return jpeg;
      }
    }
    try (var source = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(source);
      if (!readers.hasNext()) {
        throw new IOException("Unsupported report portrait format");
      }
      var reader = readers.next();
      try {
        reader.setInput(source);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > 40_000_000L) {
          throw new IOException("Invalid report portrait dimensions");
        }
        var parameters = reader.getDefaultReadParam();
        int sampling = Math.max(1, Math.max(width, height) / MAX_DIMENSION);
        parameters.setSourceSubsampling(sampling, sampling, 0, 0);
        var image = reader.read(0, parameters);
        double ratio = Math.min(1, (double) MAX_DIMENSION / Math.max(image.getWidth(), image.getHeight()));
        var resized = new BufferedImage(Math.max(1, (int) Math.round(image.getWidth() * ratio)),
            Math.max(1, (int) Math.round(image.getHeight() * ratio)), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
          graphics.setColor(Color.WHITE);
          graphics.fillRect(0, 0, resized.getWidth(), resized.getHeight());
          graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          graphics.drawImage(image, 0, 0, resized.getWidth(), resized.getHeight(), null);
        } finally {
          graphics.dispose();
        }
        return jpeg(resized);
      } finally {
        reader.dispose();
      }
    }
  }

  private ImageData placeholder(String name) {
    var image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(new Color(237, 243, 248));
      graphics.fillRect(0, 0, 96, 96);
      graphics.setColor(new Color(40, 101, 160));
      graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 25));
      String text = name == null || name.isBlank() ? "?" : name.strip();
      text = text.substring(0, text.offsetByCodePoints(0, Math.min(2, text.codePointCount(0, text.length()))))
          .toUpperCase(Locale.ROOT);
      graphics.drawString(text, (96 - graphics.getFontMetrics().stringWidth(text)) / 2, 56);
    } finally {
      graphics.dispose();
    }
    try {
      return jpeg(image);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create report portrait", exception);
    }
  }

  private ImageData jpeg(BufferedImage image) throws IOException {
    var output = new ByteArrayOutputStream();
    var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    try (var destination = ImageIO.createImageOutputStream(output)) {
      writer.setOutput(destination);
      var parameters = writer.getDefaultWriteParam();
      parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      parameters.setCompressionQuality(0.8f);
      writer.write(null, new javax.imageio.IIOImage(image, null, null), parameters);
    } finally {
      writer.dispose();
    }
    return ImageDataFactory.create(output.toByteArray());
  }
}
