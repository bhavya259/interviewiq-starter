package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AIService aiService;

    private static final List<String> FALLBACK_QUESTIONS = List.of(
            "Tell me about yourself",
            "where do you see yourself in 5 years"
    );

    public QuestionService(QuestionRepository questionRepository, InterviewRepository interviewRepository, AIService aiService) {
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.aiService = aiService;

    }

    public List<Question> getQuestionsForInterview(Long interviewId){
        //1. cached?
        List<Question> existing = questionRepository.findByinterviewId(interviewId);
        if(!existing.isEmpty()){
            return existing;
        }

        //2. Need the interview to know what to ask
        Optional<Interview> opt = interviewRepository.findById(interviewId);
        if(opt.isEmpty()){
            return List.of();
        }
        Interview interview = opt.get();

        //3. Try AI, fall back to generic
        List<String> texts = aiService.generateQuestions(interview.getRole(),interview.getExperiencelevel(),interview.getDifficulty(),5);

        if(texts==null || texts.isEmpty()){
            System.out.println("Ai UNAVAILABLE - GOING TO USE FALLBACK QUESTIONS");
            texts = FALLBACK_QUESTIONS;
        }

        List<Question> toSave = new ArrayList<>();
        for(String text :texts){
            toSave.add(new Question(null, interviewId, text));
        }
        return questionRepository.saveAll(toSave);
    }
}
