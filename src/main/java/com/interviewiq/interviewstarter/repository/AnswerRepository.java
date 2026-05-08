package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AnswerRepository  extends JpaRepository<Interview,Long> {
}