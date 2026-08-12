package com.cluj1.eventapp.config;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;
import com.cluj1.eventapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {

            User participant = User.builder()
                    .email("user@msg.group")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(Role.PARTICIPANT)
                    .isActive(true)
                    .build();
            userRepository.save(participant);

            User marketing = User.builder()
                    .email("marketing.user@msg.group")
                    .passwordHash(passwordEncoder.encode("marketing123"))
                    .role(Role.MARKETING_ORGANIZER)
                    .isActive(true)
                    .build();
            userRepository.save(marketing);

            User hr = User.builder()
                    .email("hr.user@msg.group")
                    .passwordHash(passwordEncoder.encode("hr123"))
                    .role(Role.HR_USER)
                    .isActive(true)
                    .build();
            userRepository.save(hr);

            User admin = User.builder()
                    .email("admin.test@msg.group")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);

            System.out.println(">>> DataInitializer: Utilizatorii de test au fost creați cu succes!");
        }
    }
}