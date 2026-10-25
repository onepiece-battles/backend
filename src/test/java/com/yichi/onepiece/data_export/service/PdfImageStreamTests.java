package com.yichi.onepiece.data_export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.itextpdf.io.image.ImageData;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.test.util.ReflectionTestUtils;
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
    var service = new DataExportService(null, null, null, null, null);
    try (var storage = mockStatic(AliCloudObjectStorage.class)) {
      storage.when(() -> AliCloudObjectStorage.getImageStream("oss-test", true)).thenReturn(stream);
      ImageData image = ReflectionTestUtils.invokeMethod(service, "loadCharacterImage", character, "Test", true);
      assertThat(image).isNotNull();
      assertThat(image.getWidth()).isEqualTo(validImage ? 1f : 240f);
      verify(stream).close();
    }
  }
}
