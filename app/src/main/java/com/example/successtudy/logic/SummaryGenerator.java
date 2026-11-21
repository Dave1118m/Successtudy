package com.example.successtudy.logic;

import android.content.Context;
import com.google.gson.Gson;
import java.util.List;

/**
 * AI-powered and rule-based summarizer for study notes.
 */
public class SummaryGenerator {

    /**
     * Generate an AI-powered comprehensive study summary and key notes using Gemini.
     */
    public static String generateAiSummary(Context context, String noteTitle, String subject, String noteText) {
        return GeminiService.generateSummary(context, noteTitle, subject, noteText);
    }

    /**
     * Fallback quick point extractor if offline.
     */
    public static String generateSummaryJson(String noteTitle, String noteText, int maxPoints) {
        if (noteText == null || noteText.isEmpty()) {
            return new Gson().toJson(List.of(noteTitle));
        }

        String[] rawSentences = noteText.split("[.!?]+");
        java.util.List<String> summary = new java.util.ArrayList<>();
        for (String s : rawSentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 25) {
                summary.add(trimmed);
                if (summary.size() >= maxPoints) break;
            }
        }

        if (summary.isEmpty()) {
            summary.add(noteTitle);
        }

        return new Gson().toJson(summary);
    }
}
