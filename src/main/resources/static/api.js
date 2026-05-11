/* ============================================
   Backend API Client
   --------------------------------------------
   Thin wrapper around the Spring Boot endpoints:
     GET  /questions/{interviewId}  -> AI-generated + cached questions
     POST /answers                  -> save a batch of answers
     POST /evaluate-answer          -> score + feedback for the interview
   ============================================ */

const API_BASE = ""; // same origin (Spring Boot serves the HTML too)

const DURATION_TO_COUNT = {
  "10 Minutes": 3,
  "20 Minutes": 5,
  "30 Minutes": 7
};

async function generateInterview(setup) {
  // We need an interviewId so questions can be persisted against it.
  // duration.html stores it after POST /interview/create.
  const interviewId = localStorage.getItem("interviewId");
  if (!interviewId) {
    throw new Error("No interviewId in localStorage. Did POST /interview/create run?");
  }

  // Backend handles AI + caching + fallback questions in QuestionService.
  const res = await fetch(API_BASE + "/questions/" + interviewId);
  if (!res.ok) {
    throw new Error("Failed to load questions: HTTP " + res.status);
  }

  const rows = await res.json();   // [{ id, interviewId, questionText }]
  let questions = rows.map(r => ({
    id: r.id,
    question: r.questionText
  }));

  // Trim by duration on the client side (UX choice).
  const limit = DURATION_TO_COUNT[setup.duration];
  if (limit && questions.length > limit) {
    questions = questions.slice(0, limit);
  }

  return questions;
}

async function submitAnswersToBackend(answers) {
  const res = await fetch(API_BASE + "/answers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      answers: answers.map(a => ({
        questionId: a.questionId,
        answerText: a.answer
      }))
    })
  });
  if (!res.ok) throw new Error("Failed to save answers: " + res.status);
  return res.json();
}

async function submitInterview(answers) {
  const res = await fetch(API_BASE + "/evaluate-answer", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      answers: answers.map(a => ({
        questionId: a.questionId,
        answerText: a.answer
      }))
    })
  });
  if (!res.ok) throw new Error("Failed to evaluate: " + res.status);

  const data = await res.json();
  return {
    score:         data.score,
    strengths:     data.strengths       || [],
    weaknesses:    data.weaknesses      || [],
    suggestions:   data.recommendations || [],   // map for UI
    fillerWords:   data.fillerWords,
    confidence:    data.confidence,
    answeredCount: answers.length
  };
}

