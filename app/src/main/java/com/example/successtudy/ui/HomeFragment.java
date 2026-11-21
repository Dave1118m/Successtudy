package com.example.successtudy.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.SessionManager;
import com.example.successtudy.ui.adapter.NoteAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvWelcomeUser, tvUserUni, tvRoleBadge;
    private TextView tvStreakCount, tvWalletBalance, tvAvgScore;
    private EditText etHomeSearch;
    private CardView cardQuickUpload, cardQuickQuiz, cardQuickMarket;
    private RecyclerView rvHomeRecommended;
    
    private SessionManager sessionManager;
    private AppDao appDao;
    private NoteAdapter recommendedAdapter;
    private List<Note> recommendedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        appDao = AppDatabase.getDatabase(requireContext()).appDao();

        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser);
        tvUserUni = view.findViewById(R.id.tvUserUni);
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvAvgScore = view.findViewById(R.id.tvAvgScore);
        etHomeSearch = view.findViewById(R.id.etHomeSearch);
        
        cardQuickUpload = view.findViewById(R.id.cardQuickUpload);
        cardQuickQuiz = view.findViewById(R.id.cardQuickQuiz);
        cardQuickMarket = view.findViewById(R.id.cardQuickMarket);
        rvHomeRecommended = view.findViewById(R.id.rvHomeRecommended);

        // Load Session info
        tvWelcomeUser.setText("Hello, " + sessionManager.getCurrentUserName() + "!");
        tvRoleBadge.setText(sessionManager.getCurrentUserRole().toUpperCase());

        // Setup RecyclerView vertically to show full study material cards
        rvHomeRecommended.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        rvHomeRecommended.setNestedScrollingEnabled(false);
        recommendedAdapter = new NoteAdapter(recommendedList, sessionManager.getCurrentUserId(), appDao, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onBuyClick(Note note) {
                navigateToQuiz(note);
            }

            @Override
            public void onTakeQuizClick(Note note) {
                navigateToQuiz(note);
            }
        });
        rvHomeRecommended.setAdapter(recommendedAdapter);

        // Load DB Statistics
        refreshDashboardData();

        // Bind Actions
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);

        cardQuickUpload.setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_upload);
        });

        cardQuickQuiz.setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_quiz);
        });

        cardQuickMarket.setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_marketplace);
        });

        etHomeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshDashboardData() {
        int userId = sessionManager.getCurrentUserId();
        new Thread(() -> {
            User user = appDao.getUserById(userId);
            List<Note> allNotes = appDao.getAllNotes();
            float average = appDao.getAverageScoreForUser(userId);

            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (user != null) {
                    tvUserUni.setText(user.university);
                }
                tvWalletBalance.setText(String.valueOf(allNotes != null ? allNotes.size() : 0));
                
                // Show average score
                if (average > 0) {
                    tvAvgScore.setText(String.format("%.0f%%", average));
                } else {
                    tvAvgScore.setText("N/A");
                }

                // Populate Recommended Feed (Show all notes: user's own + others)
                recommendedList.clear();
                if (allNotes != null) {
                    recommendedList.addAll(allNotes);
                }
                recommendedAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void filterNotes(String query) {
        if (query.isEmpty()) {
            refreshDashboardData();
            return;
        }

        List<Note> filtered = new ArrayList<>();
        for (Note n : recommendedList) {
            if (n.title.toLowerCase().contains(query.toLowerCase()) || 
                n.subject.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(n);
            }
        }
        recommendedList.clear();
        recommendedList.addAll(filtered);
        recommendedAdapter.notifyDataSetChanged();
    }

    private void buyNoteItem(Note note) {
        int userId = sessionManager.getCurrentUserId();
        new Thread(() -> {
            // Check buyer funds
            User buyer = appDao.getUserById(userId);
            if (buyer != null && buyer.balance >= note.price) {
                com.example.successtudy.logic.MarketplaceService.purchaseNote(appDao, userId, note.id);
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Purchased note successfully!", Toast.LENGTH_SHORT).show();
                    refreshDashboardData();
                });
            } else {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Insufficient balance to purchase this study note.", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void navigateToQuiz(Note note) {
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // Put arguments to let QuizFragment start immediately with this note
            QuizFragment quizFragment = new QuizFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("preselect_note_id", note.id);
            quizFragment.setArguments(bundle);
            
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, quizFragment, "Quiz")
                    .commit();
            bottomNav.getMenu().findItem(R.id.nav_quiz).setChecked(true);
        }
    }
}
