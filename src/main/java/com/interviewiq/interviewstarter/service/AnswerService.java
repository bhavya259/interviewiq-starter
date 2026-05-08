package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerService {
    private final AnswerRepository answerRepository;
    public AnswerService(AnswerRepository answerRepository){
        this.answerRepository = answerRepository;
    }

    public Answer save(Long questionId, String answerText){
        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setAnswerText(answerText);
        return answerRepository.save(a);
    }

    public List<Answer> saveAll(List<Answer> answers){
        return answerRepository.saveAll(answers);
    }
}
