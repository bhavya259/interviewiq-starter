package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question,Long> {
    List<Question> findByinterviewId(Long interviewId);
}
