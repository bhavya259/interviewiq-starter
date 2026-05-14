package com.interviewiq.interviewstarter.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse{
    private long totalInterviews;
    private double averageScore;
    private int bestScore;
    private String totalPracticeTime;

    private List<TrendPoint> scoreTrend;
    private List<NamedValue> weakAreas;
    private List<RecentInterviews> recentInterviews;
    private List<NamedValue>     strengths;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint{
        private String date;
        private int score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NamedValue{
        private String name;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentInterviews{
        private String role;
        private String level;
        private String date;
        private int score;
        private String status;
    }
}