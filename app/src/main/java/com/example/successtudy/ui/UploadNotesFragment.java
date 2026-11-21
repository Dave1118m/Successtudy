package com.example.successtudy.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.Quiz;
import com.example.successtudy.logic.FileStorageHelper;
import com.example.successtudy.logic.GeminiService;
import com.example.successtudy.logic.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UploadNotesFragment extends Fragment {

    private CardView cardFileDropZone;
    private TextView tvUploadStatus, tvUploadSubtitle;
    private EditText etNoteTitle, etNoteDesc;
    private RadioGroup rgQuizDifficultyUpload;
    private EditText etQuestionCount;
    private LinearLayout layoutAiLoading;
    private TextView tvAiLoadingStatus;
    private Button btnUploadNote;

    private String selectedFilePath = "";
    private String selectedFileName = "";

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SessionManager sessionManager;
    private AppDao appDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        appDao = AppDatabase.getDatabase(requireContext()).appDao();

        cardFileDropZone = view.findViewById(R.id.cardFileDropZone);
        tvUploadStatus = view.findViewById(R.id.tvUploadStatus);
        tvUploadSubtitle = view.findViewById(R.id.tvUploadSubtitle);
        etNoteTitle = view.findViewById(R.id.etNoteTitle);
        etNoteDesc = view.findViewById(R.id.etNoteDesc);
        rgQuizDifficultyUpload = view.findViewById(R.id.rgQuizDifficultyUpload);
        etQuestionCount = view.findViewById(R.id.etQuestionCount);
        layoutAiLoading = view.findViewById(R.id.layoutAiLoading);
        tvAiLoadingStatus = view.findViewById(R.id.tvAiLoadingStatus);
        btnUploadNote = view.findViewById(R.id.btnUploadNote);

        // File picker setup
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    String displayName = getFileName(uri);
                    if (displayName == null) displayName = "study_material.pdf";

                    String copiedPath = new FileStorageHelper(requireContext()).copyToExternalPublic(uri, displayName);
                    if (copiedPath != null) {
                        selectedFilePath = copiedPath;
                        selectedFileName = displayName;
                        tvUploadStatus.setText("Selected: " + displayName);
                        tvUploadStatus.setTextColor(getResources().getColor(R.color.teal_primary));
                        tvUploadSubtitle.setText("Tap to change file selection");

                        // Auto-fill title if empty
                        if (etNoteTitle.getText().toString().trim().isEmpty()) {
                            String nameWithoutExt = displayName.contains(".") 
                                    ? displayName.substring(0, displayName.lastIndexOf('.')) : displayName;
                            etNoteTitle.setText(nameWithoutExt.replace('_', ' ').replace('-', ' '));
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to read file", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        cardFileDropZone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {
                    "application/pdf",
                    "text/plain",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            filePickerLauncher.launch(intent);
        });

        btnUploadNote.setOnClickListener(v -> uploadAndProcessMaterial());
    }

    private void uploadAndProcessMaterial() {
        String title = etNoteTitle.getText().toString().trim();
        String pastedText = etNoteDesc.getText().toString().trim();

        if (selectedFilePath.isEmpty() && pastedText.isEmpty()) {
            Toast.makeText(getContext(), "Please select a study file or paste text notes first", Toast.LENGTH_LONG).show();
            return;
        }

        if (title.isEmpty()) {
            if (!selectedFileName.isEmpty()) {
                title = selectedFileName.contains(".") 
                        ? selectedFileName.substring(0, selectedFileName.lastIndexOf('.')) : selectedFileName;
            } else {
                title = "Study Material Notes";
            }
        }

        // Determine difficulty chosen by user
        String difficulty = "Medium";
        int checkedDiff = rgQuizDifficultyUpload.getCheckedRadioButtonId();
        if (checkedDiff == R.id.rbDiffSimple) {
            difficulty = "Simple";
        } else if (checkedDiff == R.id.rbDiffDifficult) {
            difficulty = "Difficult";
        }

        // Determine custom question count chosen by user
        int qCount = 5;
        try {
            String countStr = etQuestionCount.getText().toString().trim();
            if (!countStr.isEmpty()) {
                qCount = Integer.parseInt(countStr);
                if (qCount <= 0) qCount = 5;
                if (qCount > 50) qCount = 50;
            }
        } catch (Exception ignored) {}

        final String finalTitle = title;
        final String finalDiff = difficulty;
        final int finalCount = qCount;
        int uploaderId = sessionManager.getCurrentUserId();

        // Show loading
        layoutAiLoading.setVisibility(View.VISIBLE);
        btnUploadNote.setEnabled(false);
        tvAiLoadingStatus.setText("AI is analyzing material & creating your quiz...");

        new Thread(() -> {
            // Extract text from file
            String extracted = "";
            if (!selectedFilePath.isEmpty()) {
                extracted = new FileStorageHelper(requireContext()).extractTextFromFile(selectedFilePath);
            }

            // Combine extracted content + typed notes
            String fullSourceText = (extracted + "\n" + pastedText).trim();
            if (fullSourceText.isEmpty()) {
                fullSourceText = finalTitle;
            }

            // 1. AI Summary from the actual content
            String aiSummary = GeminiService.generateSummary(requireContext(), finalTitle, "Academic Material", fullSourceText);

            // 2. Insert Note into Room Database (0.00 price, no tags needed)
            Note note = new Note(finalTitle, "Study Notes", "", selectedFilePath, 5.0f, uploaderId, 0.00, pastedText);
            note.aiSummary = aiSummary;
            long noteId = appDao.insertNote(note);

            // 3. AI Quiz from the actual content (limited by user count & difficulty)
            String quizJson = GeminiService.generateQuizJson(requireContext(), finalTitle, fullSourceText, finalDiff, finalCount);
            Quiz quiz = new Quiz((int) noteId, finalTitle + " (" + finalDiff + " Quiz)", finalDiff, quizJson);
            appDao.insertQuiz(quiz);

            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                layoutAiLoading.setVisibility(View.GONE);
                btnUploadNote.setEnabled(true);
                Toast.makeText(getContext(), "AI successfully analyzed material & generated quiz!", Toast.LENGTH_LONG).show();

                // Redirect to Home Dashboard
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            });
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
