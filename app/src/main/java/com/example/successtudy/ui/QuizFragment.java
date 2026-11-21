package com.example.successtudy.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.Quiz;
import com.example.successtudy.database.entity.UserAnalytics;
import com.example.successtudy.logic.AiQuizGenerator;
import com.example.successtudy.logic.FileStorageHelper;
import com.example.successtudy.logic.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {

    // Setup Views
    private View layoutQuizSetup;
    private Spinner spQuizSourceNote;
    private EditText etQuizQuestionCount;
    private RadioGroup rgQuizDifficulty;
    private Button btnStartQuiz, btnStartExamSim, btnUploadNewNote;

    // Active Views
    private View layoutQuizActive;
    private TextView tvQuizProgressIndicator, tvQuizTimer, tvQuestionTypeBadge, tvQuestionText;
    private ProgressBar pbQuizTimer;
    private RadioGroup rgQuizOptions;
    private RadioButton rbOpt1, rbOpt2, rbOpt3, rbOpt4;
    private LinearLayout layoutShortAnswer;
    private EditText etShortAnswerInput;
    private Button btnSubmitAnswer;

    // Results Views
    private View layoutQuizResults;
    private TextView tvResultScore, tvResultFraction, tvResultFeedback, tvWeakAreasList;
    private Button btnFinishQuiz;

    // Controllers
    private SessionManager sessionManager;
    private AppDao appDao;
    private List<Note> noteList = new ArrayList<>();
    private List<String> spinnerTitles = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    // Quiz Session State
    private List<AiQuizGenerator.Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private int activeQuizId = -1;
    private boolean isExamSim = false;
    private int totalTimeSeconds = 120; // 2 minutes standard
    private int secondsRemaining = 120;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                int minutes = secondsRemaining / 60;
                int seconds = secondsRemaining % 60;
                tvQuizTimer.setText(String.format("%02d:%02d", minutes, seconds));
                
                int progress = (secondsRemaining * 100) / totalTimeSeconds;
                pbQuizTimer.setProgress(progress);
                
                timerHandler.postDelayed(this, 1000);
            } else {
                Toast.makeText(getContext(), "Time's Up! Auto-submitting exam.", Toast.LENGTH_LONG).show();
                finishQuizSession();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        appDao = AppDatabase.getDatabase(requireContext()).appDao();

        // Bind Setup
        layoutQuizSetup = view.findViewById(R.id.layoutQuizSetup);
        spQuizSourceNote = view.findViewById(R.id.spQuizSourceNote);
        etQuizQuestionCount = view.findViewById(R.id.etQuizQuestionCount);
        rgQuizDifficulty = view.findViewById(R.id.rgQuizDifficulty);
        btnStartQuiz = view.findViewById(R.id.btnStartQuiz);
        btnStartExamSim = view.findViewById(R.id.btnStartExamSim);
        btnUploadNewNote = view.findViewById(R.id.btnUploadNewNote);

        // Bind Active
        layoutQuizActive = view.findViewById(R.id.layoutQuizActive);
        tvQuizProgressIndicator = view.findViewById(R.id.tvQuizProgressIndicator);
        tvQuizTimer = view.findViewById(R.id.tvQuizTimer);
        pbQuizTimer = view.findViewById(R.id.pbQuizTimer);
        tvQuestionTypeBadge = view.findViewById(R.id.tvQuestionTypeBadge);
        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        rgQuizOptions = view.findViewById(R.id.rgQuizOptions);
        rbOpt1 = view.findViewById(R.id.rbOpt1);
        rbOpt2 = view.findViewById(R.id.rbOpt2);
        rbOpt3 = view.findViewById(R.id.rbOpt3);
        rbOpt4 = view.findViewById(R.id.rbOpt4);
        layoutShortAnswer = view.findViewById(R.id.layoutShortAnswer);
        etShortAnswerInput = view.findViewById(R.id.etShortAnswerInput);
        btnSubmitAnswer = view.findViewById(R.id.btnSubmitAnswer);

        // Bind Results
        layoutQuizResults = view.findViewById(R.id.layoutQuizResults);
        tvResultScore = view.findViewById(R.id.tvResultScore);
        tvResultFraction = view.findViewById(R.id.tvResultFraction);
        tvResultFeedback = view.findViewById(R.id.tvResultFeedback);
        tvWeakAreasList = view.findViewById(R.id.tvWeakAreasList);
        btnFinishQuiz = view.findViewById(R.id.btnFinishQuiz);

        // Setup Source Spinner
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, spinnerTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQuizSourceNote.setAdapter(spinnerAdapter);

        loadNotesList();

        btnUploadNewNote.setOnClickListener(v -> {
            // Navigate to Upload tab
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_upload);
        });

        btnStartQuiz.setOnClickListener(v -> startQuizSession(false));
        btnStartExamSim.setOnClickListener(v -> startQuizSession(true));
        
        btnSubmitAnswer.setOnClickListener(v -> submitActiveAnswer());
        btnFinishQuiz.setOnClickListener(v -> resetToSetup());
    }

    private void loadNotesList() {
        new Thread(() -> {
            List<Note> allNotes = appDao.getAllNotes();
            
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                noteList.clear();
                spinnerTitles.clear();
                
                noteList.addAll(allNotes);
                for (Note note : allNotes) {
                    String title = note.title;
                    if (note.price > 0) {
                        title += " (Premium - " + String.format("$%.2f", note.price) + ")";
                    } else {
                        title += " (Free)";
                    }
                    spinnerTitles.add(title);
                }
                spinnerAdapter.notifyDataSetChanged();

                // Check preselect bundle
                if (getArguments() != null && getArguments().containsKey("preselect_note_id")) {
                    int preselectId = getArguments().getInt("preselect_note_id");
                    for (int i = 0; i < noteList.size(); i++) {
                        if (noteList.get(i).id == preselectId) {
                            spQuizSourceNote.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void startQuizSession(boolean timed) {
        if (noteList.isEmpty()) {
            Toast.makeText(getContext(), "Please upload a study note before generating quizzes.", Toast.LENGTH_LONG).show();
            return;
        }

        int noteIdx = spQuizSourceNote.getSelectedItemPosition();
        Note selectedNote = noteList.get(noteIdx);
        
        int diffCheckedId = rgQuizDifficulty.getCheckedRadioButtonId();
        String difficulty = "Medium";
        if (diffCheckedId == R.id.rbDiffSimple) {
            difficulty = "Simple";
        } else if (diffCheckedId == R.id.rbDiffDifficult) {
            difficulty = "Difficult";
        }

        final String finalDiff = difficulty;
        isExamSim = timed;
        int count = 5;
        try {
            String countStr = etQuizQuestionCount.getText().toString().trim();
            if (!countStr.isEmpty()) {
                count = Integer.parseInt(countStr);
                if (count <= 0) count = 5;
                if (count > 50) count = 50;
            }
        } catch (Exception ignored) {}
        final int requestedCount = count;

        Toast.makeText(getContext(), "AI is reading material and generating " + requestedCount + " questions (" + finalDiff + ")...", Toast.LENGTH_SHORT).show();
        btnStartQuiz.setEnabled(false);
        btnStartExamSim.setEnabled(false);

        new Thread(() -> {
            // Check if there is an existing quiz for this note + difficulty in the DB
            List<Quiz> existingQuizzes = appDao.getQuizzesForNote(selectedNote.id);
            Quiz targetQuiz = null;
            for (Quiz q : existingQuizzes) {
                if (finalDiff.equalsIgnoreCase(q.difficulty)) {
                    targetQuiz = q;
                    break;
                }
            }

            if (targetQuiz == null) {
                // Generate dynamically using notes info with extracted text via Gemini AI
                String title = selectedNote.title + " (" + finalDiff + " Practice)";
                String extracted = new com.example.successtudy.logic.FileStorageHelper(requireContext()).extractTextFromFile(selectedNote.fileUrl);
                String sourceText = (extracted == null || extracted.isEmpty()) ? (selectedNote.description + " " + selectedNote.subject) : extracted;
                String generatedJson = com.example.successtudy.logic.GeminiService.generateQuizJson(requireContext(), selectedNote.title, sourceText, finalDiff, requestedCount);
                Quiz newQuiz = new Quiz(selectedNote.id, title, finalDiff, generatedJson);
                long newQuizId = appDao.insertQuiz(newQuiz);
                newQuiz.id = (int) newQuizId;
                targetQuiz = newQuiz;
            } else {
                // If exists but count requested differs, regenerate and update DB
                List<AiQuizGenerator.Question> existing = new Gson().fromJson(targetQuiz.questionsJson, new TypeToken<List<AiQuizGenerator.Question>>(){}.getType());
                if (existing == null || existing.size() != requestedCount) {
                    String extracted = new com.example.successtudy.logic.FileStorageHelper(requireContext()).extractTextFromFile(selectedNote.fileUrl);
                    String sourceText = (extracted == null || extracted.isEmpty()) ? (selectedNote.description + " " + selectedNote.subject) : extracted;
                    String generatedJson = com.example.successtudy.logic.GeminiService.generateQuizJson(requireContext(), selectedNote.title, sourceText, finalDiff, requestedCount);
                    targetQuiz.questionsJson = generatedJson;
                    appDao.updateQuiz(targetQuiz);
                }
            }

            activeQuizId = targetQuiz.id;
            
            // Parse questions list
            questionList = new Gson().fromJson(targetQuiz.questionsJson, new TypeToken<List<AiQuizGenerator.Question>>(){}.getType());
            
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                btnStartQuiz.setEnabled(true);
                btnStartExamSim.setEnabled(true);
                if (questionList == null || questionList.isEmpty()) {
                    Toast.makeText(getContext(), "Failed to generate questions. Try another note.", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentQuestionIndex = 0;
                correctAnswers = 0;

                // Toggle visibility
                layoutQuizSetup.setVisibility(View.GONE);
                layoutQuizActive.setVisibility(View.VISIBLE);
                layoutQuizResults.setVisibility(View.GONE);

                // Set timer settings
                if (isExamSim) {
                    tvQuizTimer.setVisibility(View.VISIBLE);
                    pbQuizTimer.setVisibility(View.VISIBLE);
                    totalTimeSeconds = questionList.size() * 30; // 30s per question
                    secondsRemaining = totalTimeSeconds;
                    pbQuizTimer.setProgress(100);
                    timerHandler.postDelayed(timerRunnable, 1000);
                } else {
                    tvQuizTimer.setVisibility(View.GONE);
                    pbQuizTimer.setVisibility(View.GONE);
                }

                loadQuestion(currentQuestionIndex);
            });
        }).start();
    }

    private void loadQuestion(int index) {
        if (index >= questionList.size()) {
            finishQuizSession();
            return;
        }

        AiQuizGenerator.Question q = questionList.get(index);
        tvQuizProgressIndicator.setText("Question " + (index + 1) + " of " + questionList.size());
        tvQuestionText.setText(q.question);
        tvQuestionTypeBadge.setText(q.type.toUpperCase());

        // Reset inputs
        rgQuizOptions.clearCheck();
        etShortAnswerInput.setText("");

        if ("MCQ".equalsIgnoreCase(q.type) || "TF".equalsIgnoreCase(q.type)) {
            rgQuizOptions.setVisibility(View.VISIBLE);
            layoutShortAnswer.setVisibility(View.GONE);

            rbOpt1.setText(q.choices.get(0));
            rbOpt2.setText(q.choices.get(1));

            if (q.choices.size() > 2) {
                rbOpt3.setVisibility(View.VISIBLE);
                rbOpt3.setText(q.choices.get(2));
            } else {
                rbOpt3.setVisibility(View.GONE);
            }

            if (q.choices.size() > 3) {
                rbOpt4.setVisibility(View.VISIBLE);
                rbOpt4.setText(q.choices.get(3));
            } else {
                rbOpt4.setVisibility(View.GONE);
            }
        } else { // Short Answer
            rgQuizOptions.setVisibility(View.GONE);
            layoutShortAnswer.setVisibility(View.VISIBLE);
        }
    }

    private void submitActiveAnswer() {
        if (currentQuestionIndex >= questionList.size()) return;

        AiQuizGenerator.Question q = questionList.get(currentQuestionIndex);
        boolean answered = false;
        boolean isCorrect = false;

        if ("MCQ".equalsIgnoreCase(q.type) || "TF".equalsIgnoreCase(q.type)) {
            int selectedId = rgQuizOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                answered = true;
                int answerIndex = -1;
                if (selectedId == R.id.rbOpt1) answerIndex = 0;
                else if (selectedId == R.id.rbOpt2) answerIndex = 1;
                else if (selectedId == R.id.rbOpt3) answerIndex = 2;
                else if (selectedId == R.id.rbOpt4) answerIndex = 3;

                if (answerIndex == q.correctAnswerIndex) {
                    isCorrect = true;
                }
            }
        } else { // Short Answer
            String answer = etShortAnswerInput.getText().toString().trim().toLowerCase();
            if (!answer.isEmpty()) {
                answered = true;
                if (answer.contains(q.answerKeyword.toLowerCase()) || q.answerKeyword.toLowerCase().contains(answer)) {
                    isCorrect = true;
                }
            }
        }

        if (!answered) {
            Toast.makeText(getContext(), "Please supply an answer first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCorrect) {
            correctAnswers++;
        }

        currentQuestionIndex++;
        loadQuestion(currentQuestionIndex);
    }

    private void finishQuizSession() {
        timerHandler.removeCallbacks(timerRunnable);

        int scorePercentage = (correctAnswers * 100) / questionList.size();
        int userId = sessionManager.getCurrentUserId();

        // Save User Analytics in background
        new Thread(() -> {
            UserAnalytics analytics = new UserAnalytics(userId, activeQuizId, scorePercentage, 100, System.currentTimeMillis());
            appDao.insertAnalytics(analytics);

            // Update uploader balance slightly if student got 100% or participated (simulating engagement reward!)
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                // Display Results Screen
                layoutQuizActive.setVisibility(View.GONE);
                layoutQuizResults.setVisibility(View.VISIBLE);

                tvResultScore.setText(scorePercentage + "%");
                tvResultFraction.setText(correctAnswers + " of " + questionList.size() + " Correct");

                // Dynamic feedback
                if (scorePercentage == 100) {
                    tvResultFeedback.setText("Perfect Score! You're an expert. 🏆");
                } else if (scorePercentage >= 70) {
                    tvResultFeedback.setText("Great Job! Strong understanding. 🎓");
                } else {
                    tvResultFeedback.setText("Keep practicing! AI advises reviewing weak spots.");
                }

                // AI Weakness summary
                StringBuilder recommendation = new StringBuilder();
                if (scorePercentage < 100) {
                    recommendation.append("• Concepts to review: Focus on core definitions.\n");
                    recommendation.append("• Recommendation: Re-read the source note. Take a Simple-difficulty quiz to test fundamentals.");
                } else {
                    recommendation.append("• Analytics check: You've mastered all concepts in this document!\n");
                    recommendation.append("• Recommendation: Challenge yourself by uploading premium materials or trying Difficult settings.");
                }
                tvWeakAreasList.setText(recommendation.toString());
            });
        }).start();
    }

    private void resetToSetup() {
        layoutQuizSetup.setVisibility(View.VISIBLE);
        layoutQuizActive.setVisibility(View.GONE);
        layoutQuizResults.setVisibility(View.GONE);
        loadNotesList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
