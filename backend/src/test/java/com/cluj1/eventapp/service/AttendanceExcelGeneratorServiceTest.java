package com.cluj1.eventapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;

class AttendanceExcelGeneratorServiceTest {

    private AttendanceExcelGeneratorService generator;

    @BeforeEach
    void setUp() {
        generator = new AttendanceExcelGeneratorService();
    }

    @Test
    void generate_returnsValidXlsxWithHeaders_whenRowsProvided() throws Exception {
        AttendanceReportExcelRowDto row = AttendanceReportExcelRowDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@msg.group")
                .hasGdprConsent(true)
                .isPresent(true)
                .registrationDate(OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC))
                .build();

        byte[] result = generator.generate(List.of(row));

        assertThat(result).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Attendance Report");
            assertThat(sheet).isNotNull();

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("nr_crt");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("lastName");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("firstName");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("email");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("gdpr");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("registration_date");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("present");

            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getNumericCellValue()).isEqualTo(1d);
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("Doe");
            assertThat(data.getCell(2).getStringCellValue()).isEqualTo("John");
            assertThat(data.getCell(3).getStringCellValue()).isEqualTo("john.doe@msg.group");
            assertThat(data.getCell(4).getStringCellValue()).isEqualTo("yes");
            assertThat(data.getCell(5).getStringCellValue()).isEqualTo("2026-01-15 10:30");
            assertThat(data.getCell(6).getStringCellValue()).isEqualTo("yes");
        }
    }

    @Test
    void generate_writesSequentialNrCrt_forMultipleRows() throws Exception {
        List<AttendanceReportExcelRowDto> rows = List.of(
                AttendanceReportExcelRowDto.builder().firstName("A").lastName("A").email("a.a@msg.group")
                        .hasGdprConsent(true).isPresent(true).build(),
                AttendanceReportExcelRowDto.builder().firstName("B").lastName("B").email("b.b@msg.group")
                        .hasGdprConsent(false).isPresent(false).build(),
                AttendanceReportExcelRowDto.builder().firstName("C").lastName("C").email("c.c@msg.group")
                        .hasGdprConsent(true).isPresent(false).build());

        byte[] result = generator.generate(rows);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat((int) sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat((int) sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(2);
            assertThat((int) sheet.getRow(3).getCell(0).getNumericCellValue()).isEqualTo(3);

            // Boolean serialization
            assertThat(sheet.getRow(2).getCell(4).getStringCellValue()).isEqualTo("no");
            assertThat(sheet.getRow(2).getCell(6).getStringCellValue()).isEqualTo("no");
        }
    }

    @Test
    void generate_returnsWorkbookWithOnlyHeader_whenRowsEmpty() throws Exception {
        byte[] result = generator.generate(List.of());

        assertThat(result).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Attendance Report");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }

    @Test
    void generate_writesBlankCell_whenRegistrationDateIsNull() throws Exception {
        AttendanceReportExcelRowDto row = AttendanceReportExcelRowDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@msg.group")
                .hasGdprConsent(true)
                .isPresent(true)
                .registrationDate(null)
                .build();

        byte[] result = generator.generate(List.of(row));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(5).getCellType())
                    .isEqualTo(org.apache.poi.ss.usermodel.CellType.BLANK);
        }
    }
}

