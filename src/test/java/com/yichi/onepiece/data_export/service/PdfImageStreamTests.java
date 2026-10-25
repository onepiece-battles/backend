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
  public void downsizesLegacyPortraitAndFlattensTransparencyToWhite() throws Exception {
    var character = mock(CharacterEntity.class);
    when(character.getProfileImage()).thenReturn(org.apache.commons.lang3.ArrayUtils.toObject(BattleReportGenerationTests.Images.ORIGINAL));
    var portrait = new ReportImageLoader().load(character, "Test");
    assertThat(portrait.getWidth()).isLessThanOrEqualTo(256);
    assertThat(portrait.getHeight()).isLessThanOrEqualTo(256);
    assertThat(portrait.getData().length).isLessThan(BattleReportGenerationTests.Images.ORIGINAL.length / 5);
    var transparent = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    var source = new ByteArrayOutputStream();
    ImageIO.write(transparent, "png", source);
    when(character.getProfileImage()).thenReturn(org.apache.commons.lang3.ArrayUtils.toObject(source.toByteArray()));
    var flattened = new ReportImageLoader().load(character, "Test");
    var decoded = ImageIO.read(new ByteArrayInputStream(flattened.getData()));
    assertThat(decoded.getRGB(0, 0) & 0xffffff).isEqualTo(0xffffff);
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
  public void readFailureClosesStreamAndUsesLegacyFallback() throws Exception {
    var stream = mock(InputStream.class);
    when(stream.readNBytes(anyInt())).thenThrow(new IOException("Simulated read failure"));
    var character = mock(CharacterEntity.class);
    when(character.getImageUri()).thenReturn("oss-test");
    when(character.getProfileImage()).thenReturn(org.apache.commons.lang3.ArrayUtils.toObject(BattleReportGenerationTests.Images.REPORT));
    try (var storage = mockStatic(AliCloudObjectStorage.class)) {
      storage.when(() -> AliCloudObjectStorage.getReportImageStream("oss-test")).thenReturn(stream);
      assertThat(new ReportImageLoader().load(character, "Test").getWidth()).isEqualTo(256);
      verify(stream).close();
    }
  }
}
