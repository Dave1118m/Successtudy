package com.example.successtudy.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int buyerId;
    public int sellerId;
    public int noteId;
    public double amount;
    public long timestamp;
    
    public Transaction() {}
    
    @Ignore
    public Transaction(int buyerId, int sellerId, int noteId, double amount, long timestamp) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.noteId = noteId;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
