package com.example.successtudy.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public String email;
    public String passwordHash;
    public String role; // "Student", "Teacher", "Admin"
    public String university;
    public double balance;
    public boolean isVerified;
    public String verificationCode;
    public String mockToken; // Simulated JWT
    
    public User() {}
    
    @Ignore
    public User(String name, String email, String passwordHash, String role, String university, double balance, boolean isVerified) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.university = university;
        this.balance = balance;
        this.isVerified = isVerified;
        this.mockToken = "";
        this.verificationCode = "";
    }
}
