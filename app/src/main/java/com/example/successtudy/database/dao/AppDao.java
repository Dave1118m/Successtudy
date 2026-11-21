package com.example.successtudy.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.Quiz;
import com.example.successtudy.database.entity.Transaction;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.database.entity.UserAnalytics;

import java.util.List;

@Dao
public interface AppDao {
    // --- User Queries ---
    @Insert
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users ORDER BY balance DESC")
    List<User> getAllUsers();

    // --- Note Queries ---
    @Insert
    long insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE subject = :subject")
    List<Note> getNotesBySubject(String subject);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteById(int id);

    @Query("SELECT * FROM notes WHERE uploaderId = :uploaderId")
    List<Note> getNotesByUploader(int uploaderId);

    // --- Quiz Queries ---
    @Insert
    long insertQuiz(Quiz quiz);

    @Update
    void updateQuiz(Quiz quiz);

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    Quiz getQuizById(int id);

    @Query("SELECT * FROM quizzes WHERE noteId = :noteId")
    List<Quiz> getQuizzesForNote(int noteId);

    @Query("SELECT * FROM quizzes")
    List<Quiz> getAllQuizzes();

    // --- Transaction Queries ---
    @Insert
    long insertTransaction(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE buyerId = :buyerId")
    List<Transaction> getTransactionsForBuyer(int buyerId);

    @Query("SELECT * FROM transactions WHERE sellerId = :sellerId")
    List<Transaction> getTransactionsForSeller(int sellerId);

    @Query("SELECT * FROM transactions WHERE noteId = :noteId AND buyerId = :buyerId LIMIT 1")
    Transaction getTransactionByNoteAndBuyer(int noteId, int buyerId);

    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactions();

    // --- Analytics Queries ---
    @Insert
    long insertAnalytics(UserAnalytics analytics);

    @Query("SELECT * FROM analytics WHERE userId = :userId ORDER BY dateTaken DESC")
    List<UserAnalytics> getAnalyticsForUser(int userId);

    @Query("SELECT * FROM analytics WHERE userId = :userId AND quizId = :quizId ORDER BY dateTaken DESC")
    List<UserAnalytics> getAnalyticsForUserAndQuiz(int userId, int quizId);

    @Query("SELECT AVG(score) FROM analytics WHERE userId = :userId")
    float getAverageScoreForUser(int userId);
}
