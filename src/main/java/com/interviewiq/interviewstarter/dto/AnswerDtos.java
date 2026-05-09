package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerItem {
        private Long questionId;     // optional, may be null in mock flow
        private String questionText; // optional but used for relevance check
        private String answerText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswersRequest {
        private List<SubmitAnswerItem> answers;
    }

    @Data
    @AllArgsConstructor
    public static class SubmitAnswersResponse {
        private boolean success;
        private int saved;
    }


}
