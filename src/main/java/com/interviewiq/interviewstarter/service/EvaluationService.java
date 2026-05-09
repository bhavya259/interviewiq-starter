package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/*
 * EvaluationService — AI does everything; we just average + save.
 *
 *   1. Ask Gemini to evaluate each Q&A (score, relevance, fillers, feedback).
 *   2. Average the per-answer scores.
 *   3. Collect strengths / weaknesses / recommendations from AI.
 *   4. Save one Evaluation row per answer for history.
 */
@Service
public class EvaluationService {

    private final AIService aiService;
    private final EvaluationRepository evaluationRepository;

    public EvaluationService(AIService aiService, EvaluationRepository evaluationRepository) {
        this.aiService = aiService;
        this.evaluationRepository = evaluationRepository;
    }

    /* Inputs from the controller. */
    public static class QA {
        public final String questionText, answerText;
        public final Long   answerId;
        public QA(String q, String a, Long id) {
            this.questionText = q == null ? "" : q;
            this.answerText   = a == null ? "" : a;
            this.answerId     = id;
        }
    }

    /* Output sent back to the UI. */
    public static class OverallResult {
        public int score, fillerWords, confidence;
        public String relevance = "medium";
        public List<String> strengths       = new ArrayList<>();
        public List<String> weaknesses      = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
    }

    public OverallResult evaluateAnswerPairs(List<QA> pairs) {
        OverallResult r = new OverallResult();
        if (pairs == null || pairs.isEmpty()) {
            r.weaknesses.add("No answers were provided.");
            r.recommendations.add("Complete the interview before evaluating.");
            return r;
        }

        int total = 0;
        for (QA qa : pairs) {
            // Ask AI; fall back to a neutral 50 if the AI is unavailable.
            AIService.AIEvaluation ev = aiService.evaluateWithAI(qa.questionText, qa.answerText);
            int score = (ev != null) ? ev.score : 50;
            total += score;

            if (ev != null) {
                if (ev.strengths       != null) r.strengths.addAll(ev.strengths);
                if (ev.weaknesses      != null) r.weaknesses.addAll(ev.weaknesses);
                if (ev.recommendations != null) r.recommendations.addAll(ev.recommendations);
                if (ev.relevance != null && !ev.relevance.isBlank()) r.relevance = ev.relevance;
                r.fillerWords += ev.fillerWords;   // AI counted these for us
            }

            // Persist for dashboard/history.
            if (qa.answerId != null) {
                evaluationRepository.save(new Evaluation(
                        null, qa.answerId, score,
                        ev != null ? "AI score " + score : "Fallback score 50"));
            }
        }

        r.score      = total / pairs.size();
        r.confidence = r.score;
        return r;
    }
}
