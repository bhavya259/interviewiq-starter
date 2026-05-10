package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class Evaluationsdtos
{
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluateAnswersRequest{
        private List<AnswerDtos.SubmitAnswerItem> answers;
    }

    @Data
    @AllArgsConstructor
    public static class EvaluateAnswersResponse{
        private boolean success;
        private int score;
        private int fillerWords;
        private int confidence;
        private String relevences;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
    }
}
