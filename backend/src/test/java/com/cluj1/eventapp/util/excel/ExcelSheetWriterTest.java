package com.cluj1.eventapp.util.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelSheetWriterTest {

    private record Row(String name, Integer count, Boolean active, LocalDateTime when, Object nothing) {
    }

    @Test
    void getInstance_returnsSingleton() {
        assertThat(ExcelSheetWriter.getInstance()).isSameAs(ExcelSheetWriter.getInstance());
    }

    @Test
    void writeSheet_writesHeadersAndAllValueTypesCorrectly() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("s");

            List<ExcelColumn<Row>> columns = List.of(
                    new ExcelColumn<>("name", Row::name),
                    new ExcelColumn<>("count", Row::count),
                    new ExcelColumn<>("active", Row::active),
                    new ExcelColumn<>("when", Row::when),
                    new ExcelColumn<>("nothing", Row::nothing));

            Row row = new Row("John", 42, true, LocalDateTime.of(2026, 1, 15, 10, 30), null);
            Row row2 = new Row("Jane", 0, false, null, null);

            ExcelSheetWriter.getInstance().writeSheet(workbook, sheet, columns, List.of(row, row2));

            // Header row
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("name");
            assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("nothing");

            // Row 1: String / Number / Boolean(yes) / Temporal / null(BLANK)
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("John");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(42d);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("yes");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("2026-01-15 10:30");
            assertThat(sheet.getRow(1).getCell(4).getCellType()).isEqualTo(CellType.BLANK);

            // Row 2: Boolean(no)
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("no");
            assertThat(sheet.getRow(2).getCell(3).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void writeSheet_writesOnlyHeaderRow_whenRowsEmpty() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("s");
            List<ExcelColumn<Row>> columns = List.of(new ExcelColumn<>("name", Row::name));

            ExcelSheetWriter.getInstance().writeSheet(workbook, sheet, columns, List.of());

            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("name");
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}

