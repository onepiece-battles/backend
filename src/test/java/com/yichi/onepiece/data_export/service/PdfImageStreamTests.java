package com.yichi.onepiece.data_export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.itextpdf.io.image.ImageData;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PdfImageStreamTests {
  @DataProvider
  public Object[][] imageValidity() {
    return new Object[][] {{true}, {false}};
  }

  @Test(dataProvider = "imageValidity")
  public void closesOssStreamEvenWhenImageDecodingFallsBack(boolean validImage) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
    var stream = spy(new ByteArrayInputStream(validImage ? output.toByteArray() : new byte[] {1, 2, 3}));
    var character = mock(CharacterEntity.class);
    when(character.getImageUri()).thenReturn("oss-test");
    var loader = new ReportImageLoader();
    try (var storage = mockStatic(AliCloudObjectStorage.class)) {
      storage.when(() -> AliCloudObjectStorage.getReportImageStream("oss-test")).thenReturn(stream);
      ImageData image = loader.load(character, "Test");
      assertThat(image).isNotNull();
      assertThat(image.getWidth()).isEqualTo(validImage ? 1f : 96f);
      verify(stream).close();
    }
  }

  @Test
  public void oversizedRemoteImageIsBoundedAndStreamIsClosed() throws Exception {
    var stream = spy(new ByteArrayInputStream(new byte[2 * 1024 * 1024 + 2]));
    var character = mock(CharacterEntity.class);
    when(character.getImageUri()).thenReturn("oss-test");
    try (var storage = mockStatic(AliCloudObjectStorage.class)) {
      storage.when(() -> AliCloudObjectStorage.getReportImageStream("oss-test")).thenReturn(stream);
      assertThat(new ReportImageLoader().load(character, "Test").getWidth()).isEqualTo(96);
      verify(stream).close();
      verify(stream).readNBytes(2 * 1024 * 1024 + 1);
    }
  }

  @Test
  public void readFailureClosesStreamAndFallsBackToPlaceholder() throws Exception {
    var stream = mock(InputStream.class);
    when(stream.readNBytes(anyInt())).thenThrow(new IOException("Simulated read failure"));
    var character = mock(CharacterEntity.class);
    when(character.getImageUri()).thenReturn("oss-test");
    try (var storage = mockStatic(AliCloudObjectStorage.class)) {
      storage.when(() -> AliCloudObjectStorage.getReportImageStream("oss-test")).thenReturn(stream);
      // The legacy profile_image BLOB fallback was removed (images live only in
      // OSS now), so a failed OSS read falls back to the generated placeholder.
      assertThat(new ReportImageLoader().load(character, "Test").getWidth()).isEqualTo(96);
      verify(stream).close();
    }
  }
}
