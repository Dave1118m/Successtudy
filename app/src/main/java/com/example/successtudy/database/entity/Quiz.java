package com.example.successtudy.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "quizzes")
public class Quiz {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int noteId;
    public String title;
    public String difficulty; // "Simple", "Medium", "Difficult"
    public String questionsJson; // JSON list of questions
    
    public Quiz() {}
    
    @Ignore
    public Quiz(int noteId, String title, String difficulty, String questionsJson) {
        this.noteId = noteId;
        this.title = title;
        this.difficulty = difficulty;
        this.questionsJson = questionsJson;
    }
}
