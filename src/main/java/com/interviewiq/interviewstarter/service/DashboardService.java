package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.dto.DashboardResponse.*;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/* ============================================================
 *  DashboardService
 *  ----------------
 *  Builds the data shown on the Dashboard page.
 *  Two public methods:
 *    1) getStats()       -> tiny summary  (GET /dashboard)
 *    2) buildDashboard() -> full payload  (GET /api/dashboard)
 *  If DB has no finished interviews, demo data is returned so the
 *  UI never looks empty.
 * ============================================================ */
@Service
public class DashboardService {

    /* Date format used everywhere on the dashboard, e.g. "2026-05-11". */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /* Repositories — Spring injects them via constructor. */
    private final InterviewRepository interviewRepository;
    private final EvaluationRepository evaluationRepository;

    public DashboardService(InterviewRepository interviewRepository,
                            EvaluationRepository evaluationRepository) {
        this.interviewRepository = interviewRepository;
        this.evaluationRepository = evaluationRepository;
    }

    /* ============================================================
     *  1) SIMPLE STATS  ->  GET /dashboard
     *     Returns total interview count + average evaluation score.
     * ============================================================ */
    public DashboardResponse getStats() {
        // Total number of interview rows in the DB.
        long totalInterviews = interviewRepository.count();

        // Average of all evaluation scores (null scores treated as 0; 0 if no rows).
        double avg = evaluationRepository.findAll().stream()
                .mapToInt(e -> e.getScore() == null ? 0 : e.getScore())
                .average().orElse(0.0);

        // Round to 2 decimal places for a clean number on the UI.
        double averageScore = Math.round(avg * 100.0) / 100.0;
        return DashboardResponse.stats(totalInterviews, averageScore);
    }

    /* ============================================================
     *  2) FULL DASHBOARD  ->  GET /api/dashboard
     *     Steps: load -> keep finished -> demo if empty -> build DTO.
     * ============================================================ */
    public DashboardResponse buildDashboard() {
        // Keep only interviews that have a finalScore (i.e. actually completed).
        List<Interview> finished = interviewRepository.findAll().stream()
                .filter(iv -> iv.getFinalScore() != null)
                .toList();

        // Nothing finished yet? Show demo data so the dashboard isn't blank.
        if (finished.isEmpty()) return demoDashboard();

        // Headline numbers.
        int avg  = (int) Math.round(finished.stream().mapToInt(Interview::getFinalScore).average().orElse(0));
        int best = finished.stream().mapToInt(Interview::getFinalScore).max().orElse(0);
        int totalMinutes = finished.stream()
                .mapToInt(iv -> iv.getDuration() == null ? 0 : iv.getDuration()).sum();

        // Assemble the response.
        return DashboardResponse.builder()
                .totalInterviews((long) finished.size())
                .averageScore(avg)
                .bestScore(best)
                .totalPracticeTime(formatMinutes(totalMinutes))   // e.g. "8h 45m"
                .scoreTrend(buildTrend(finished))                 // last 7 days
                .weakAreas(buildWeakAreas(finished))              // top 4 weakest roles
                .recentInterviews(buildRecent(finished))          // newest 5
                .strengths(buildStrengths(avg))                   // 4 fixed labels
                .build();
    }

    /* -------------------- small helpers -------------------- */

    /** Format minutes as "Xh Ym" / "Ym" / "0m". */
    private String formatMinutes(int total) {
        if (total <= 0) return "0m";
        int h = total / 60, m = total % 60;
        return h == 0 ? m + "m" : h + "h " + m + "m";
    }

    /** Clamp a value into the 0..100 range (used by strength bars). */
    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    /* ============================================================
     *  TREND — average score per day for the last 7 days (oldest first).
     * ============================================================ */
    private List<TrendPoint> buildTrend(List<Interview> finished) {
        LocalDate today = LocalDate.now();

        // Group scores by completion date (fallback = today if null).
        Map<LocalDate, List<Integer>> byDay = finished.stream().collect(Collectors.groupingBy(
                iv -> iv.getCompletedAt() == null ? today : iv.getCompletedAt().toLocalDate(),
                Collectors.mapping(Interview::getFinalScore, Collectors.toList())
        ));

        // Walk last 7 days oldest -> today, compute daily average (0 if no data).
        return IntStream.rangeClosed(0, 6).boxed()
                .sorted(Comparator.reverseOrder())                // 6,5,4,...,0
                .map(offset -> {
                    LocalDate d = today.minusDays(offset);
                    List<Integer> scores = byDay.getOrDefault(d, List.of());
                    int dayAvg = scores.isEmpty() ? 0
                            : (int) scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                    return new TrendPoint(d.format(DATE_FMT), dayAvg);
                })
                .toList();
    }

