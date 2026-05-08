package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AnswerDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerRequest{
        private Long questionId;
        private String answerText;
    }


    @Data
    @AllArgsConstructor
    public static class AnswerResponse{
        private boolean success;
        private String message;
        private Long answerId;
    }

}
