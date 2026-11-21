package com.example.successtudy.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.successtudy.R;
import com.example.successtudy.database.AppDatabase;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.User;
import com.example.successtudy.logic.MarketplaceService;
import com.example.successtudy.logic.SessionManager;
import com.example.successtudy.ui.adapter.NoteAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MarketplaceFragment extends Fragment {

    private EditText etMarketSearch;
    private Spinner spMarketFilterSubject;
    private RecyclerView rvMarketItems;

    private SessionManager sessionManager;
    private AppDao appDao;
    
    private NoteAdapter marketAdapter;
    private List<Note> fullMarketNotes = new ArrayList<>();
    private List<Note> displayedNotes = new ArrayList<>();
    
    private String[] filterSubjects = {"All Subjects", "Computer Science", "Biology", "History"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        appDao = AppDatabase.getDatabase(requireContext()).appDao();

        etMarketSearch = view.findViewById(R.id.etMarketSearch);
        spMarketFilterSubject = view.findViewById(R.id.spMarketFilterSubject);
        rvMarketItems = view.findViewById(R.id.rvMarketItems);

        // Subject filter adapter
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, filterSubjects);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMarketFilterSubject.setAdapter(spinAdapter);

        // Grid RecyclerView (2 Columns)
        rvMarketItems.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        marketAdapter = new NoteAdapter(displayedNotes, sessionManager.getCurrentUserId(), appDao, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onBuyClick(Note note) {
                promptPurchase(note);
            }

            @Override
            public void onTakeQuizClick(Note note) {
                navigateToQuiz(note);
            }
        });
        rvMarketItems.setAdapter(marketAdapter);

        // Load data
        loadMarketData();

        // Listeners
        spMarketFilterSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etMarketSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMarketData() {
        new Thread(() -> {
            List<Note> allNotes = appDao.getAllNotes();
            
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                fullMarketNotes.clear();
                fullMarketNotes.addAll(allNotes);
                applyFilters();
            });
        }).start();
    }

    private void applyFilters() {
        String search = etMarketSearch.getText().toString().trim().toLowerCase();
        int subjectPos = spMarketFilterSubject.getSelectedItemPosition();
        String selectedSub = filterSubjects[subjectPos];

        displayedNotes.clear();
        for (Note note : fullMarketNotes) {
            boolean matchesSearch = note.title.toLowerCase().contains(search) || 
                                    note.description.toLowerCase().contains(search) ||
                                    note.tags.toLowerCase().contains(search);
                                    
            boolean matchesSubject = "All Subjects".equalsIgnoreCase(selectedSub) || 
                                     note.subject.equalsIgnoreCase(selectedSub);

            if (matchesSearch && matchesSubject) {
                displayedNotes.add(note);
            }
        }
        marketAdapter.notifyDataSetChanged();
    }

    private void promptPurchase(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirm Transaction");
        builder.setMessage("Are you sure you want to purchase '" + note.title + "' for $" + String.format("%.2f", note.price) + "?\n\n(A 15% platform commission will be applied to this purchase)");
        builder.setPositiveButton("Buy now", (dialog, which) -> executePurchase(note));
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void executePurchase(Note note) {
        int buyerId = sessionManager.getCurrentUserId();
        new Thread(() -> {
            MarketplaceService.PurchaseResult result = MarketplaceService.purchaseNote(appDao, buyerId, note.id);
            
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                switch (result) {
                    case SUCCESS:
                        Toast.makeText(getContext(), "Note unlocked! You can now access details.", Toast.LENGTH_SHORT).show();
                        loadMarketData();
                        break;
                    case INSUFFICIENT_FUNDS:
                        Toast.makeText(getContext(), "Transaction declined: Insufficient funds in wallet balance.", Toast.LENGTH_LONG).show();
                        break;
                    case OWN_NOTE:
                        Toast.makeText(getContext(), "Error: Uploader cannot buy their own study note.", Toast.LENGTH_SHORT).show();
                        break;
                    case ALREADY_OWNED:
                        Toast.makeText(getContext(), "You already own this material.", Toast.LENGTH_SHORT).show();
                        break;
                    case USER_NOT_FOUND:
                        Toast.makeText(getContext(), "User details not found.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getContext(), "Payment gateway error. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        }).start();
    }

    private void navigateToQuiz(Note note) {
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
}
