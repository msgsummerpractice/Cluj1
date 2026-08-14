package com.cluj1.eventapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.UserDetails;
import com.cluj1.eventapp.model.enums.EventLocation;
import com.cluj1.eventapp.model.enums.EventStatus;
import com.cluj1.eventapp.model.enums.EventType;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.model.enums.UserLocation;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class EventRepositoryTest {

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private UserRepository userRepository;

        @Test
        void savePersistEventWithAllFields() {
                User organizer = createAndSaveUser("john.doe@msg.group", "John", "Doe", Role.MARKETING_ORGANIZER);
                OffsetDateTime start = OffsetDateTime.parse("2026-09-01T10:00:00+00:00");
                OffsetDateTime end = OffsetDateTime.parse("2026-09-01T18:00:00+00:00");

                Event event = Event.builder()
                                .name("Summer Fest")
                                .location(EventLocation.CLUJ)
                                .type(EventType.LOCAL)
                                .status(EventStatus.PUBLISHED)
                                .eventStartDate(start)
                                .eventEndTime(end)
                                .createdBy(organizer)
                                .build();

                Event saved = eventRepository.saveAndFlush(event);

                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getName()).isEqualTo("Summer Fest");
                assertThat(saved.getLocation()).isEqualTo(EventLocation.CLUJ);
                assertThat(saved.getType()).isEqualTo(EventType.LOCAL);
                assertThat(saved.getStatus()).isEqualTo(EventStatus.PUBLISHED);
                assertThat(saved.getEventStartDate()).isEqualTo(start);
                assertThat(saved.getEventEndTime()).isEqualTo(end);
        }

        @Test
        void findByIdReturnPersistedEvent() {
                User organizer = createAndSaveUser("jane.doe@msg.group", "Jane", "Doe", Role.HR_USER);

                Event saved = eventRepository.saveAndFlush(Event.builder()
                                .name("HR Summit")
                                .location(EventLocation.TIMISOARA)
                                .type(EventType.INTERNAL)
                                .status(EventStatus.DRAFT)
                                .createdBy(organizer)
                                .build());

                Event found = eventRepository.findById(saved.getId()).orElse(null);

                assertThat(found).isNotNull();
                assertThat(found.getId()).isEqualTo(saved.getId());
                assertThat(found.getName()).isEqualTo("HR Summit");
                assertThat(found.getLocation()).isEqualTo(EventLocation.TIMISOARA);
                assertThat(found.getType()).isEqualTo(EventType.INTERNAL);
                assertThat(found.getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        void findAllReturnAllSavedEvents() {
                User organizer = createAndSaveUser("alex.pop@msg.group", "Alex", "Pop", Role.ADMIN);

                eventRepository.save(Event.builder()
                                .name("Event One")
                                .location(EventLocation.CLUJ)
                                .type(EventType.LOCAL)
                                .status(EventStatus.DRAFT)
                                .createdBy(organizer)
                                .build());

                eventRepository.save(Event.builder()
                                .name("Event Two")
                                .location(EventLocation.MURES)
                                .type(EventType.EXTERNAL)
                                .status(EventStatus.PUBLISHED)
                                .createdBy(organizer)
                                .build());

                eventRepository.flush();

                List<Event> events = eventRepository.findAll();

                assertThat(events).hasSize(2);
                assertThat(events).extracting(Event::getName).containsExactlyInAnyOrder("Event One", "Event Two");
        }

        @Test
        void savePersistUpdateExistingEvent() {
                User organizer = createAndSaveUser("maria.ionescu@msg.group", "Maria", "Ionescu",
                                Role.MARKETING_ORGANIZER);

                Event saved = eventRepository.saveAndFlush(Event.builder()
                                .name("Launch Day")
                                .location(EventLocation.CLUJ)
                                .type(EventType.LOCAL)
                                .status(EventStatus.DRAFT)
                                .createdBy(organizer)
                                .build());

                UUID eventId = saved.getId();
                saved.setStatus(EventStatus.COMPLETED);
                saved.setRegistrationEndDate(OffsetDateTime.parse("2026-08-15T12:00:00+00:00"));
                eventRepository.saveAndFlush(saved);

                Event updated = eventRepository.findById(eventId).orElseThrow();

                assertThat(updated.getStatus()).isEqualTo(EventStatus.COMPLETED);
                assertThat(updated.getRegistrationEndDate())
                                .isEqualTo(OffsetDateTime.parse("2026-08-15T12:00:00+00:00"));
        }

        @Test
        void deleteByIdRemoveEvent() {
                User organizer = createAndSaveUser("mihai.popescu@msg.group", "Mihai", "Popescu",
                                Role.MARKETING_ORGANIZER);

                Event saved = eventRepository.saveAndFlush(Event.builder()
                                .name("Delete Me")
                                .location(EventLocation.TIMISOARA)
                                .type(EventType.INTERNAL)
                                .status(EventStatus.DRAFT)
                                .createdBy(organizer)
                                .build());

                eventRepository.deleteById(saved.getId());
                eventRepository.flush();

                assertThat(eventRepository.findById(saved.getId())).isEmpty();
        }

        private User createAndSaveUser(String email, String firstName, String lastName, Role role) {
                User user = User.builder()
                                .email(email)
                                .passwordHash("hash")
                                .role(role)
                                .isActive(true)
                                .build();

                UserDetails details = UserDetails.builder()
                                .user(user)
                                .firstName(firstName)
                                .lastName(lastName)
                                .location(UserLocation.CLUJ)
                                .build();
                user.setUserDetails(details);

                return userRepository.saveAndFlush(user);
        }

}
