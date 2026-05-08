package com.interviewiq.interviewstarter.controller;


import com.interviewiq.interviewstarter.dto.AnswerDtos.*;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.service.AnswerService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/answers")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping
    public AnswerResponse getQuestions(@RequestBody AnswerRequest request){
        Answer answer = answerService.save(request.getQuestionId(),request.getAnswerText());
        return new AnswerResponse(true, "Answer saved", answer.getId());
    }

}
