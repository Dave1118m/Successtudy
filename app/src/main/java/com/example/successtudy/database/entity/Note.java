package com.example.successtudy.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String subject;
    public String tags;
    public String fileUrl;
    public float rating;
    public int uploaderId;
    public double price;
    public String description;
    public String aiSummary;
    
    public Note() {}
    
    @Ignore
    public Note(String title, String subject, String tags, String fileUrl, float rating, int uploaderId, double price, String description) {
        this.title = title;
        this.subject = subject;
        this.tags = tags;
        this.fileUrl = fileUrl;
        this.rating = rating;
        this.uploaderId = uploaderId;
        this.price = price;
        this.description = description;
        this.aiSummary = "";
    }
}
