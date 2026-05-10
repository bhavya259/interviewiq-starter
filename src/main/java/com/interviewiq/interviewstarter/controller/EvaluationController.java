package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.AnswerDtos;
import com.interviewiq.interviewstarter.dto.Evaluationsdtos;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.service.AnswerService;
import com.interviewiq.interviewstarter.service.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class EvaluationController {

    private final AnswerService answerService;
    private final EvaluationService evaluationService;

    public EvaluationController(AnswerService answerService, EvaluationService evaluationService) {
        this.answerService = answerService;
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate-answer")
    public Evaluationsdtos.EvaluateAnswersResponse evaluate(@RequestBody Evaluationsdtos.EvaluateAnswersRequest req){
        List<EvaluationService.QA> pairs = new ArrayList<>();

        if(req.getAnswers()!=null){
            List<Answer> toSave = new ArrayList<>();
            for(AnswerDtos.SubmitAnswerItem item: req.getAnswers()){
                Answer a = new Answer();
                a.setAnswerText(item.getAnswerText());
                a.setQuestionId(item.getQuestionId());
                toSave.add(a);
            }
            List<Answer> saved = answerService.saveAll(toSave);

            for(int i=0;i<req.getAnswers().size();i++){
                AnswerDtos.SubmitAnswerItem item = req.getAnswers().get(i);
                Long answerid = saved.get(i).getId();
                pairs.add(new EvaluationService.QA(item.getQuestionText(),item.getAnswerText(),answerid));
            }
        }

        EvaluationService.OverallResult r = evaluationService.evaluateAnswerPairs(pairs);

        return new Evaluationsdtos.EvaluateAnswersResponse(true,r.score,r.fillerWords,r.confidence,r.relevance,r.strengths,r.weaknesses,r.recommendations);

    }
}
