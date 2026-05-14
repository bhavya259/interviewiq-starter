package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public Interview finish(Long id, Integer finalScore){
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found: " + id));
        interview.setFinalScore(finalScore);
        interview.setCompletedAt(LocalDateTime.now());
        return interviewRepository.save(interview);
    }
}
