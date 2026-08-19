package com.cluj1.eventapp.service;

import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.TransportationDetails;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventExportService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public byte[] exportEventRegistrationsToExcel(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() == EventStatus.DRAFT) {
            throw new InvalidEventOperationException("Cannot export data for events in DRAFT status.");
        }

        List<Registration> registrations = registrationRepository.findAllByEventIdWithDetails(eventId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Report");

            String[] columns = {
                    "nr_crt", "lastName", "firstName", "eventName", "email",
                    "foodPreference", "transportRequiered", "accomodationRequired",
                    "driverName", "driverPhoneNumber", "gdpr"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (Registration reg : registrations) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(reg.getUser().getUserDetails().getLastName());
                row.createCell(2).setCellValue(reg.getUser().getUserDetails().getFirstName());
                row.createCell(3).setCellValue(event.getName());
                row.createCell(4).setCellValue(reg.getUser().getEmail());

                String foodPref = reg.getFoodPreference() != null ? reg.getFoodPreference().name() : "";
                row.createCell(5).setCellValue(foodPref);

                boolean transportNeeded = Boolean.TRUE.equals(reg.getTransportationNeeded());
                row.createCell(6).setCellValue(transportNeeded ? "yes" : "no");

                boolean accommNeeded = Boolean.TRUE.equals(reg.getAccommodationNeeded());
                String accommStr = accommNeeded ? "yes"
                        + (reg.getAccommodationDays() != null ? " (" + reg.getAccommodationDays() + " days)" : "")
                        : "no";
                row.createCell(7).setCellValue(accommStr);

                TransportationDetails td = reg.getTransportationDetails();
                if (transportNeeded && td != null) {
                    row.createCell(8).setCellValue(td.getDriverName() != null ? td.getDriverName() : "");
                    row.createCell(9).setCellValue(td.getDriverPhoneNumber() != null ? td.getDriverPhoneNumber() : "");
                } else {
                    row.createCell(8).setCellValue("");
                    row.createCell(9).setCellValue("");
                }

                row.createCell(10).setCellValue(Boolean.TRUE.equals(reg.getGdprConsent()) ? "yes" : "no");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}