package com.cluj1.eventapp.util;

import com.cluj1.eventapp.dto.AttendanceExportRowDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelExportUtil {

    private static volatile ExcelExportUtil INSTANCE;

    private static final String[] ATTENDANCE_COLUMNS = {
            "nr_crt", "lastName", "firstName", "eventName", "email",
            "foodPreference", "transportRequiered", "accomodationRequired",
            "driverName", "driverPhoneNumber", "gdpr"
    };

    private ExcelExportUtil() {
        // Prevent external instantiation
    }

    public static ExcelExportUtil getInstance() {
        if (INSTANCE == null) {
            synchronized (ExcelExportUtil.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ExcelExportUtil();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Generates an Excel workbook containing the attendance report.
     *
     * @param data the list of row data to be written into the Excel sheet
     * @return a byte array representing the generated Excel file content
     * @throws RuntimeException if an I/O error occurs during workbook generation
     */
    public byte[] generateAttendanceReport(List<AttendanceExportRowDto> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Report");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ATTENDANCE_COLUMNS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ATTENDANCE_COLUMNS[i]);
            }

            int rowIdx = 1;
            for (AttendanceExportRowDto rowData : data) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(rowData.getNrCrt());
                row.createCell(1).setCellValue(rowData.getLastName());
                row.createCell(2).setCellValue(rowData.getFirstName());
                row.createCell(3).setCellValue(rowData.getEventName());
                row.createCell(4).setCellValue(rowData.getEmail());
                row.createCell(5).setCellValue(rowData.getFoodPreference());
                row.createCell(6).setCellValue(rowData.getTransportRequired());
                row.createCell(7).setCellValue(rowData.getAccommodationRequired());
                row.createCell(8).setCellValue(rowData.getDriverName());
                row.createCell(9).setCellValue(rowData.getDriverPhoneNumber());
                row.createCell(10).setCellValue(rowData.getGdpr());
            }

            for (int i = 0; i < ATTENDANCE_COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}