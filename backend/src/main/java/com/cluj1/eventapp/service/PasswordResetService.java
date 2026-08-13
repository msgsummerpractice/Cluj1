package com.cluj1.eventapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.cluj1.eventapp.repository.PasswordResetTokenRepository;
import com.cluj1.eventapp.repository.UserRepository;
public class PasswordResetService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordResetTokenRepository tokenRepo;
}
