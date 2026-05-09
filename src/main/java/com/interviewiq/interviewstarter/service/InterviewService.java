package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.stereotype.Service;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;

    public InterviewService(InterviewRepository interviewRepository){
        this.interviewRepository=interviewRepository;
    }

    public Interview create(String role, String experienceLevel, String difficulty, Integer duration){
        Interview interview = new Interview();
        interview.setRole(role);
        interview.setExperienceLevel(experienceLevel);
        interview.setDifficulty(difficulty);
        interview.setDuration(duration);
        return interviewRepository.save(interview);
    }
}
