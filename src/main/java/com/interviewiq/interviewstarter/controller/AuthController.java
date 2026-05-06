package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.AuthDtos.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.interviewiq.interviewstarter.service.AuthSerivce;

@RestController
@RequestMapping("/auth")
public class AuthController {

        private final AuthSerivce authservice;

        public AuthController(AuthSerivce authService){
            this.authservice = authService;
        }

        @PostMapping("/register")
        public ApiResponse register(@RequestBody RegisterRequest request){
                String message = authservice.register(request.getName(), request.getEmail(), request.getPassword());
                boolean success = message.equals("User registered successfully");
                return new ApiResponse(success, message);
        }

        @PostMapping("/login")
        public ApiResponse login(@RequestBody LoginRequest request){
                boolean ok = authservice.login(request.getEmail(), request.getPassword());
                if(ok){
                        return new ApiResponse(true, "Login successful");
                }else{
                        return new ApiResponse(false, "Invalid email or password");
                }
        }

}