    /* ============================================================
     *  WEAK AREAS — group scores by role, weakness = (100 − avg).
     *  Floor weakness at 5 so bar is always visible. Return top 4.
     * ============================================================ */
    private List<NamedValue> buildWeakAreas(List<Interview> finished) {
        // role -> list of scores
        Map<String, List<Integer>> byRole = finished.stream().collect(Collectors.groupingBy(
                iv -> iv.getRole() == null ? "General" : iv.getRole(),
                Collectors.mapping(Interview::getFinalScore, Collectors.toList())
        ));

        // Convert to NamedValue(role, weakness), sort weakest first, keep top 4.
        return byRole.entrySet().stream()
                .map(e -> {
                    int roleAvg = (int) e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                    return new NamedValue(e.getKey(), Math.max(5, 100 - roleAvg));
                })
                .sorted(Comparator.comparingInt(NamedValue::getValue).reversed())
                .limit(4)
                .toList();
    }

    /* ============================================================
     *  RECENT — newest 5 interviews (sorted by id desc) as DTOs.
     *  Status text from score band: >=70 Completed, >=40 Needs Review, else Practiced.
     * ============================================================ */
    private List<RecentInterview> buildRecent(List<Interview> finished) {
        return finished.stream()
                .sorted(Comparator.comparingLong(Interview::getId).reversed()) // newest first
                .limit(5)
                .map(iv -> {
                    int score = iv.getFinalScore();
                    String status = score >= 70 ? "Completed"
                                  : score >= 40 ? "Needs Review"
                                  : "Practiced";
                    LocalDate when = iv.getCompletedAt() != null
                            ? iv.getCompletedAt().toLocalDate() : LocalDate.now();
                    return RecentInterview.builder()
                            .role(iv.getRole() == null ? "General" : iv.getRole())
                            .level(iv.getDifficulty() == null ? "Medium" : iv.getDifficulty())
                            .date(when.format(DATE_FMT))
                            .score(score)
                            .status(status)
                            .build();
                })
                .toList();
    }

    /* ============================================================
     *  STRENGTHS — 4 hard-coded labels nudged around the user's avg
     *  (no per-skill data yet; this just personalizes the bars).
     * ============================================================ */
    private List<NamedValue> buildStrengths(int avgScore) {
        int base = Math.max(40, avgScore); // never look embarrassingly empty
        return List.of(
                new NamedValue("Problem Solving", clamp(base + 5)),
                new NamedValue("Communication",   clamp(base - 5)),
                new NamedValue("Confidence",      clamp(base - 10)),
                new NamedValue("Technical Depth", clamp(base))
        );
    }

    /* ============================================================
     *  DEMO DATA — shown when no finished interviews exist yet.
     * ============================================================ */
    private DashboardResponse demoDashboard() {
        LocalDate today = LocalDate.now();
        int[] sample = {60, 65, 72, 68, 80, 85, 78};

        // Build 7-day fake trend (oldest -> today).
        List<TrendPoint> trend = IntStream.range(0, 7)
                .mapToObj(i -> new TrendPoint(today.minusDays(6 - i).format(DATE_FMT), sample[i]))
                .toList();

        return DashboardResponse.builder()
                .totalInterviews(12L)
                .averageScore(78)
                .bestScore(92)
                .totalPracticeTime("8h 45m")
                .scoreTrend(trend)
                .weakAreas(List.of(
                        new NamedValue("DSA", 40),
                        new NamedValue("System Design", 25),
                        new NamedValue("API", 20),
                        new NamedValue("Others", 15)))
                .recentInterviews(List.of(
                        demoRow("Backend Developer",  "Medium", today.minusDays(2),  85, "Completed"),
                        demoRow("Frontend Developer", "Easy",   today.minusDays(4),  78, "Completed"),
                        demoRow("Java Developer",     "Hard",   today.minusDays(7),  62, "Needs Review"),
                        demoRow("HR Interview",       "Easy",   today.minusDays(10), 92, "Completed"),
                        demoRow("DevOps Engineer",    "Medium", today.minusDays(13), 71, "Practiced")))
                .strengths(List.of(
                        new NamedValue("Problem Solving", 80),
                        new NamedValue("Communication", 70),
                        new NamedValue("Confidence", 65),
                        new NamedValue("Technical Depth", 74)))
                .build();
    }

    /** Tiny factory to keep demoDashboard() readable. */
    private RecentInterview demoRow(String role, String level, LocalDate date, int score, String status) {
        return RecentInterview.builder()
                .role(role).level(level)
                .date(date.format(DATE_FMT))
                .score(score).status(status)
                .build();
    }
}
