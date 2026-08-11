package com.cluj1.eventapp.service;


import com.cluj1.eventapp.dto.UserRegistrationDto;
import com.cluj1.eventapp.exception.EmailAlreadyRegisteredException;
import com.cluj1.eventapp.mapper.DtoMapper;
import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoMapper mapper;

    public void registerUser(UserRegistrationDto registrationDto){
        if(userRepository.existsByEmail(registrationDto.getEmail())){
            throw new EmailAlreadyRegisteredException();
        }
        User user = mapper.mapToEntity(registrationDto);
        userRepository.save(user);
    }


}
