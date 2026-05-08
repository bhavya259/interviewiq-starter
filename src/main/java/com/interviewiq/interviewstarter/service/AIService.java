package com.interviewiq.interviewstarter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.*;

/* ============================================================================
 *  AIService
 * ============================================================================
 *
 *  WHAT THIS CLASS DOES (in one paragraph):
 *  ----------------------------------------
 *  We send text (a question + a candidate's answer, OR a request like
 *  "give me 5 Java questions") to Google's Gemini AI over the internet using
 *  HTTP. Gemini sends back a JSON string. We pull the useful bits out of that
 *  JSON and return a normal Java object the rest of our app can use.
 *
 *  THE BIG PICTURE (data flow):
 *  ----------------------------
 *      Java method call
 *           │
 *           ▼
 *      buildPrompt(...)                ← write English instructions for AI
 *           │
 *           ▼
 *      callGemini(prompt)              ← HTTP POST to Google's server
 *           │  (returns a big JSON string)
 *           ▼
 *      parseEvaluation(...)            ← pull score / strengths / etc.
 *           │
 *           ▼
 *      AIEvaluation object             ← returned to caller
 *
 *  WHY REGEX INSTEAD OF A JSON LIBRARY?
 *  ------------------------------------
 *  A real project would use Jackson (com.fasterxml.jackson) to parse JSON.
 *  We use regex here so beginners don't have to learn a new library on day 1.
 *  The shape of Gemini's JSON is very predictable, so simple regex works.
 *
 *  HOW TO SET THE API KEY:
 *  -----------------------
 *  In src/main/resources/application.yaml:
 *      ai:
 *        gemini:
 *          api-key: YOUR_KEY_HERE
 *  Get a free key at: https://aistudio.google.com/app/apikey
 *
 *  If the key is missing, every public method here returns null and the
 *  caller falls back to manual rule-based scoring. The app still works.
 * ============================================================================ */
@Service
public class AIService {

    /* The base URL of Google's Gemini API. We append "?key=YOUR_API_KEY" to it.
     * Example final URL:
     *   https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyABC123
     */
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    /* @Value tells Spring: "look up this property in application.yaml and inject it here."
     * The ":" at the end means: if the property is missing, default to "" (empty string).
     * So if the student forgets to set the key, apiKey is "" — not null — and we handle that. */
    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    /* RestTemplate is Spring's classic HTTP client. Think of it as "Java's fetch()".
     * We use it to POST our prompt to Gemini and read the response. */
    private final RestTemplate http = new RestTemplate();

