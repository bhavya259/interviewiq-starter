package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * Single response object for the dashboard endpoints.
 *
 *   GET /dashboard       -> uses only { totalInterviews, averageScore }
 *                           (built via the DashboardResponse.stats(...) helper).
 *   GET /api/dashboard   -> uses the FULL payload below.
 *
 * Inner static classes keep all dashboard-related DTOs in one file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long   totalInterviews;
    private double averageScore;            // double so /dashboard keeps decimal precision
    private int    bestScore;
    private String totalPracticeTime;       // e.g. "8h 45m"

    private List<TrendPoint>     scoreTrend;
    private List<NamedValue>     weakAreas;
    private List<RecentInterview> recentInterviews;
    private List<NamedValue>     strengths;

    /** Convenience factory for the simple /dashboard endpoint. */
    public static DashboardResponse stats(long totalInterviews, double averageScore) {
        return DashboardResponse.builder()
                .totalInterviews(totalInterviews)
                .averageScore(averageScore)
                .build();
    }

    /* ---------- Inner DTOs ---------- */

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TrendPoint {
        private String date;    // ISO yyyy-MM-dd
        private int    score;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class NamedValue {
        private String name;
        private int    value;   // 0-100
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentInterview {
        private String role;
        private String level;
        private String date;
        private int    score;
        private String status;
    }
}

