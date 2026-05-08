package com.interviewiq.interviewstarter.controller;


import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.service.InterviewService;
import com.interviewiq.interviewstarter.service.QuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService){
        this.questionService = questionService;
    }

    @GetMapping
    public List<Question> getQuestions(@PathVariable Long interviewId){
//        return questionService.get
    }

}