    /* @PostConstruct = "run this method ONCE, right after Spring builds the object."
     * We use it just to print whether the key was loaded — helps students debug. */
    @jakarta.annotation.PostConstruct
    void logKeyStatus() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[AIService] Gemini API key NOT configured — using fallback scoring.");
        } else {
            System.out.println("[AIService] Gemini API key loaded (length=" + apiKey.length() + ").");
        }
    }

    /* ------------------------------------------------------------------
     *  AIEvaluation — a plain data holder for what AI sends back.
     *  Public fields (no getters/setters) keep things simple for beginners.
     *  Example after a successful call:
     *      score = 78
     *      relevance = "high"
     *      technicalAccuracy = "good"
     *      strengths = ["Clear explanation", "Mentioned time complexity"]
     *      weaknesses = ["Did not discuss edge cases"]
     *      recommendations = ["Practice tree problems"]
     * ------------------------------------------------------------------ */
    public static class AIEvaluation {
        public int score;                                       // 0-100
        public String relevance;                                // low | medium | high
        public String technicalAccuracy;                        // poor | average | good
        public List<String> strengths       = new ArrayList<>();
        public List<String> weaknesses      = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
    }

    /* ==================================================================
     *  PUBLIC METHOD #1 — score one question/answer pair using AI.
     *  Returns null on ANY problem; caller (EvaluationService) then uses
     *  its own simple rule-based scoring. This way the app never crashes
     *  just because the AI is down or the key is missing.
     * ================================================================== */
    public AIEvaluation evaluateWithAI(String question, String answer) {
        // Guard #1: no API key set in application.yaml -> can't call AI.
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        try {
            String prompt = buildPrompt(question, answer);   // step 1: English instructions
            String aiText = callGemini(prompt);              // step 2: HTTP call
            return parseEvaluation(aiText);                  // step 3: text -> Java object
        } catch (Exception e) {
            // Common reasons: no internet, invalid key, Gemini quota exceeded, weird response.
            System.err.println("[AIService] Failed: " + e.getMessage());
            return null;
        }
    }

    /* ==================================================================
     *  PUBLIC METHOD #2 — ask AI to invent N interview questions.
     *  Used when starting a new interview if you don't have a hand-written
     *  question bank for the chosen role/level/difficulty.
     * ================================================================== */
    public List<String> generateQuestions(String role,
                                          String experienceLevel,
                                          String difficulty,
                                          int count) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String prompt = buildQuestionPrompt(role, experienceLevel, difficulty, count);
            String aiText = callGemini(prompt);
            return parseQuestionList(aiText);
        } catch (Exception e) {
            System.err.println("[AIService] generateQuestions failed: " + e.getMessage());
            return null;
        }
    }

    /* Build the English instructions we send to the AI for question generation.
     * Notice we say "Return ONLY a valid JSON array of strings" — that strict
     * format makes it easy for parseQuestionList() to read the answer. */
    private String buildQuestionPrompt(String role, String exp, String difficulty, int count) {
        // Defensive defaults so we never send "null" to the AI.
        String safeRole = role == null || role.isBlank() ? "Software Engineer" : role;
        String safeExp  = exp == null || exp.isBlank()   ? "Mid"               : exp;
        String safeDiff = difficulty == null || difficulty.isBlank() ? "Medium" : difficulty;
        int n = count <= 0 ? 5 : count;

        return "You are an expert technical interviewer. " +
               "Generate exactly " + n + " interview questions for a " + safeExp +
               "-level " + safeRole + " at " + safeDiff + " difficulty.\n\n" +
               "Rules:\n" +
               "- Mix conceptual, practical, and scenario-based questions.\n" +
               "- Each question must be ONE sentence, clear and specific.\n" +
               "- No numbering, no preamble, no markdown.\n" +
               "- Return ONLY a valid JSON array of strings, e.g.:\n" +
               "[\"Question 1?\", \"Question 2?\", \"Question 3?\"]";
    }

    /* Pull a JSON array of strings out of the AI's reply.
     *
     * Example AI reply (the "clean" string below):
     *     ["What is polymorphism?", "Explain ACID properties.", "How does HashMap work?"]
     *
     * Sometimes the AI wraps it in markdown like:
     *     ```json
     *     ["...", "..."]
     *     ```
     * so we strip those backticks first.
     */
    private List<String> parseQuestionList(String aiText) {
        String clean = aiText == null ? "" : aiText.trim();

        // Remove ```json ... ``` fences if present.
        // replaceFirst with regex: ^``` matches at start; (?:json)? optionally matches "json".
        if (clean.startsWith("```")) {
            clean = clean.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        // Step A: find the [...] block.
        //   "\\[" matches a literal '['  (in Java strings "\\[" is the 2-char regex \[ )
        //   "(.*?)" captures everything inside, lazily (the smallest match)
        //   "\\]" matches a literal ']'
        //   Pattern.DOTALL makes "." match newlines too.
        Matcher block = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL).matcher(clean);
        if (!block.find()) return null;

        // Step B: inside the brackets, find every "double-quoted string".
        //   "\""             -> a literal double quote
        //   ((?:\\.|[^"\\])*) -> capture: either an escaped char (\X) or any non-quote/non-backslash char
        //   "\""             -> closing double quote
        // This handles strings that contain escaped quotes like "He said \"hi\"".
        List<String> out = new ArrayList<>();
        Matcher items = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(block.group(1));
        while (items.find()) {
            String q = unescapeJson(items.group(1)).trim();
            if (!q.isEmpty()) out.add(q);
        }
        return out.isEmpty() ? null : out;
    }

    /* Build the English instructions we send to AI for answer evaluation.
     * We strongly tell it to return JSON ONLY — no markdown, no explanations. */
    private String buildPrompt(String question, String answer) {
        return "You are an expert technical interviewer. " +
               "Evaluate the candidate's answer.\n\n" +
               "Question: " + question + "\n" +
               "Answer: "   + answer   + "\n\n" +
               "Evaluate based on:\n" +
               "1. Relevance to the question\n" +
               "2. Technical correctness\n" +
               "3. Clarity\n\n" +
               "Return ONLY valid JSON in this exact format (no markdown, no prose):\n" +
               "{\n" +
               "  \"score\": 0,\n" +
               "  \"relevance\": \"low\",\n" +
               "  \"technicalAccuracy\": \"poor\",\n" +
               "  \"strengths\": [\"...\"],\n" +
               "  \"weaknesses\": [\"...\"],\n" +
               "  \"recommendations\": [\"...\"]\n" +
               "}";
    }

    /* ==================================================================
     *  callGemini — does the actual HTTP POST to Google.
     *
     *  WHAT GEMINI EXPECTS (request body):
     *    {
     *      "contents": [
     *        { "parts": [ { "text": "<our prompt here>" } ] }
     *      ]
     *    }
     *
     *  WHAT GEMINI RETURNS (response body, simplified):
     *    {
     *      "candidates": [
     *        { "content": { "parts": [ { "text": "<AI's answer>" } ] } }
     *      ]
     *    }
     *
     *  We only care about that inner "text" field — that's the AI's reply.
     * ================================================================== */
    private String callGemini(String prompt) throws Exception {
        // Build the JSON body by hand. We must escape the prompt because the
        // prompt itself may contain quotes/newlines that would BREAK the JSON.
        // Example: prompt = ' Say "hi" '
        //   Without escaping: {"text":" Say "hi" "}  <-- broken JSON
        //   With escaping:    {"text":" Say \"hi\" "} <-- valid JSON
        String body = "{\"contents\":[{\"parts\":[{\"text\":\""
                + escapeJson(prompt) + "\"}]}]}";

        // Tell Gemini we're sending JSON.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> req = new HttpEntity<>(body, headers);

        // POST to: https://...generateContent?key=YOUR_KEY
        ResponseEntity<String> resp = http.exchange(
                GEMINI_URL + apiKey, HttpMethod.POST, req, String.class
        );

        // The full JSON Gemini sent us, as one big String.
        String full = resp.getBody();

        // We want the value of the FIRST "text" field. Regex breakdown:
        //   "\"text\""        -> the literal word "text" with surrounding quotes
        //   "\\s*:\\s*"       -> a colon, possibly with whitespace around it
        //   "\""              -> opening quote of the value
        //   "((?:\\\\.|[^\"\\\\])*)"  -> capture: zero or more of (escaped char OR any non-quote)
        //   "\""              -> closing quote
        Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(full);
        if (!m.find()) throw new RuntimeException("No 'text' field in Gemini response");

        // m.group(1) is still in JSON-escaped form (e.g. \"  \n  \\). Convert back to a normal Java string.
        return unescapeJson(m.group(1));
    }

    /* ==================================================================
     *  parseEvaluation — turn the AI's JSON text into our AIEvaluation.
     *
     *  Example aiText we might receive:
     *  {
     *    "score": 72,
     *    "relevance": "high",
     *    "technicalAccuracy": "good",
     *    "strengths": ["Clear", "Used examples"],
     *    "weaknesses": ["No edge cases"],
     *    "recommendations": ["Practice trees"]
     *  }
     * ================================================================== */
    private AIEvaluation parseEvaluation(String aiText) {
        // Strip ```json fences if the AI ignored our "no markdown" rule.
        String clean = aiText.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        AIEvaluation e = new AIEvaluation();
        e.score             = parseInt(clean,    "score",             50);        // fallback 50
        e.relevance         = parseString(clean, "relevance",         "medium");
        e.technicalAccuracy = parseString(clean, "technicalAccuracy", "average");
        e.strengths         = parseArray(clean,  "strengths");
        e.weaknesses        = parseArray(clean,  "weaknesses");
        e.recommendations   = parseArray(clean,  "recommendations");
        return e;
    }

    /* ==================================================================
     *  Tiny JSON helpers (regex-based — fine because the AI's JSON shape
     *  is small and predictable).
     * ================================================================== */

    /* Find a number after "key": .
     * Example: parseInt({"score": 72}, "score", 0) -> 72
     *
     * Regex pieces:
     *   "\"" + key + "\""  -> the literal key in quotes, e.g. "score"
     *   "\\s*:\\s*"        -> colon with optional whitespace
     *   "(\\d+)"           -> capture one or more digits
     */
    private int parseInt(String json, String key, int fallback) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }

    /* Find a string after "key": .
     * Example: parseString({"relevance": "high"}, "relevance", "low") -> "high"
     *
     * "([^\"]*)" -> capture any chars except a quote (good enough for our well-formed AI output).
     */
    private String parseString(String json, String key, String fallback) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : fallback;
    }

    /* Find an array of strings after "key": .
     * Example:
     *   json = { "strengths": ["Clear", "Concise"] }
     *   parseArray(json, "strengths") -> ["Clear", "Concise"]
     *
     * Two-step: first grab the "[ ... ]" block, then pull each "..." inside it.
     */
    private List<String> parseArray(String json, String key) {
        List<String> out = new ArrayList<>();
        Matcher block = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL)
                               .matcher(json);
        if (!block.find()) return out;   // key missing -> empty list

        Matcher items = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(block.group(1));
        while (items.find()) out.add(unescapeJson(items.group(1)));
        return out;
    }

    /* ==================================================================
     *  ESCAPING / UNESCAPING
     *
     *  WHY DO WE NEED THIS?
     *  --------------------
     *  JSON is just text. Inside a JSON string, certain characters are
     *  SPECIAL and must be written with a backslash:
     *
     *      Real character        How JSON writes it
     *      ------------------    ------------------
     *      "  (double quote)     \"
     *      \  (backslash)        \\
     *      newline               \n
     *      carriage return       \r
     *      tab                   \t
     *
     *  EXAMPLE — escapeJson (Java string -> JSON-safe string):
     *  -------------------------------------------------------
     *   (3 chars: " H i ", newline, tab):
     *      He said "Hi"
     *      <tab>bye
     *
     *  If we drop it straight into JSON, we get BROKEN JSON:
     *      {"text":"He said "Hi"
     *      	bye"}
     *
     *  After escapeJson(...) it becomes a SAFE one-liner:
     *      He said \"Hi\"\n\tbye
     *
     *  And the full JSON is now valid:
     *      {"text":"He said \"Hi\"\n\tbye"}
     *
     *  EXAMPLE — unescapeJson (JSON-safe string -> normal Java string):
     *  ----------------------------------------------------------------
     *  Gemini's response contains:   He said \"Hi\"\n\tbye
     *  Java sees these as literal characters:  \  "  H ...  \  n  \  t ...
     *  unescapeJson turns them back into the real characters " and newline and tab
     *  so we end up with what a human would actually read.
     *
     *  ORDER MATTERS!
     *  --------------
     *  In escapeJson we replace "\\" FIRST. Why?
     *    If we replaced " -> \" first, the new \ we just inserted would itself
     *    get doubled in the next step, corrupting the output.
     *  In unescapeJson we replace "\\\\" LAST for the same reason in reverse.
     * ================================================================== */

    /* Java -> JSON-safe.
     * Note on the Java string literals here:
     *   "\\"   in Java code = ONE backslash character at runtime (\)
     *   "\\\\" in Java code = TWO backslashes at runtime (\\)
     *   "\""   in Java code = ONE double-quote character at runtime (")
     *   "\\\"" in Java code = a backslash followed by a double-quote (\")  <-- the JSON escape
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")   // \  ->  \\        (must be FIRST — see note above)
                .replace("\"", "\\\"")   // "  ->  \"
                .replace("\n", "\\n")    // newline   -> \n  (two chars: backslash + n)
                .replace("\r", "\\r")    // CR        -> \r
                .replace("\t", "\\t");   // tab       -> \t
    }

    /* JSON-safe -> Java.
     * We do the reverse. Replace the two-char sequences (like \  +  n) with
     * the real single character (newline). Backslash-backslash goes LAST so
     * sequences we just produced aren't reinterpreted.
     */
    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")     // \n -> real newline
                .replace("\\r", "\r")     // \r -> real CR
                .replace("\\t", "\t")     // \t -> real tab
                .replace("\\\"", "\"")    // \" -> "
                .replace("\\\\", "\\");   // \\ -> \   (must be LAST)
    }
}
