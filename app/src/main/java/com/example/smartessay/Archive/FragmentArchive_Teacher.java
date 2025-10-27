package com.example.smartessay.Archive;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class FragmentArchive_Teacher extends Fragment {

    private RecyclerView recyclerView;
    private TextView noArchivedText;
    private ArchivedClassAdapter adapter;
    private List<ArchivedClass> archivedClassList;
    private DatabaseReference databaseReference;
    private String teacherEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive__teacher, container, false);

        // Initialize
        recyclerView = view.findViewById(R.id.recyclerArchivedClasses);
        noArchivedText = view.findViewById(R.id.no_archived_rooms_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        archivedClassList = new ArrayList<>();
        adapter = new ArchivedClassAdapter(getContext(), archivedClassList);
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            teacherEmail = getArguments().getString("teacherEmail");
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("archived_classrooms");

        loadArchivedClasses();
        enableSwipeToUnarchive(); // 👈 enable swipe feature

        return view;
    }

    private void loadArchivedClasses() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                archivedClassList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ArchivedClass archivedClass = dataSnapshot.getValue(ArchivedClass.class);
                    if (archivedClass != null && archivedClass.getClassroom_owner().equals(teacherEmail)) {
                        archivedClass.setKey(dataSnapshot.getKey()); // store Firebase key
                        archivedClassList.add(archivedClass);
                    }
                }

                if (archivedClassList.isEmpty()) {
                    noArchivedText.setVisibility(View.VISIBLE);
                } else {
                    noArchivedText.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableSwipeToUnarchive() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ArchivedClass archivedClass = archivedClassList.get(position);

                DatabaseReference archivedRef = FirebaseDatabase.getInstance()
                        .getReference("archived_classrooms")
                        .child(archivedClass.getKey());

                DatabaseReference activeRef = FirebaseDatabase.getInstance()
                        .getReference("classrooms")
                        .push(); // new classroom entry

                activeRef.setValue(archivedClass)
                        .addOnSuccessListener(aVoid -> {
                            archivedRef.removeValue();
                            Toast.makeText(getContext(), "Room unarchived successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to unarchive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                archivedClassList.remove(position);
                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(getContext(), R.color.archived))
                        .addSwipeLeftActionIcon(R.drawable.outline_archive_25)
                        .addSwipeLeftLabel("Unarchive")
                        .setSwipeLeftLabelColor(Color.WHITE)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.7f; // Optional: require 70% swipe
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

}
