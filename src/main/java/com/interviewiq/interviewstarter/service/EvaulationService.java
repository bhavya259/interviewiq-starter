package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.repository.EvaluationRepository;

import java.util.ArrayList;
import java.util.List;

public class EvaulationService {

    private final EvaluationRepository evaluationRepository;
    private final AIService aiService;

    public EvaulationService(EvaluationRepository evaluationRepository, AIService aiService){
        this.evaluationRepository = evaluationRepository;
        this.aiService = aiService;
    }

    public static class OverallResult{
        public int score;
        public int fillerWords;
        public String confidence;
        public String relevences;
        public List<String> strengths = new ArrayList<>();
        public List<String> weaknesses = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();

    }

    public static class QA{
        public String question;
        public String answer;
        public Long answerId;

    }

    public OverallResult evaluateAnswerPairs(List<QA> pairs){
        if(pairs==null || pairs.isEmpty()){
            OverallResult r = new OverallResult();
            r.score = 0;

        }

        List<AIService.AIEvaluation> aiResults = new ArrayList<>();
        boolean aiOk = true;
        for(QA p : pairs) {
            AIService.AIEvaluation ev = aiService.evaluateWithAI(p.question == null ? "(no question)" : p.question, p.answer == null ? "" : p.answer);

            if (ev == null){
                aiOk = false;
                break;
            }
            aiResults.add(ev);
        }
        return null;
    }
}
