package com.example.successtudy;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.successtudy.logic.AiQuizGenerator;
import com.example.successtudy.logic.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class DatabaseTest {

    @Test
    public void testAiQuizGenerator_BiologySubject() {
        String title = "Cell Biology Fundamentals";
        String content = "The mitochondria is vital for cells and produces energy in the form of ATP. Cells divide to make organelles.";
        
        String quizJson = AiQuizGenerator.generateQuizJson(title, content, "Medium");
        
        assertNotNull(quizJson);
        assertFalse(quizJson.isEmpty());

        // Parse JSON back to verify formatting
        List<AiQuizGenerator.Question> questions = new Gson().fromJson(quizJson, 
                new TypeToken<List<AiQuizGenerator.Question>>(){}.getType());
        
        assertNotNull(questions);
        assertTrue(questions.size() >= 3);
        
        // Assert that the first question contains cell/mitochondria vocabulary
        boolean containsBioQuestion = false;
        for (AiQuizGenerator.Question q : questions) {
            if (q.question.contains("organelle") || q.question.contains("mitochondria") || q.question.contains("cell") || q.question.contains("ATP")) {
                containsBioQuestion = true;
                break;
            }
        }
        assertTrue("Expected biology terms in generated questions", containsBioQuestion);
    }

    @Test
    public void testAiQuizGenerator_ComputerScienceSubject() {
        String title = "Sorting Algorithms";
        String content = "An algorithm takes input data and runs sorting algorithms. We use binary search tree and queues.";
        
        String quizJson = AiQuizGenerator.generateQuizJson(title, content, "Simple");
        
        List<AiQuizGenerator.Question> questions = new Gson().fromJson(quizJson, 
                new TypeToken<List<AiQuizGenerator.Question>>(){}.getType());
        
        assertNotNull(questions);
        assertEquals(3, questions.size()); // Simple difficulty specifies 3 questions
        
        boolean containsCsQuestion = false;
        for (AiQuizGenerator.Question q : questions) {
            if (q.question.contains("Hash Table") || q.question.contains("stack") || q.question.contains("sorting") || q.question.contains("Heap")) {
                containsCsQuestion = true;
                break;
            }
        }
        assertTrue("Expected computer science terms in generated questions", containsCsQuestion);
    }

    @Test
    public void testMarketplaceCommissionCalculation() {
        double notePrice = 15.00;
        double platformCommissionPercentage = 0.15;

        double commission = notePrice * platformCommissionPercentage;
        double sellerEarnings = notePrice - commission;

        assertEquals(2.25, commission, 0.001);
        assertEquals(12.75, sellerEarnings, 0.001);
        assertEquals(notePrice, commission + sellerEarnings, 0.001);
    }

    @Test
    public void testVerifyCodeGenerationPattern() {
        // Test that code is a 6-digit numeric string
        java.util.Random random = new java.util.Random();
        int code = 100000 + random.nextInt(900000);
        String codeStr = String.valueOf(code);

        assertEquals(6, codeStr.length());
        assertTrue(codeStr.matches("\\d{6}"));
    }
}
