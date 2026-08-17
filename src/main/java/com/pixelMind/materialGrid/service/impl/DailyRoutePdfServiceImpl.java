package com.pixelMind.materialGrid.service.impl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pixelMind.materialGrid.constant.ReportConstants;
import com.pixelMind.materialGrid.dto.response.DailyRouteReportResponse;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.service.DailyRoutePdfService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import com.pixelMind.materialGrid.util.MoneyFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Renders a single Daily Route report row using the same header branding /
 * table-grid / summary-block / signature-footer layout shown in the
 * provided sample. Landscape A4 is used because the 8-column financial
 * table (matching the sample's column set) is comfortably wider than tall.
 */
@Slf4j
@Service
public class DailyRoutePdfServiceImpl implements DailyRoutePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD);
    private static final Font TAGLINE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TOTAL_ROW_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Color HEADER_BG = new Color(64, 64, 64);

    @Override
    public byte[] generatePdf(DailyRouteReportResponse report) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document);
            addDateLine(document, report);
            addTable(document, report);
            addSummary(document, report);
            addSignatureFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate daily route report PDF", e);
            throw new BusinessException("Failed to generate the daily route report PDF",
                    ErrorCodeConstants.INTERNAL_ERROR);
        }
    }

    private void addHeader(Document document) throws Exception {
        Paragraph title = new Paragraph(ReportConstants.COMPANY_NAME, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph tagline = new Paragraph(ReportConstants.COMPANY_TAGLINE, TAGLINE_FONT);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(10f);
        document.add(tagline);

        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setBorder(PdfPCell.BOTTOM);
        ruleCell.setBorderWidth(1.2f);
        ruleCell.setFixedHeight(2f);
        rule.addCell(ruleCell);
        document.add(rule);
    }

    private void addDateLine(Document document, DailyRouteReportResponse report) throws Exception {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(4f);
        document.add(spacer);

        Paragraph datePara = new Paragraph();
        datePara.add(new Chunk("Date: ", LABEL_FONT));
        datePara.add(new Chunk(DateTimeUtil.formatReportDate(report.getDate()), VALUE_FONT));
        datePara.setSpacingAfter(14f);
        document.add(datePara);
    }

    private void addTable(Document document, DailyRouteReportResponse report) throws Exception {
        String[] headers = {"No.", "Lorry No.", "Cube", "Load Count", "Total Amount (Rs.)",
                "Paid Amount (Rs.)", "License Fee (Rs.)", "Balance (Rs.)"};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 16, 10, 10, 16, 16, 13, 13});

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6f);
            table.addCell(cell);
        }

        addRow(table, "01", report.getVehicleNumber(),
                report.getVehicleCapacity() != null ? report.getVehicleCapacity().toPlainString() : "-",
                String.valueOf(report.getLoadCount()),
                MoneyFormatUtil.format(report.getTotalAmount()),
                MoneyFormatUtil.format(report.getPaidAmount()),
                MoneyFormatUtil.format(report.getLicenceFee()),
                MoneyFormatUtil.format(report.getBalance()), TABLE_CELL_FONT);

        // A single-row TOTAL, matching the sample's layout, is trivially
        // identical to the one data row above - included for visual
        // consistency with the multi-vehicle template this format is based on.
        addRow(table, "", "TOTAL", "", "",
                MoneyFormatUtil.format(report.getTotalAmount()),
                MoneyFormatUtil.format(report.getPaidAmount()),
                MoneyFormatUtil.format(report.getLicenceFee()),
                MoneyFormatUtil.format(report.getBalance()), TOTAL_ROW_FONT);

        document.add(table);
    }

    private void addRow(PdfPTable table, String no, String lorryNo, String cube, String loadCount,
                        String totalAmount, String paidAmount, String licenceFee, String balance, Font font) {
        addCell(table, no, Element.ALIGN_CENTER, font);
        addCell(table, lorryNo, Element.ALIGN_LEFT, font);
        addCell(table, cube, Element.ALIGN_CENTER, font);
        addCell(table, loadCount, Element.ALIGN_CENTER, font);
        addCell(table, totalAmount, Element.ALIGN_RIGHT, font);
        addCell(table, paidAmount, Element.ALIGN_RIGHT, font);
        addCell(table, licenceFee, Element.ALIGN_RIGHT, font);
        addCell(table, balance, Element.ALIGN_RIGHT, font);
    }

    private void addCell(PdfPTable table, String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addSummary(Document document, DailyRouteReportResponse report) throws Exception {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(14f);
        document.add(spacer);

        Paragraph heading = new Paragraph("Payment Summary", LABEL_FONT);
        heading.setSpacingAfter(6f);
        document.add(heading);

        addSummaryLine(document, "Total Amount", report.getTotalAmount());
        addSummaryLine(document, "Total Paid Amount", report.getPaidAmount());
        addSummaryLine(document, "Total License Fee", report.getLicenceFee());
        addSummaryLine(document, "Total Balance", report.getBalance());
    }

    private void addSummaryLine(Document document, String label, java.math.BigDecimal value) throws Exception {
        Paragraph line = new Paragraph();
        line.add(new Chunk(label + ": ", LABEL_FONT));
        line.add(new Chunk(ReportConstants.CURRENCY_PREFIX + MoneyFormatUtil.format(value), VALUE_FONT));
        line.setSpacingAfter(3f);
        document.add(line);
    }

    private void addSignatureFooter(Document document) throws Exception {
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(40f);
        footer.setWidths(new float[]{1, 1, 1});

        String[] labels = {"Prepared By", "Checked By", "Authorized Signature"};
        for (String label : labels) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(PdfPCell.TOP);
            cell.setBorderWidth(0.8f);
            cell.setPaddingTop(4f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.addElement(new Phrase(label, VALUE_FONT));
            footer.addCell(cell);
        }
        document.add(footer);
    }
}