package com.cluj1.eventapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.cluj1.eventapp.dto.AttendanceExportRowDto;

class ExcelExportUtilTest {

    @Test
    void getInstance_returnsSingleton() {
        assertThat(ExcelExportUtil.getInstance()).isSameAs(ExcelExportUtil.getInstance());
    }

    @Test
    void generateAttendanceReport_writesHeaderAndDataRows() throws Exception {
        AttendanceExportRowDto row = AttendanceExportRowDto.builder()
                .nrCrt(1)
                .lastName("Doe")
                .firstName("John")
                .eventName("Meetup")
                .email("john.doe@msg.group")
                .foodPreference("VEGAN")
                .transportRequired("yes")
                .accommodationRequired("no")
                .driverName("N/A")
                .driverPhoneNumber("N/A")
                .gdpr("yes")
                .build();

        byte[] bytes = ExcelExportUtil.getInstance().generateAttendanceReport(List.of(row));

        assertThat(bytes).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Attendance Report");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("nr_crt");
            assertThat(sheet.getRow(0).getCell(10).getStringCellValue()).isEqualTo("gdpr");

            assertThat((int) sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Doe");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("John");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Meetup");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("john.doe@msg.group");
            assertThat(sheet.getRow(1).getCell(10).getStringCellValue()).isEqualTo("yes");
        }
    }

    @Test
    void generateAttendanceReport_writesOnlyHeaderRow_whenDataIsEmpty() throws Exception {
        byte[] bytes = ExcelExportUtil.getInstance().generateAttendanceReport(List.of());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}

