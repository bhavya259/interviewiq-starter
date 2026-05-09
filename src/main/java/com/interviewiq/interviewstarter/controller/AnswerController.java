package com.interviewiq.interviewstarter.controller;


import com.interviewiq.interviewstarter.dto.AnswerDtos.*;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.service.AnswerService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/answers")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping
    public SubmitAnswersResponse submitAnswers(@RequestBody SubmitAnswersRequest request){
        List<Answer> toSave = new ArrayList<>();
        if (request.getAnswers() != null) {
            for (SubmitAnswerItem item : request.getAnswers()) {
                Answer a = new Answer();
                a.setQuestionId(item.getQuestionId());
                a.setAnswerText(item.getAnswerText());
                toSave.add(a);
            }
        }
        List<Answer> saved = answerService.saveAll(toSave);
        return new SubmitAnswersResponse(true, saved.size());
    }

}
