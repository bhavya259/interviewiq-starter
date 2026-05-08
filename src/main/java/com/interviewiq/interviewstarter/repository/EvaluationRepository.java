package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EvaluationRepository  extends JpaRepository<Evaluation,Long> {
}