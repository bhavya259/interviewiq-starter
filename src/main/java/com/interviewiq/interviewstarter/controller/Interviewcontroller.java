package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.InterviewDtos.*;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.service.InterviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interview")
public class Interviewcontroller {
    private final InterviewService interviewService;

    public Interviewcontroller(InterviewService interviewService){
        this.interviewService = interviewService;
    }

    @PostMapping("/create")
    public InterviewResponse create(@RequestBody InterviewRequest request){
        Interview saved = interviewService.create(request.getRole(), request.getExperienceLevel(),request.getDifficulty(), request.getDuration());
        return new InterviewResponse(true,"Interview created",saved.getId());
    }

}
