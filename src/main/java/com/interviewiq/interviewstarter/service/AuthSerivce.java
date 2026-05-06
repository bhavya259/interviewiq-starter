package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthSerivce {

    private final UserRepository userRepository;

    private AuthSerivce(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public String register(String name, String email, String password) {
        //check if email is preset or not
        if (userRepository.findByEmail(email).isPresent()) {
            return "Email already registered";
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        userRepository.save(user);
        return "User registered succesfully";
    }

    public boolean login(String email, String password){
        // 1. Looku up the user by email
        Optional<User> userOpt = userRepository.findByEmail(email);

        // 2. If user exists and password matches - > success
        return userOpt.isPresent() && userOpt.get().getPassword().equals(password);
    }

}
