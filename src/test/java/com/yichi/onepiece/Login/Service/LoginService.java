package com.yichi.onepiece.Login.Service;

import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.Test;

public class LoginService {

  @Test
  public void testExcel() throws IOException {
    FileInputStream s1 =
        new FileInputStream(
            "/Users/zhangyichi/Desktop/OnePieceBattles/src/test/java/com/yichi/onepiece/Login/Service/ae.xlsx");
    FileInputStream s2 =
        new FileInputStream(
            "/Users/zhangyichi/Desktop/OnePieceBattles/src/test/java/com/yichi/onepiece/Login/Service/ae2.xlsx");
    var workbook1 = new XSSFWorkbook(s1);
    var workbook2 = new XSSFWorkbook(s2);
    var result = ExcelUtils.isXSSFWorkBookEquals(workbook1, workbook2);
    System.out.println(result);
  }
}
