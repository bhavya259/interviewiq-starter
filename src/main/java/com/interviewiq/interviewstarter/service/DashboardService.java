package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardService {
    private final InterviewRepository interviewRepository;
    private final EvaluationRepository evaluationRepository;

    public DashboardService(InterviewRepository interviewRepository,EvaluationRepository evaluationRepository){
        this.interviewRepository = interviewRepository;
        this.evaluationRepository = evaluationRepository;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardResponse buildDashboard()
    {
        List<Interview> finished = interviewRepository.findAll().stream().filter(interviewre -> interviewre.getFinalScore()!=null).toList();
        if(finished.isEmpty()){
            return demoDashboard();
        }

        int average = (int) Math.round(finished.stream().mapToInt(Interview::getFinalScore).average().orElse(0));
        int best = finished.stream().mapToInt(Interview::getFinalScore).max().orElse(0);
        int totalMinutes = finished.stream().mapToInt(iv ->iv.getDuration()==null? 0: iv.getDuration()).sum();

        return DashboardResponse.builder()
                .totalInterviews(finished.size())
                .averageScore(average)
                .totalPracticeTime(formatMin(totalMinutes))
                .bestScore(best)
                .scoreTrend(buildingTrend(finished))
                .weakAreas(buildWeakAreas(finished))
                .recentinterviews(buildRecent(finished))
                .strengths(buildStrengths(average))
                .build();
    }

    private String formatMin(int totalMinutes){
        if(totalMinutes<=0){
            return "0m";
        }
        int h = totalMinutes/60, m = totalMinutes%60;
        return h==0? m+"m":h+"h"+m+"m";
    }
    private List<DashboardResponse.TrendPoint> buildingTrend(List<Interview> finished){
        LocalDate today = LocalDate.now();

        Map<LocalDate,List<Integer>> byDay = finished.stream().collect(Collectors.groupingBy(
                iv -> iv.getCompletedAt() ==null? today:iv.getCompletedAt().toLocalDate(),
                Collectors.mapping(Interview::getFinalScore,Collectors.toList())
        ));

        return IntStream.rangeClosed(0,6).boxed()
                .sorted(Comparator.reverseOrder())
                .map(offset -> {
                    LocalDate d = today.minusDays(offset);
                    List<Integer> scores = byDay.getOrDefault(d,List.of());
                    int dayAverage = scores.isEmpty()? 0 :
                            (int) scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                    return new DashboardResponse.TrendPoint(d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) , dayAverage);
                })
                .toList();

    }


    public List<DashboardResponse.NamedValue> buildWeakAreas(List<Interview> finished){

        Map<String, List<Integer>> byRole = finished.stream().collect(Collectors.groupingBy(
                iv -> iv.getRole() == null ? "Generalrole" : iv.getRole(),Collectors.mapping(Interview::getFinalScore,Collectors.toList())
        ));

        return byRole.entrySet().stream()
                .map(e -> {
                    int roleAvg = (int) e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                    return new DashboardResponse.NamedValue(e.getKey(), Math.max(5,100 - roleAvg));
                })
                .sorted(Comparator.comparingInt(DashboardResponse.NamedValue::getValue).reversed())
                .limit(4)
                .toList();

    }

    private List<DashboardResponse.RecentInterviews> buildRecent(List<Interview> finished){
        return finished.stream()
                .sorted(Comparator.comparingLong(Interview::getId).reversed())
                .limit(5)
                .map(iv->{
                            int score = iv.getFinalScore();
                            String status = score>=70? "Completed"
                                    :score>=40? "Needs Review"
                                    :"Practiced";
                            LocalDate when = iv.getCompletedAt()!=null
                                    ?iv.getCompletedAt().toLocalDate():LocalDate.now();

                            return DashboardResponse.RecentInterviews.builder()
                                    .role(iv.getRole()==null ?"GeneralRole":iv.getRole())
                                    .level(iv.getDifficulty()==null? "Medium": iv.getDifficulty())
                                    .date(when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                                    .score(score)
                                    .status(status)
                                    .build();
                        }
                        ).toList();
    }


    private List<DashboardResponse.NamedValue> buildStrengths(int avgScore) {
        int base = Math.max(40, avgScore); // never look embarrassingly empty
        return List.of(
                new DashboardResponse.NamedValue("Problem Solving", clamp(base + 5)),
                new DashboardResponse.NamedValue("Communication",   clamp(base - 5)),
                new DashboardResponse.NamedValue("Confidence",      clamp(base - 10)),
                new DashboardResponse.NamedValue("Technical Depth", clamp(base))
        );
    }


    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }



    private DashboardResponse demoDashboard() {
        LocalDate today = LocalDate.now();
        int[] sample = {60, 65, 72, 68, 80, 85, 78};

        // Build 7-day fake trend (oldest -> today).
        List<DashboardResponse.TrendPoint> trend = IntStream.range(0, 7)
                .mapToObj(i -> new DashboardResponse.TrendPoint(today.minusDays(6 - i).format(DATE_FMT), sample[i]))
                .toList();

        return DashboardResponse.builder()
                .totalInterviews(12L)
                .averageScore(78)
                .bestScore(92)
                .totalPracticeTime("8h 45m")
                .scoreTrend(trend)
                .weakAreas(List.of(
                        new DashboardResponse.NamedValue("DSA", 40),
                        new DashboardResponse.NamedValue("System Design", 25),
                        new DashboardResponse.NamedValue("API", 20),
                        new DashboardResponse.NamedValue("Others", 15)))
                .recentinterviews(List.of(
                        demoRow("Backend Developer",  "Medium", today.minusDays(2),  85, "Completed"),
                        demoRow("Frontend Developer", "Easy",   today.minusDays(4),  78, "Completed"),
                        demoRow("Java Developer",     "Hard",   today.minusDays(7),  62, "Needs Review"),
                        demoRow("HR Interview",       "Easy",   today.minusDays(10), 92, "Completed"),
                        demoRow("DevOps Engineer",    "Medium", today.minusDays(13), 71, "Practiced")))
                .strengths(List.of(
                        new DashboardResponse.NamedValue("Problem Solving", 80),
                        new DashboardResponse.NamedValue("Communication", 70),
                        new DashboardResponse.NamedValue("Confidence", 65),
                        new DashboardResponse.NamedValue("Technical Depth", 74)))
                .build();
    }

    /** Tiny factory to keep demoDashboard() readable. */
    private DashboardResponse.RecentInterviews demoRow(String role, String level, LocalDate date, int score, String status) {
        return DashboardResponse.RecentInterviews.builder()
                .role(role).level(level)
                .date(date.format(DATE_FMT))
                .score(score).status(status)
                .build();
    }



}
