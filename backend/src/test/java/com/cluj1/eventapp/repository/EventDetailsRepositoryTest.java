package com.cluj1.eventapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.cluj1.eventapp.model.Event;
import com.cluj1.eventapp.model.EventDetails;
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
class EventDetailsRepositoryTest {

    @Autowired
    private EventDetailsRepository eventDetailsRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savePersistEventDetailsWithAllFields() {
        User organizer = createAndSaveUser("eventrepo.details@msg.group", "Event", "Details",
                Role.MARKETING_ORGANIZER);
        Event event = createAndSaveEvent(organizer, "Event Details Save Test");

        byte[] poster = new byte[] { 1, 3, 7 };

        EventDetails saved = eventDetailsRepository.saveAndFlush(EventDetails.builder()
                .event(event)
                .description("Detailed repository test description")
                .poster(poster)
                .foodProvided(true)
                .qrCodeContent("repo-qr-content")
                .eventCode("RP45QZ")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEvent()).isNotNull();
        assertThat(saved.getEvent().getId()).isEqualTo(event.getId());
        assertThat(saved.getDescription()).isEqualTo("Detailed repository test description");
        assertThat(saved.getPoster()).containsExactly(1, 3, 7);
        assertThat(saved.getFoodProvided()).isTrue();
        assertThat(saved.getQrCodeContent()).isEqualTo("repo-qr-content");
        assertThat(saved.getEventCode()).isEqualTo("RP45QZ");
    }

    @Test
    void findByIdReturnPersistedEventDetails() {
        User organizer = createAndSaveUser("eventfind.case@msg.group", "Find", "Case", Role.HR_USER);
        Event event = createAndSaveEvent(organizer, "Event Details Find Test");

        EventDetails saved = eventDetailsRepository.saveAndFlush(EventDetails.builder()
                .event(event)
                .description("Find me")
                .foodProvided(false)
                .qrCodeContent("find-qr")
                .eventCode("FD88TR")
                .build());

        EventDetails found = eventDetailsRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getDescription()).isEqualTo("Find me");
        assertThat(found.getFoodProvided()).isFalse();
        assertThat(found.getQrCodeContent()).isEqualTo("find-qr");
        assertThat(found.getEventCode()).isEqualTo("FD88TR");
    }

    @Test
    void findAllReturnAllSavedEventDetails() {
        User organizer = createAndSaveUser("eventall.case@msg.group", "All", "Case", Role.ADMIN);

        Event firstEvent = createAndSaveEvent(organizer, "Event Details First");
        Event secondEvent = createAndSaveEvent(organizer, "Event Details Second");

        eventDetailsRepository.save(EventDetails.builder()
                .event(firstEvent)
                .description("First details")
                .foodProvided(true)
                .eventCode("AA11BB")
                .build());

        eventDetailsRepository.save(EventDetails.builder()
                .event(secondEvent)
                .description("Second details")
                .foodProvided(false)
                .eventCode("CC22DD")
                .build());

        eventDetailsRepository.flush();

        List<EventDetails> all = eventDetailsRepository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(EventDetails::getDescription)
                .containsExactlyInAnyOrder("First details", "Second details");
    }

    @Test
    void deleteByIdRemoveEventDetails() {
        User organizer = createAndSaveUser("eventdelete.case@msg.group", "Delete", "Case",
                Role.MARKETING_ORGANIZER);
        Event event = createAndSaveEvent(organizer, "Event Details Delete Test");

        EventDetails saved = eventDetailsRepository.saveAndFlush(EventDetails.builder()
                .event(event)
                .description("Delete this")
                .foodProvided(true)
                .eventCode("DL33TY")
                .build());

        eventDetailsRepository.deleteById(saved.getId());
        eventDetailsRepository.flush();

        assertThat(eventDetailsRepository.findById(saved.getId())).isEmpty();
    }

    private Event createAndSaveEvent(User organizer, String name) {
        return eventRepository.saveAndFlush(Event.builder()
                .name(name)
                .location(EventLocation.CLUJ)
                .type(EventType.LOCAL)
                .status(EventStatus.DRAFT)
                .createdBy(organizer)
                .build());
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