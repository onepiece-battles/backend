package com.yichi.onepiece;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.yichi.onepiece.alicloud.AliCloudOssHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PdfCreate {
  public static void main(String[] args) throws DocumentException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Rectangle rectangle = new Rectangle(PageSize.A4);
    rectangle.setBackgroundColor(BaseColor.GRAY);
    Document doc = new Document(rectangle);
    PdfWriter writer =  PdfWriter.getInstance(doc, outputStream);
    doc.addTitle("battle");
    doc.addAuthor("nika");
    doc.addSubject("battle details");
    doc.addCreator("onepiece-battles");
    doc.setMargins(30,30,30,30);
    doc.open();
    doc.add(new Paragraph("paragraph 1"));
    doc.add(new Paragraph("paragraph 2"));
    doc.add(new Paragraph("paragraph 3"));
    doc.close();
    writer.close();
    AliCloudOssHelper.put("battle", new ByteArrayInputStream(outputStream.toByteArray()));
  }
}
