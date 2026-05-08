package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<User, Long> {

    Optional<User> findByInterviewId(Long interviewId);
}
