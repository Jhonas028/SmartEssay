package com.example.smartessay.Archive;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class FragmentArchive_Teacher extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;
    private DatabaseReference classroomsRef;
    private String teacherEmail;

    public FragmentArchive_Teacher() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive__teacher, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(requireContext(), roomList);
        recyclerView.setAdapter(roomAdapter);

        // ✅ Get the teacher email from arguments
        if (getArguments() != null) {
            teacherEmail = getArguments().getString("teacherEmail");
            Log.d("ArchiveDebug", "Teacher email received: " + teacherEmail);
        } else {
            Log.d("ArchiveDebug", "No teacherEmail argument received!");
        }

        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");
        loadArchivedRooms();

        enableSwipeToUnarchive();

        return view;
    }

    private void loadArchivedRooms() {
        classroomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    // 🔍 Raw snapshot for debugging
                    Log.d("ArchiveDebug", "Raw data: " + data.getValue());

                    Room room = data.getValue(Room.class);

                    if (room != null) {

                        room.setRoomId(data.getKey());

                        Log.d("ArchiveDebug", "Room found: "
                                + room.getClassroom_name() + " | status=" + room.getStatus()
                                + " | owner=" + room.getClassroom_owner());
                    } else {
                        Log.d("ArchiveDebug", "⚠️ Room object is null for key: " + data.getKey());
                    }

                    // ✅ Safe and case-insensitive comparison
                    if (room != null
                            && "archived".equalsIgnoreCase(room.getStatus())
                            && teacherEmail != null
                            && teacherEmail.equalsIgnoreCase(room.getClassroom_owner())) {

                        roomList.add(room);
                    }
                }

                roomAdapter.notifyDataSetChanged();
                Log.d("ArchiveDebug", "✅ Total archive rooms loaded: " + roomList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Failed to load archives: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showYesNoDialog(String title, String message, Runnable onYes, Runnable onCancel) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_yes_no, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnNo.setOnClickListener(v -> {
            if (onCancel != null) onCancel.run();
            dialog.dismiss();
        });

        btnYes.setOnClickListener(v -> {
            onYes.run();
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> {
            if (onCancel != null) onCancel.run();
        });

        dialog.show();
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

                // ✅ Prevent invalid positions
                if (position < 0 || position >= roomList.size()) {
                    roomAdapter.notifyDataSetChanged();
                    return;
                }

                final Room room = roomList.get(position);
                if (room == null) {
                    roomAdapter.notifyItemChanged(position);
                    return;
                }

                // Confirm with the custom dialog
                showYesNoDialog(
                        "Unarchive Room",
                        "Do you want to unarchive this room?",
                        () -> {
                            String roomId = room.getRoomId();
                            if (roomId == null || roomId.isEmpty()) {
                                Toast.makeText(requireContext(), "Invalid room id", Toast.LENGTH_SHORT).show();
                                roomAdapter.notifyItemChanged(position);
                                return;
                            }

                            DatabaseReference roomRef = classroomsRef.child(roomId);
                            roomRef.child("status").setValue("active")
                                    .addOnSuccessListener(aVoid -> {
                                        // ✅ Safe removal check before modifying the list
                                        if (position >= 0 && position < roomList.size()) {
                                            roomList.remove(position);
                                            roomAdapter.notifyItemRemoved(position);
                                        } else {
                                            roomAdapter.notifyDataSetChanged();
                                        }

                                        Toast.makeText(requireContext(),
                                                "Room unarchived successfully!",
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        roomAdapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(),
                                                "Failed to unarchive: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        },
                        // CANCEL: restore item
                        () -> roomAdapter.notifyItemChanged(position)
                );
            }


            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Optional visual: green background + icon while swiping left
                try {
                    new it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator.Builder(
                            c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                            .addSwipeLeftBackgroundColor(
                                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.archived))
                            .addSwipeLeftActionIcon(R.drawable.outline_archive_25) // provide icon or replace with android drawable
                            .create()
                            .decorate();
                } catch (Exception ex) {
                    // ignore decorator errors (library missing) and continue
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }






}
