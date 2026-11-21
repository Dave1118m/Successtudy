package com.example.successtudy.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "analytics")
public class UserAnalytics {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int userId;
    public int quizId;
    public int score;
    public int progress;
    public long dateTaken;
    
    public UserAnalytics() {}
    
    @Ignore
    public UserAnalytics(int userId, int quizId, int score, int progress, long dateTaken) {
        this.userId = userId;
        this.quizId = quizId;
        this.score = score;
        this.progress = progress;
        this.dateTaken = dateTaken;
    }
}
