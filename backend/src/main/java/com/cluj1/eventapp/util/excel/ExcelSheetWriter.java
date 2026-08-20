package com.cluj1.eventapp.util.excel;

import org.apache.poi.ss.usermodel.*;

import java.time.temporal.Temporal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExcelSheetWriter {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static ExcelSheetWriter INSTANCE;

    private ExcelSheetWriter() {
    }

    public static ExcelSheetWriter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExcelSheetWriter();
        }
        return INSTANCE;
    }

    public <T> void writeSheet(
            Workbook workbook,
            Sheet sheet,
            List<ExcelColumn<T>> columns,
            List<T> rows) {
        writeHeaderRow(workbook, sheet, columns);
        writeDataRows(sheet, columns, rows);
        autoSizeColumns(sheet, columns.size());
    }

    private <T> void writeHeaderRow(Workbook workbook, Sheet sheet, List<ExcelColumn<T>> columns) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < columns.size(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(columns.get(col).header());
            cell.setCellStyle(headerStyle);
        }
    }

    private <T> void writeDataRows(Sheet sheet, List<ExcelColumn<T>> columns, List<T> rows) {
        int rowIndex = 1;
        for (T row : rows) {
            Row excelRow = sheet.createRow(rowIndex++);
            for (int col = 0; col < columns.size(); col++) {
                Object value = columns.get(col).valueExtractor().apply(row);
                setCellValue(excelRow.createCell(col), value);
            }
        }
    }

    private void setCellValue(Cell cell, Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool ? "yes" : "no");
            case Temporal temporal -> cell.setCellValue(DEFAULT_DATE_FORMAT.format(temporal));
            default -> cell.setCellValue(value.toString());
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int col = 0; col < columnCount; col++) {
            sheet.autoSizeColumn(col);
        }
    }
}
