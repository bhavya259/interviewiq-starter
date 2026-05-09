package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/*
 * QuestionService — used by the PERSISTENT flow (GET /questions/{interviewId}).
 *
 *   1. If questions for this interview already exist in DB, return them.
 *   2. Otherwise, ask the AI (Gemini) to generate a fresh batch.
 *   3. If the AI is unavailable (no key, network error, ...), fall back
 *      to a small generic question list so the interview still works.
 *   4. Save the generated questions linked to the interviewId so next time
 *      we just read from DB (step 1).
 *
 * NOTE: InterviewService.generateInterview() also calls AIService for
 * questions, but that one is STATELESS — it's used by POST /generate-interview
 * which just returns strings without saving anything. Same AI call, different
 * use case (persisted vs. throwaway).
 */
@Service
public class QuestionService {

    private static final int DEFAULT_QUESTION_COUNT = 5;

    /** Generic, role-agnostic fallback used when the AI call fails. */
    private static final List<String> FALLBACK_QUESTIONS = List.of(
            "Tell me about yourself and your background.",
            "Describe a challenging project you worked on recently.",
            "What are your biggest strengths and weaknesses?",
            "Why are you interested in this role?",
            "Where do you see yourself in 5 years?"
    );

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final com.interviewiq.interviewstarter.service.AIService aiService;

    public QuestionService(QuestionRepository questionRepository,
                           InterviewRepository interviewRepository,
                           com.interviewiq.interviewstarter.service.AIService aiService) {
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.aiService = aiService;
    }

    public List<Question> getQuestionsForInterview(Long interviewId) {
        // 1. Already generated? return cached rows.
        List<Question> existing = questionRepository.findByinterviewId(interviewId);
        if (!existing.isEmpty()) return existing;

        // 2. Need the interview to know role/experience/difficulty.
        Optional<Interview> opt = interviewRepository.findById(interviewId);
        if (opt.isEmpty()) return List.of();   // controller treats empty as "not found"
        Interview interview = opt.get();

        // 3. Try AI; fall back to generic questions if it fails.
        List<String> texts = aiService.generateQuestions(
                interview.getRole(),
                interview.getExperienceLevel(),
                interview.getDifficulty(),
                DEFAULT_QUESTION_COUNT
        );
        if (texts == null || texts.isEmpty()) {
            System.out.println("[QuestionService] AI unavailable — using fallback questions.");
            texts = FALLBACK_QUESTIONS;
        }

        // 4. Persist linked to this interview.
        List<Question> toSave = new ArrayList<>();
        for (String text : texts) {
            toSave.add(new Question(null, interviewId, text));
        }
        return questionRepository.saveAll(toSave);
    }
}
