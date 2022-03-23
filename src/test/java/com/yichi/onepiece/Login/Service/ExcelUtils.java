package com.yichi.onepiece.Login.Service;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.presentation.StandardRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtils {
  public static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

  private ExcelUtils() {}

  /**
   * Take two XSSFWorkbooks, return false if
   * <li>1. number of sheets not match
   * <li>2. name of counterpart sheets not match
   * <li>3. number of counterpart sheets not match
   * <li>4. dataType of cells not match
   * <li>5. values of cells not match
   * <li>6. null condition of row/cell not match. e.g. one row is null and another is not.
   *
   *     <p>Otherwise, return true.
   *
   * @param workbook1 first XSSFWorkbook.
   * @param workbook2 second XSSFWorkbook.
   * @return boolean value indicating two XSSFWorkbooks are identical or not.
   */
  public static boolean isXSSFWorkBookEquals(XSSFWorkbook workbook1, XSSFWorkbook workbook2) {
    int sheetNum1 = workbook1.getNumberOfSheets();
    int sheetNum2 = workbook2.getNumberOfSheets();
    if (sheetNum1 != sheetNum2) {
      logger.info("can not compare excels with different number of sheets");
      return false;
    }
    for (int i = 0; i < sheetNum1; i++) {
      XSSFSheet sheet1 = workbook1.getSheetAt(i);
      XSSFSheet sheet2 = workbook2.getSheetAt(i);
      if (!sheet1.getSheetName().equals(sheet2.getSheetName())) {
        logger.info(
            "Can not compare sheet with different names: {} and {}",
            sheet1.getSheetName(),
            sheet2.getSheetName());
        return false;
      }
      if (!sameSheets(sheet1, sheet2)) {
        logger.info("Sheet {} is different", sheet1.getSheetName());
        return false;
      }
    }
    return true;
  }

  public static boolean sameSheets(XSSFSheet sheet1, XSSFSheet sheet2) {
    int firstRowNum1 = sheet1.getFirstRowNum();
    int lastRowNum1 = sheet1.getLastRowNum();

    int firstRowNum2 = sheet2.getFirstRowNum();
    int lastRowNum2 = sheet2.getLastRowNum();

    if (firstRowNum1 != firstRowNum2 || lastRowNum1 != lastRowNum2) {
      logger.info("sheet {} has different number of rows", sheet1.getSheetName());
      return false;
    }

    for (int i = firstRowNum1; i <= lastRowNum1; i++) {
      var row1 = sheet1.getRow(i);
      var row2 = sheet2.getRow(i);
      if (!sameTwoRows(row1, row2)) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameTwoRows(XSSFRow row1, XSSFRow row2) {
    if (row1 == null && row2 == null) {
      return true;
    } else if (row1 == null || row2 == null) {
      logger.info("one row is null and another row is not null");
      return false;
    }
    int firstCellNum1 = row1.getFirstCellNum();
    int lastCellNum1 = row1.getLastCellNum();
    int firstCellNum2 = row1.getFirstCellNum();
    int lastCellNum2 = row1.getLastCellNum();
    if (firstCellNum1 != firstCellNum2 || lastCellNum1 != lastCellNum2) {
      logger.info("row {} has different number of cells", row1.getRowNum());
      return false;
    }
    for (int i = firstCellNum1; i <= lastCellNum1; i++) {
      if (!sameTwoCells(row1.getCell(i), row2.getCell(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameTwoCells(XSSFCell cell1, XSSFCell cell2) {
    if ((cell1 == null) && (cell2 == null)) {
      return true;
    } else if ((cell1 == null) || (cell2 == null)) {
      return false;
    }
    var cellAddress = cell1.getAddress();

    assertCellStyleEquals(cellAddress, cell1.getCellStyle(), cell2.getCellStyle());

    if (!cell1.getCellType().equals(cell2.getCellType())) {
      logger.info(
          "types of cell located at {} are different, they are: \"{}\" and \"{}\"",
          cellAddress,
          cell1.getCellType(),
          cell2.getCellType());
      return false;
    }

    switch (cell1.getCellType()) {
      case FORMULA:
        if (!cell1.getCellFormula().equals(cell2.getCellFormula())) {
          logger.info(
              "Formulas of cell located at {} are different, they are  \"{}\" and \"{}\"",
              cellAddress,
              cell1.getCellFormula(),
              cell2.getCellFormula());
          return false;
        }
        break;
      case NUMERIC:
        if (cell1.getNumericCellValue() != cell2.getNumericCellValue()) {
          logger.info(
              "numeric value of cell located at {} not match, they are  \"{}\" and \"{}\"",
              cellAddress,
              cell1.getNumericCellValue(),
              cell2.getNumericCellValue());
          return false;
        }
        break;
      case BLANK:
        return true;
      case BOOLEAN:
        if (cell1.getBooleanCellValue() != cell2.getBooleanCellValue()) {
          logger.info(
              "boolean value of cell located at {} not match, they are \"{}\" and \"{}\"",
              cellAddress,
              cell1.getBooleanCellValue(),
              cell2.getBooleanCellValue());
          return false;
        }
        break;
      case ERROR:
        if (cell1.getErrorCellValue() != cell2.getErrorCellValue()) {
          logger.info(
              "error code of cell located at {}  not match, they are  \"{}\" and \"{}\"",
              cellAddress,
              cell1.getErrorCellValue(),
              cell2.getErrorCellValue());
          return false;
        }
        break;
      default:
        if (!cell1.getStringCellValue().equals(cell2.getStringCellValue())) {
          logger.info(
              "string value of cell located at {} not match, they are \"{}\" and \"{}\"",
              cellAddress,
              cell1.getStringCellValue(),
              cell2.getStringCellValue());
          return false;
        }
        break;
    }
    return true;
  }

  public static class CustomRepresentation extends StandardRepresentation {
    @Override
    public String toStringOf(Object o) {
      if (o instanceof XSSFCellStyle) {
        XSSFCellStyle cellStyle = (XSSFCellStyle) o;
        return String.format(
            "horizontal alignment: %s, vertical alignment: %s, coreXf: %s, dataFormat: %s,"
                + " fillPattern: %s, fillBackgroundColor: %s, fillForegroundColor: %s,"
                + " shrinkToFit: %s, hidden: %s, locked: %s, quotePrefixed: %s, wrapText: %s,"
                + " rotation: %s, indention: %s, borderBottom: %s, borderLeft: %s, borderTop: %s,"
                + " borderRight: %s",
            cellStyle.getAlignment(),
            cellStyle.getVerticalAlignment(),
            cellStyle.getCoreXf(),
            cellStyle.getDataFormatString(),
            cellStyle.getFillPattern(),
            cellStyle.getFillBackgroundColor(),
            cellStyle.getFillForegroundColor(),
            cellStyle.getShrinkToFit(),
            cellStyle.getHidden(),
            cellStyle.getLocked(),
            cellStyle.getQuotePrefixed(),
            cellStyle.getWrapText(),
            cellStyle.getRotation(),
            cellStyle.getIndention(),
            cellStyle.getBorderBottom(),
            cellStyle.getBorderLeft(),
            cellStyle.getBorderTop(),
            cellStyle.getBorderRight());
      }
      return super.toStringOf(o);
    }
  }

  private static void assertCellStyleEquals(
      CellAddress cellAddress, XSSFCellStyle style1, XSSFCellStyle style2) {
    CustomRepresentation customRepresentation = new CustomRepresentation();
    assertThat(style1)
        .describedAs("Cell styles are different at: %s", cellAddress)
        .withRepresentation(customRepresentation)
        .isEqualTo(style2);
  }
}
