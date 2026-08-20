package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.AttendanceReportExcelRowDto;
import com.cluj1.eventapp.util.excel.ExcelColumn;
import com.cluj1.eventapp.util.excel.ExcelSheetWriter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AttendanceExcelGeneratorService {

    public byte[] generate(List<AttendanceReportExcelRowDto> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance Report");
            ExcelSheetWriter.getInstance().writeSheet(workbook, sheet, columns(), rows);
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to generate attendance report Excel file", exception);
        }
    }

    private List<ExcelColumn<AttendanceReportExcelRowDto>> columns() {
        AtomicInteger sequence = new AtomicInteger(1);

        return List.of(
                new ExcelColumn<>("nr_crt", row -> sequence.getAndIncrement()),
                new ExcelColumn<>("lastName", AttendanceReportExcelRowDto::getLastName),
                new ExcelColumn<>("firstName", AttendanceReportExcelRowDto::getFirstName),
                new ExcelColumn<>("email", AttendanceReportExcelRowDto::getEmail),
                new ExcelColumn<>("gdpr", AttendanceReportExcelRowDto::isHasGdprConsent),
                new ExcelColumn<>("registration_date", AttendanceReportExcelRowDto::getRegistrationDate),
                new ExcelColumn<>("present", AttendanceReportExcelRowDto::isPresent));
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
