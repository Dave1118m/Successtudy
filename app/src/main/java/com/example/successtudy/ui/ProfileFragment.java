package com.example.successtudy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.Transaction;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.database.entity.UserAnalytics;
import com.example.successtudy.logic.SessionManager;
import com.example.successtudy.ui.adapter.NoteAdapter;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileUniversity;
    private TextView tvProfileBalance, tvProfileEarnings;
    private Button btnLogout;
    private RecyclerView rvProfileBadges, rvProfileUploadedNotes;

    private SessionManager sessionManager;
    private AppDao appDao;
    
    private NoteAdapter uploadedAdapter;
    private List<Note> uploadedList = new ArrayList<>();
    
    private List<BadgeItem> badgeList = new ArrayList<>();
    private BadgeAdapter badgeAdapter;

    public static class BadgeItem {
        public String name;
        public String desc;
        public boolean isUnlocked;

        public BadgeItem(String name, String desc, boolean isUnlocked) {
            this.name = name;
            this.desc = desc;
            this.isUnlocked = isUnlocked;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        appDao = AppDatabase.getDatabase(requireContext()).appDao();

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileUniversity = view.findViewById(R.id.tvProfileUniversity);
        tvProfileBalance = view.findViewById(R.id.tvProfileBalance);
        tvProfileEarnings = view.findViewById(R.id.tvProfileEarnings);
        btnLogout = view.findViewById(R.id.btnLogout);
        rvProfileBadges = view.findViewById(R.id.rvProfileBadges);
        rvProfileUploadedNotes = view.findViewById(R.id.rvProfileUploadedNotes);

        // Populate baseline text
        tvProfileName.setText(sessionManager.getCurrentUserName());
        tvProfileEmail.setText(sessionManager.getCurrentUserEmail());

        // Setup horizontal Badges RecyclerView
        rvProfileBadges.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        badgeAdapter = new BadgeAdapter(badgeList);
        rvProfileBadges.setAdapter(badgeAdapter);

        // Setup vertical Uploaded notes RecyclerView
        rvProfileUploadedNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        uploadedAdapter = new NoteAdapter(uploadedList, sessionManager.getCurrentUserId(), appDao, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onBuyClick(Note note) {}

            @Override
            public void onTakeQuizClick(Note note) {
                // Open quiz
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
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
        });
        rvProfileUploadedNotes.setAdapter(uploadedAdapter);

        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), WelcomeActivity.class));
            requireActivity().finish();
        });

        loadProfileStats();
    }

    private void loadProfileStats() {
        int userId = sessionManager.getCurrentUserId();
        new Thread(() -> {
            User user = appDao.getUserById(userId);
            List<Transaction> sales = appDao.getTransactionsForSeller(userId);
            List<Transaction> purchases = appDao.getTransactionsForBuyer(userId);
            List<UserAnalytics> attempts = appDao.getAnalyticsForUser(userId);
            List<Note> uploadedNotes = appDao.getNotesByUploader(userId);

            // Calculate earnings: Sum (notePrice * 0.85) from all sales
            double earnings = 0.0;
            for (Transaction tx : sales) {
                earnings += tx.amount * 0.85; // 85% payout
            }

            // Calculate badges
            boolean streakUnlocked = attempts.size() > 0;
            boolean perfectScoreUnlocked = false;
            for (UserAnalytics ua : attempts) {
                if (ua.score == 100) {
                    perfectScoreUnlocked = true;
                    break;
                }
            }
            boolean marketPioneerUnlocked = purchases.size() > 0 || sales.size() > 0;

            final double finalEarnings = earnings;
            final boolean finalStreak = streakUnlocked;
            final boolean finalPerfect = perfectScoreUnlocked;
            final boolean finalMarket = marketPioneerUnlocked;

            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (user != null) {
                    tvProfileBalance.setText(String.format("$%.2f", user.balance));
                    tvProfileUniversity.setText(user.university);
                }
                tvProfileEarnings.setText(String.format("$%.2f", finalEarnings));

                // Populate badges list
                badgeList.clear();
                badgeList.add(new BadgeItem("Scholar Streak", "Take your first practice quiz", finalStreak));
                badgeList.add(new BadgeItem("Perfect Quiz", "Scored 100% correct answers", finalPerfect));
                badgeList.add(new BadgeItem("Market Pioneer", "Completed buy/sell notes transactions", finalMarket));
                badgeAdapter.notifyDataSetChanged();

                // Populate uploaded notes list
                uploadedList.clear();
                uploadedList.addAll(uploadedNotes);
                uploadedAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    // --- Inline Badge Adapter ---
    private static class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {
        private final List<BadgeItem> list;

        public BadgeAdapter(List<BadgeItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
            return new BadgeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
            BadgeItem item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);

            if (item.isUnlocked) {
                holder.ivIcon.setAlpha(1.0f);
                holder.tvName.setAlpha(1.0f);
                holder.tvDesc.setAlpha(1.0f);
            } else {
                holder.ivIcon.setAlpha(0.2f);
                holder.tvName.setAlpha(0.4f);
                holder.tvDesc.setAlpha(0.4f);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class BadgeViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc;
            View ivIcon;

            public BadgeViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvBadgeName);
                tvDesc = itemView.findViewById(R.id.tvBadgeDesc);
                ivIcon = itemView.findViewById(R.id.ivBadgeIcon);
            }
        }
    }
}
