package com.cluj1.eventapp.service;

import com.cluj1.eventapp.dto.AttendanceExportRowDto;
import com.cluj1.eventapp.exception.InvalidEventOperationException;
import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.Registration;
import com.cluj1.eventapp.model.TransportationDetails;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.repository.EventRepository;
import com.cluj1.eventapp.repository.RegistrationRepository;
import com.cluj1.eventapp.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventExportService {

        private final EventRepository eventRepository;
        private final RegistrationRepository registrationRepository;

        /**
         * Retrieves event registration data and triggers the generation of an Excel
         * attendance report.
         *
         * @param eventId the unique identifier of the event
         * @return a byte array representing the Excel file
         * @throws IllegalArgumentException       if the event cannot be found
         * @throws InvalidEventOperationException if the event is in DRAFT status
         */
        public byte[] exportEventRegistrationsToExcel(UUID eventId) {
                Event event = eventRepository.findById(eventId)
                                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

                if (event.getStatus() == EventStatus.DRAFT) {
                        throw new InvalidEventOperationException("Cannot export data for events in DRAFT status.");
                }

                List<Registration> registrations = registrationRepository.findAllByEventIdWithDetails(eventId);
                List<AttendanceExportRowDto> exportData = new ArrayList<>();

                int counter = 1;
                for (Registration reg : registrations) {
                        exportData.add(mapToExportRow(counter++, reg, event.getName()));
                }

                return ExcelExportUtil.getInstance().generateAttendanceReport(exportData);
        }

        private AttendanceExportRowDto mapToExportRow(int nrCrt, Registration reg, String eventName) {
                String foodPref = (reg.getFoodPreference() != null && !reg.getFoodPreference().name().equals("NONE"))
                                ? reg.getFoodPreference().name()
                                : "";

                boolean transportNeeded = Boolean.TRUE.equals(reg.getTransportationNeeded());
                boolean accommNeeded = Boolean.TRUE.equals(reg.getAccommodationNeeded());

                String accommStr = accommNeeded
                                ? "yes" + (reg.getAccommodationDays() != null
                                                ? " (" + reg.getAccommodationDays() + " days)"
                                                : "")
                                : "no";

                TransportationDetails td = reg.getTransportationDetails();
                String driverName = (transportNeeded && td != null && td.getDriverName() != null) ? td.getDriverName()
                                : "";
                String driverPhone = (transportNeeded && td != null && td.getDriverPhoneNumber() != null)
                                ? td.getDriverPhoneNumber()
                                : "";

                return AttendanceExportRowDto.builder()
                                .nrCrt(nrCrt)
                                .lastName(reg.getUser().getUserDetails().getLastName())
                                .firstName(reg.getUser().getUserDetails().getFirstName())
                                .eventName(eventName)
                                .email(reg.getUser().getEmail())
                                .foodPreference(foodPref)
                                .transportRequired(transportNeeded ? "yes" : "no")
                                .accommodationRequired(accommStr)
                                .driverName(driverName)
                                .driverPhoneNumber(driverPhone)
                                .gdpr(Boolean.TRUE.equals(reg.getGdprConsent()) ? "yes" : "no")
                                .build();
        }
}