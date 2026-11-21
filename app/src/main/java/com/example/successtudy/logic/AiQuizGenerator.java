package com.example.successtudy.logic;

import android.content.Context;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AiQuizGenerator {

    public static class Question {
        public String question;
        public String type; // "MCQ", "TF", "SA"
        public List<String> choices; // empty for SA
        public int correctAnswerIndex; // -1 for SA
        public String answerKeyword; // for SA matching

        public Question(String question, String type, List<String> choices, int correctAnswerIndex, String answerKeyword) {
            this.question = question;
            this.type = type;
            this.choices = choices;
            this.correctAnswerIndex = correctAnswerIndex;
            this.answerKeyword = answerKeyword;
        }
    }

    /**
     * Primary AI-powered quiz generator using Gemini.
     */
    public static String generateQuizJsonWithContext(Context context, String noteTitle, String noteText, String difficulty, int count) {
        return GeminiService.generateQuizJson(context, noteTitle, noteText, difficulty, count);
    }

    /**
     * Overload for calls with or without Context.
     */
    public static String generateQuizJsonWithCount(String noteTitle, String noteText, String difficulty, int count) {
        return generateDynamicFallbackQuiz(noteTitle, noteText, difficulty, count);
    }

    /**
     * Fallback dynamic question generation from sentences when offline or key is missing.
     */
    private static String generateDynamicFallbackQuiz(String noteTitle, String noteText, String difficulty, int count) {
        List<Question> questions = new ArrayList<>();
        if (noteText == null) noteText = "";

        String[] sentences = noteText.split("[.!?]+");
        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 25 && (trimmed.contains(" is ") || trimmed.contains(" was ") || trimmed.contains(" are ") || trimmed.contains(" were "))) {
                validSentences.add(trimmed);
            }
        }

        if (validSentences.isEmpty()) {
            questions.add(new Question("What is the central focus of '" + noteTitle + "'?", "MCQ",
                    List.of("Core concepts and definitions", "Historical background", "Applied methodology", "Theoretical summary"), 0, ""));
            questions.add(new Question("The material in '" + noteTitle + "' covers fundamental principles.", "TF",
                    List.of("True", "False"), 0, ""));
            questions.add(new Question("Name the main subject explored in '" + noteTitle + "':", "SA",
                    new ArrayList<>(), -1, noteTitle.split(" ")[0].toLowerCase()));
        } else {
            Random random = new Random();
            for (int i = 0; i < Math.min(validSentences.size(), count); i++) {
                String sentence = validSentences.get(i);
                if (i % 2 == 0) {
                    questions.add(new Question("According to the text: \"" + sentence + "\", is this correct?", "TF",
                            List.of("True", "False"), 0, ""));
                } else {
                    String copula = " is ";
                    if (sentence.contains(" was ")) copula = " was ";
                    else if (sentence.contains(" are ")) copula = " are ";
                    else if (sentence.contains(" were ")) copula = " were ";

                    int splitIndex = sentence.indexOf(copula);
                    if (splitIndex > 0) {
                        String firstPart = sentence.substring(0, splitIndex).trim();
                        String secondPart = sentence.substring(splitIndex + copula.length()).trim();

                        List<String> choices = new ArrayList<>();
                        choices.add(secondPart);
                        choices.add("an unrelated theoretical exception");
                        choices.add("the inverse of the expected behavior");
                        choices.add("a transient condition");
                        Collections.shuffle(choices);
                        int correctIdx = choices.indexOf(secondPart);

                        questions.add(new Question("According to the notes, \"" + firstPart + "\"" + copula + "...", "MCQ", choices, correctIdx, ""));
                    }
                }
            }
        }

        List<Question> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, questions.size()); i++) {
            result.add(questions.get(i));
        }
        return new Gson().toJson(result);
    }
}
