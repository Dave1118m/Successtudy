package com.example.successtudy.logic;

import android.content.Context;
import android.util.Log;

import com.example.successtudy.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeminiService {

    private static final String TAG = "GeminiService";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public static String getApiKey(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String key = sessionManager.getGeminiApiKey();
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        try {
            return context.getString(R.string.default_gemini_api_key).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getModel(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String model = sessionManager.getGeminiModel();
        if (model == null || model.trim().isEmpty() || model.equals("gemini-1.5-pro")) {
            return "gemini-flash-latest";
        }
        return model.trim();
    }

    /**
     * Generates a structured academic summary and key notes for uploaded study material.
     */
    public static String generateSummary(Context context, String title, String subject, String contentText) {
        String apiKey = getApiKey(context);
        if (apiKey.isEmpty() || apiKey.contains("YOUR_")) {
            return "⚠️ Gemini API Key Not Configured\n\nPlease open Settings (top right gear icon) and enter your Gemini Pro API key to automatically generate AI summaries from notes.";
        }

        String model = getModel(context);
        String trimmedContent = contentText != null && contentText.length() > 8000 
                ? contentText.substring(0, 8000) : (contentText != null ? contentText : "");

        String prompt = "You are an expert academic tutor. Analyze the following study material thoroughly and generate an informative, high-yield study summary for students.\n\n"
                + "Topic/Title: " + title + "\n\n"
                + "Material Content:\n" + trimmedContent + "\n\n"
                + "Format the summary strictly as:\n"
                + "📌 1. EXECUTIVE SUMMARY\n"
                + "(Provide a thorough explanation of the main themes and arguments)\n\n"
                + "🔑 2. KEY CONCEPTS & DEFINITIONS\n"
                + "(Bullet list of critical terms, theories, and rules explained in the text)\n\n"
                + "💡 3. CORE TAKEAWAYS & EXAM REVIEW\n"
                + "(Key high-yield points for exam preparation and quick revision)";

        try {
            JsonObject requestJson = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            contentObj.add("parts", parts);
            contents.add(contentObj);
            requestJson.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.3);
            requestJson.add("generationConfig", generationConfig);

            String responseText = executeGeminiRequest(model, apiKey, requestJson.toString());
            return extractTextFromResponse(responseText);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate AI summary", e);
            return "❌ AI Summary Error: " + e.getMessage();
        }
    }

    /**
     * Generates a quiz based strictly on the uploaded material text.
     */
    public static String generateQuizJson(Context context, String title, String contentText, String difficulty, int count) {
        String apiKey = getApiKey(context);
        String trimmedContent = contentText != null && contentText.length() > 12000 
                ? contentText.substring(0, 12000) : (contentText != null ? contentText : "");

        if (apiKey.isEmpty() || apiKey.contains("YOUR_")) {
            return createFallbackQuizJson(trimmedContent, difficulty, count);
        }

        String model = getModel(context);
        String prompt = "You are an academic examiner. Carefully read the following study material and create exactly " + count 
                + " challenging quiz questions tailored to difficulty level: " + difficulty + ".\n\n"
                + "CRITICAL RULES:\n"
                + "1. ALL questions MUST test specific, substantive facts, concepts, definitions, formulas, or principles found directly within the text below.\n"
                + "2. DO NOT ask meta-questions about the title, file name, author, or document structure. (Never ask 'What is the title?' or 'What is this document about?').\n"
                + "3. Create high-quality Multiple-Choice Questions (MCQ) with 4 plausible options, True/False (TF) questions, and Short Answer (SA) questions.\n"
                + "4. Return ONLY a valid JSON array of objects without any markdown formatting.\n\n"
                + "Study Material Text:\n" + trimmedContent + "\n\n"
                + "Output JSON Schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Question testing specific fact/concept?\",\n"
                + "    \"type\": \"MCQ\", // Must be \"MCQ\", \"TF\", or \"SA\"\n"
                + "    \"choices\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], // 4 items for MCQ, [\"True\", \"False\"] for TF, [] for SA\n"
                + "    \"correctAnswerIndex\": 0, // 0-based index for MCQ and TF, -1 for SA\n"
                + "    \"answerKeyword\": \"\" // single word/term for SA, empty string for MCQ/TF\n"
                + "  }\n"
                + "]";

        try {
            JsonObject requestJson = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            contentObj.add("parts", parts);
            contents.add(contentObj);
            requestJson.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.2);
            generationConfig.addProperty("responseMimeType", "application/json");
            requestJson.add("generationConfig", generationConfig);

            String responseText = executeGeminiRequest(model, apiKey, requestJson.toString());
            String jsonOutput = extractTextFromResponse(responseText);
            jsonOutput = sanitizeJson(jsonOutput);

            // Validate and normalize that it parses as JSON array
            JsonElement parsed = JsonParser.parseString(jsonOutput);
            if (parsed.isJsonArray() && parsed.getAsJsonArray().size() > 0) {
                for (JsonElement elem : parsed.getAsJsonArray()) {
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        String qType = obj.has("type") ? obj.get("type").getAsString() : "MCQ";
                        if ("multiple_choice".equalsIgnoreCase(qType) || "choice".equalsIgnoreCase(qType)) {
                            obj.addProperty("type", "MCQ");
                        } else if ("true_false".equalsIgnoreCase(qType) || "boolean".equalsIgnoreCase(qType)) {
                            obj.addProperty("type", "TF");
                        } else if ("short_answer".equalsIgnoreCase(qType) || "text".equalsIgnoreCase(qType)) {
                            obj.addProperty("type", "SA");
                        }
                    }
                }
                return parsed.toString();
            } else {
                return createFallbackQuizJson(trimmedContent, difficulty, count);
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini quiz generation error", e);
            return createFallbackQuizJson(trimmedContent, difficulty, count);
        }
    }

    /**
     * Test the API key connection.
     */
    public static String testConnection(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "API Key is empty.";
        }
        if (model == null || model.trim().isEmpty()) {
            model = "gemini-flash-latest";
        }

        try {
            JsonObject requestJson = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", "Hello, are you connected?");
            parts.add(part);
            contentObj.add("parts", parts);
            contents.add(contentObj);
            requestJson.add("contents", contents);

            String responseText = executeGeminiRequest(model.trim(), apiKey.trim(), requestJson.toString());
            String reply = extractTextFromResponse(responseText);
            if (!reply.isEmpty()) {
                return "SUCCESS: Connected to " + model + " successfully!";
            } else {
                return "Received empty response from Gemini.";
            }
        } catch (Exception e) {
            return "Connection Failed: " + e.getMessage();
        }
    }

    private static String executeGeminiRequest(String model, String apiKey, String payload) throws Exception {
        String urlString = BASE_URL + model + ":generateContent?key=" + apiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(45000);
        conn.setReadTimeout(90000);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) 
                ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("HTTP " + responseCode + ": " + response.toString());
        }

        return response.toString();
    }

    private static String extractTextFromResponse(String rawJson) {
        try {
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        return parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response JSON", e);
        }
        return rawJson;
    }

    private static String sanitizeJson(String json) {
        String clean = json.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    private static String createFallbackQuizJson(String contentText, String difficulty, int count) {
        List<AiQuizGenerator.Question> questions = new ArrayList<>();
        if (contentText != null && !contentText.isEmpty()) {
            String[] sentences = contentText.split("[.!?]+");
            for (String s : sentences) {
                String t = s.trim();
                if (t.length() > 30 && (t.contains(" is ") || t.contains(" are ") || t.contains(" was ") || t.contains(" were "))) {
                    questions.add(new AiQuizGenerator.Question(
                            "According to the material: \"" + t + "\", is this correct?",
                            "TF",
                            List.of("True", "False"),
                            0,
                            ""
                    ));
                    if (questions.size() >= count) break;
                }
            }
        }

        if (questions.isEmpty()) {
            questions.add(new AiQuizGenerator.Question(
                    "Please connect to the internet and ensure your Gemini API key is configured to generate full AI quizzes from this material.",
                    "TF",
                    List.of("True", "False"),
                    0,
                    ""
            ));
        }

        return new Gson().toJson(questions);
    }
}
