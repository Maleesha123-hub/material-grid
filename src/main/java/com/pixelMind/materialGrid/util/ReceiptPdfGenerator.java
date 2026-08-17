package com.pixelMind.materialGrid.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pixelMind.materialGrid.dto.response.receipt.ReceiptItemDTO;
import com.pixelMind.materialGrid.dto.response.receipt.ReceiptSummaryDTO;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class ReceiptPdfGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(60, 60, 60));
    private static final Font LABEL_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font TABLE_TOTAL_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
    private static final Font NOTE_FONT = new Font(Font.HELVETICA, 7, Font.ITALIC, new Color(100, 100, 100));

    private static final Color HEADER_BG = new Color(230, 230, 230);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat CUBE_FORMAT = new DecimalFormat("#,##0.0#");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private ReceiptPdfGenerator() {
    }

    public static byte[] generateReceiptPdf(ReceiptSummaryDTO receipt) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Company Header
            addCompanyHeader(document);

            // 2. Date & Project/Site Meta Line
            addMetaLine(document, receipt);

            // 3. Main Data Table
            addMainTable(document, receipt);

            // 4. Payment Summary Box
            addPaymentSummary(document, receipt);

            // 5. Signatory Footer
            addSignatoryFooter(document);

            // 6. Bottom Note
            addBottomNote(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render PDF receipt", e);
            throw new RuntimeException("Error generating receipt PDF: " + e.getMessage(), e);
        }
    }

    private static void addCompanyHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph("MALSHI SUPPLIERS", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("BUILDING MATERIALS SUPPLIERS & TRANSPORT SERVICES", SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(8f);
        document.add(subtitle);

        // Divider line
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setBorder(PdfPCell.BOTTOM);
        ruleCell.setBorderWidth(1.5f);
        ruleCell.setBorderColor(Color.BLACK);
        ruleCell.setFixedHeight(2f);
        rule.addCell(ruleCell);
        rule.setSpacingAfter(10f);
        document.add(rule);
    }

    private static void addMetaLine(Document document, ReceiptSummaryDTO receipt) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{12, 38, 18, 32});
        metaTable.setSpacingAfter(10f);

        addNoBorderCell(metaTable, "Date", LABEL_BOLD, Element.ALIGN_LEFT);
        addNoBorderCell(metaTable, receipt.getDate() != null ? receipt.getDate().format(DATE_FORMATTER) : "-", VALUE_FONT, Element.ALIGN_LEFT);
        addNoBorderCell(metaTable, "Project / Site", LABEL_BOLD, Element.ALIGN_LEFT);
        addNoBorderCell(metaTable, receipt.getProjectSite() != null ? receipt.getProjectSite() : "Warakapola", VALUE_FONT, Element.ALIGN_LEFT);

        document.add(metaTable);
    }

    private static void addMainTable(Document document, ReceiptSummaryDTO receipt) throws DocumentException {
        String[] headers = {
                "No.", "Lorry No.", "Load Count", "Total Amount\n(Rs.)", "Paid Amount\n(Rs.)", "License Fee\n(Rs.)", "Balance\n(Rs.)"
        };
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 16, 12, 18, 16, 16, 16});
        table.setSpacingAfter(14f);

        // Render header cells
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5f);
            cell.setBorderWidth(0.8f);
            table.addCell(cell);
        }

        // Render data rows
        int rowIdx = 1;
        if (receipt.getItems() != null && !receipt.getItems().isEmpty()) {
            for (ReceiptItemDTO item : receipt.getItems()) {
                String noStr = String.format("%02d", rowIdx++);
                addBorderedCell(table, noStr, TABLE_CELL_FONT, Element.ALIGN_CENTER);
                addBorderedCell(table, item.getLorryNo() != null ? item.getLorryNo() : "-", TABLE_CELL_FONT, Element.ALIGN_CENTER);
                addBorderedCell(table, CUBE_FORMAT.format(item.getCube()), TABLE_CELL_FONT, Element.ALIGN_CENTER);
                addBorderedCell(table, formatMoney(item.getTotalAmount()), TABLE_CELL_FONT, Element.ALIGN_RIGHT);
                addBorderedCell(table, formatMoney(item.getDailyExpenses()), TABLE_CELL_FONT, Element.ALIGN_RIGHT);
                addBorderedCell(table, formatMoney(item.getLicenseFee()), TABLE_CELL_FONT, Element.ALIGN_RIGHT);
                addBorderedCell(table, formatMoney(item.getBalance()), TABLE_CELL_FONT, Element.ALIGN_RIGHT);
            }
        }

        // Pad with empty rows if fewer than 7 rows to match template layout
        int currentCount = receipt.getItems() != null ? receipt.getItems().size() : 0;
        for (int i = currentCount + 1; i <= Math.max(currentCount, 7); i++) {
            String noStr = String.format("%02d", i);
            addBorderedCell(table, noStr, TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_RIGHT);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_RIGHT);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_RIGHT);
            addBorderedCell(table, "", TABLE_CELL_FONT, Element.ALIGN_RIGHT);
        }

        // TOTAL row
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL", TABLE_TOTAL_FONT));
        totalLabelCell.setColspan(2);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalLabelCell.setBackgroundColor(HEADER_BG);
        totalLabelCell.setPadding(5f);
        totalLabelCell.setBorderWidth(0.8f);
        table.addCell(totalLabelCell);

        addBorderedCellWithBg(table, CUBE_FORMAT.format(receipt.getTotalCubes()), TABLE_TOTAL_FONT, Element.ALIGN_CENTER, HEADER_BG);
        addBorderedCellWithBg(table, formatMoney(receipt.getTotalAmount()), TABLE_TOTAL_FONT, Element.ALIGN_RIGHT, HEADER_BG);
        addBorderedCellWithBg(table, formatMoney(receipt.getTotalExpenses()), TABLE_TOTAL_FONT, Element.ALIGN_RIGHT, HEADER_BG);
        addBorderedCellWithBg(table, formatMoney(receipt.getTotalLicenseFee()), TABLE_TOTAL_FONT, Element.ALIGN_RIGHT, HEADER_BG);
        addBorderedCellWithBg(table, formatMoney(receipt.getTotalBalance()), TABLE_TOTAL_FONT, Element.ALIGN_RIGHT, HEADER_BG);

        document.add(table);
    }

    private static void addPaymentSummary(Document document, ReceiptSummaryDTO receipt) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setWidths(new float[]{45, 55});
        summaryTable.setSpacingAfter(25f);

        // Header
        PdfPCell headerCell = new PdfPCell(new Phrase("Payment Summary", LABEL_BOLD));
        headerCell.setColspan(2);
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(4f);
        headerCell.setBorderWidth(0.8f);
        summaryTable.addCell(headerCell);

        addSummaryRow(summaryTable, "Total Amount", receipt.getTotalAmount());
        addSummaryRow(summaryTable, "Total Paid Amount", receipt.getTotalExpenses());
        addSummaryRow(summaryTable, "Total License Fee", receipt.getTotalLicenseFee());
        addSummaryRow(summaryTable, "Total Balance", receipt.getTotalBalance());

        document.add(summaryTable);
    }

    private static void addSummaryRow(PdfPTable table, String label, BigDecimal amount) {
        addBorderedCell(table, label, TABLE_HEADER_FONT, Element.ALIGN_LEFT);
        addBorderedCell(table, "Rs. " + formatMoney(amount), TABLE_CELL_FONT, Element.ALIGN_LEFT);
    }

    private static void addSignatoryFooter(Document document) throws DocumentException {
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[]{1, 1, 1});
        footer.setSpacingBefore(30f);
        footer.setSpacingAfter(15f);

        String[] roles = {"Prepared By", "Checked By", "Authorized Signature"};
        for (String role : roles) {
            PdfPCell cell = new PdfPCell(new Phrase(role, LABEL_BOLD));
            cell.setBorder(PdfPCell.TOP);
            cell.setBorderWidth(1f);
            cell.setPaddingTop(5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footer.addCell(cell);
        }

        document.add(footer);
    }

    private static void addBottomNote(Document document) throws DocumentException {
        Paragraph note = new Paragraph("Note: Balance & totals calculate automatically.", NOTE_FONT);
        note.setAlignment(Element.ALIGN_LEFT);
        document.add(note);
    }

    private static void addBorderedCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderWidth(0.8f);
        table.addCell(cell);
    }

    private static void addBorderedCellWithBg(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderWidth(0.8f);
        table.addCell(cell);
    }

    private static void addNoBorderCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return MONEY_FORMAT.format(amount);
    }
}
