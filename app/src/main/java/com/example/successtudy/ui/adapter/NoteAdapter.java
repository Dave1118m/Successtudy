package com.example.successtudy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.successtudy.R;
import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<Note> noteList;
    private final int currentUserId;
    private final AppDao appDao;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onBuyClick(Note note);
        void onTakeQuizClick(Note note);
    }

    public NoteAdapter(List<Note> noteList, int currentUserId, AppDao appDao, OnNoteClickListener listener) {
        this.noteList = noteList;
        this.currentUserId = currentUserId;
        this.appDao = appDao;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.tvTitle.setText(note.title);
        holder.tvSubject.setText(note.subject != null ? note.subject.toUpperCase() : "STUDY MATERIAL");

        if (note.aiSummary != null && !note.aiSummary.trim().isEmpty()) {
            String cleanSnippet = note.aiSummary.replace("📌", "").replace("🔑", "").replace("💡", "").trim();
            if (cleanSnippet.length() > 140) {
                cleanSnippet = cleanSnippet.substring(0, 140) + "...";
            }
            holder.tvDesc.setText(cleanSnippet);
        } else if (note.description != null && !note.description.trim().isEmpty()) {
            holder.tvDesc.setText(note.description);
        } else {
            holder.tvDesc.setText("Tap 'Read Summary' to view the full AI study breakdown.");
        }

        holder.btnReadSummary.setOnClickListener(v -> showSummaryDialog(holder.itemView.getContext(), note));
        holder.btnStartQuiz.setOnClickListener(v -> {
            if (listener != null) listener.onTakeQuizClick(note);
        });

        holder.itemView.setOnClickListener(v -> showSummaryDialog(holder.itemView.getContext(), note));
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    private void showSummaryDialog(Context context, Note note) {
        String summary = (note.aiSummary != null && !note.aiSummary.trim().isEmpty())
                ? note.aiSummary
                : "💡 Summary:\n" + (note.description != null ? note.description : "No description available.");

        new AlertDialog.Builder(context)
                .setTitle("📖 " + note.title)
                .setMessage(summary)
                .setPositiveButton("🎯 Take Practice Quiz", (dialog, which) -> {
                    if (listener != null) listener.onTakeQuizClick(note);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTitle, tvDesc;
        Button btnReadSummary, btnStartQuiz;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.itemNoteSubject);
            tvTitle = itemView.findViewById(R.id.itemNoteTitle);
            tvDesc = itemView.findViewById(R.id.itemNoteDescription);
            btnReadSummary = itemView.findViewById(R.id.btnReadSummary);
            btnStartQuiz = itemView.findViewById(R.id.btnStartQuiz);
        }
    }
}
