package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class InterviewDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewRequest{
        private String role;
        private String experienceLevel;
        private String difficulty;
        private Integer duration;
    }

    @Data
    @AllArgsConstructor
    public static class InterviewResponse{
        private boolean success;
        private String message;
        private Long interviewId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinishInterviewRequest {
        private Integer finalScore;
    }
}
