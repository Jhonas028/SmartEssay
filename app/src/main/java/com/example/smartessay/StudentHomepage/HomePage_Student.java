package com.example.smartessay.StudentHomepage;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomePage_Student extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<EssayInfo> roomList;
    private Button btnJoinRoom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home__student, container, false);

        // 🔹 Setup RecyclerView (list of essays joined by student)
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🔹 Initialize the list + adapter
        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

        // 🔹 Load essays from Firebase when screen starts
        loadStudentEssays();

        // 🔹 Join Room button (student enters a code to join)
        btnJoinRoom = view.findViewById(R.id.btn_add_room);
        btnJoinRoom.setOnClickListener(v -> {
            // Custom dialog where student types the room code
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_add_classroom_student, null);

            EditText etRoomCode = dialogView.findViewById(R.id.etRoomCode);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnJoin = dialogView.findViewById(R.id.btnCreate);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            btnCancel.setOnClickListener(view1 -> dialog.dismiss());

            btnJoin.setOnClickListener(view12 -> {
                String roomCode = etRoomCode.getText().toString().trim();

                // IF input is not empty
                if (!roomCode.isEmpty()) {
                    // 🔹 Reference the "classrooms" node in Firebase
                    DatabaseReference classroomsRef = FirebaseDatabase.getInstance(
                            "https://smartessay-79d91-default-rtdb.firebaseio.com/"
                    ).getReference("classrooms");

                    // 🔹 Firebase query: search classrooms where "room_code" equals the input
                    classroomsRef.orderByChild("room_code").equalTo(roomCode)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    // IF at least one classroom with that code exists
                                    if (snapshot.exists()) {
                                        dialog.dismiss();

                                        // Move student to Camera_Student activity
                                        Intent intent = new Intent(getContext(), Camera_Student.class);

                                        // Loop through matching classrooms
                                        for (DataSnapshot classroom : snapshot.getChildren()) {
                                            // Get classroom details from Firebase
                                            String roomName = classroom.child("classroom_name").getValue(String.class);
                                            String code = classroom.child("room_code").getValue(String.class);

                                            // Get current student ID from SharedPreferences
                                            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                            String currentStudentId = prefs.getString("studentId", null);

                                            // IF studentId is not found → show error
                                            if (currentStudentId == null) {
                                                Toast.makeText(getContext(), "Student not logged in", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Pass data to Camera_Student
                                            intent.putExtra("roomName", roomName);
                                            intent.putExtra("roomCode", code);
                                            intent.putExtra("studentId", currentStudentId);
                                            intent.putExtra("classroomId", classroom.getKey()); // Firebase key
                                        }

                                        startActivity(intent);

                                    } else {
                                        // ELSE → No classroom found with that code
                                        Toast.makeText(getContext(), "Invalid Room Code", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    // Called if Firebase fails (ex: no internet)
                                    Log.e("FirebaseError", error.getMessage());
                                }
                            });

                } else {
                    // ELSE → Input is empty
                    Toast.makeText(getContext(), "Please enter a room code", Toast.LENGTH_SHORT).show();
                }

            });

            dialog.show();
        });

        //calling swipe to delete function
        swipeDelete();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentEssays(); // refresh essays when coming back
    }

    public void swipeDelete(){
        // 🔹 Swipe-to-delete essay logic
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // not supporting drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // IF invalid position, cancel swipe
                if (position < 0 || position >= roomList.size()) {
                    roomAdapter.notifyDataSetChanged();
                    return;
                }

                // Essay chosen for deletion
                EssayInfo essayToDelete = roomList.get(position);

                // Confirm dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Essay")
                        .setMessage("Are you sure you want to delete this essay?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            DatabaseReference rootRef = FirebaseDatabase.getInstance(
                                    "https://smartessay-79d91-default-rtdb.firebaseio.com/"
                            ).getReference();

                            // 🔹 First delete essay from "essay" node
                            rootRef.child("essay").child(essayToDelete.getEssayId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // 🔹 Then remove student from classroom_members
                                        rootRef.child("classrooms")
                                                .child(essayToDelete.getClassroomId())
                                                .child("classroom_members")
                                                .child(essayToDelete.getStudentId())
                                                .removeValue()
                                                .addOnSuccessListener(v -> {
                                                    roomList.remove(essayToDelete);
                                                    roomAdapter.notifyDataSetChanged();
                                                    Toast.makeText(requireContext(), "Essay and membership deleted", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(requireContext(), "Essay deleted but membership delete failed", Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Essay delete failed → restore swipe
                                        roomAdapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            roomAdapter.notifyItemChanged(position); // cancel swipe
                            dialog.dismiss();
                        })
                        .setOnCancelListener(dialog -> {
                            roomAdapter.notifyItemChanged(position); // restore item
                        })
                        .show();
            }

            // 🔹 Draw red background + trash icon while swiping left
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    // Red background
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

                    // Trash icon
                    Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.7f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 0.5f;
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 0.5f;
            }
        };

        // Attach swipe-to-delete to RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // 🔹 Load essays for current student from Firebase
    private void loadStudentEssays() {
        roomList.clear();

        // Get logged-in studentId
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentStudentId = prefs.getString("studentId", null);

        if (currentStudentId == null) {
            Toast.makeText(getContext(), "Student not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Reference "essay" node in Firebase
        DatabaseReference essayRef = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference("essay");

        // 🔹 Firebase query: get essays where "student_id" equals currentStudentId
        essayRef.orderByChild("student_id").equalTo(currentStudentId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomList.clear();

                        // Loop through each essay found
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String essayId = essaySnap.getKey();
                            String classroomId = essaySnap.child("classroom_id").getValue(String.class);
                            String studentId = essaySnap.child("student_id").getValue(String.class);
                            String classroomName = essaySnap.child("classroom_name").getValue(String.class);
                            Long createdAt = essaySnap.child("created_at").getValue(Long.class);
                            String status = essaySnap.child("status").getValue(String.class);

                            // Build EssayInfo object
                            EssayInfo essayInfo = new EssayInfo(
                                    essayId,
                                    classroomId != null ? classroomId : "",
                                    studentId != null ? studentId : "",
                                    classroomName != null ? classroomName : "Unknown",
                                    createdAt != null ? createdAt : 0,
                                    status != null ? status : "N/A"
                            );

                            roomList.add(essayInfo);
                        }

                        // Refresh RecyclerView
                        roomAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", error.getMessage());
                    }
                });
    }

    // 🔹 Essay model (represents data from Firebase)
    public static class EssayInfo {
        private String essayId;
        private String classroomId;
        private String studentId;
        private String classroomName;
        private long createdAt;
        private String status;

        public EssayInfo() {}

        public EssayInfo(String essayId, String classroomId, String studentId,
                         String classroomName, long createdAt, String status) {
            this.essayId = essayId;
            this.classroomId = classroomId;
            this.studentId = studentId;
            this.classroomName = classroomName;
            this.createdAt = createdAt;
            this.status = status;
        }

        public String getEssayId() { return essayId; }
        public String getClassroomId() { return classroomId; }
        public String getStudentId() { return studentId; }
        public String getClassroomName() { return classroomName; }
        public long getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
    }

    // 🔹 RecyclerView adapter for showing essays
    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private List<EssayInfo> roomList;

        public RoomAdapter(List<EssayInfo> roomList) {
            this.roomList = (roomList != null) ? roomList : new ArrayList<>();
        }

        @NonNull
        @Override
        public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.student_room_item_layout, parent, false);
            return new RoomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
            EssayInfo essay = roomList.get(position);

            // Set classroom name
            holder.textRoomName.setText(essay.getClassroomName());

            // Format creation date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a");
            String created = (essay.getCreatedAt() > 0) ? sdf.format(new java.util.Date(essay.getCreatedAt())) : "N/A";
            holder.textDateCreated.setText("created: " + created);

            // Show essay status
            holder.textStatus.setText("status: " + essay.getStatus());

            // 🔹 When student clicks essay
            holder.itemView.setOnClickListener(v -> {
                // IF status is "pending"
                if ("pending".equalsIgnoreCase(essay.getStatus())) {
                    Toast.makeText(v.getContext(),
                            "Your essay is still under review. Please wait for the teacher to post your score.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // ELSE → open EssayResult_Student to show result
                    Intent intent = new Intent(v.getContext(), EssayResult_Student.class);
                    intent.putExtra("essayId", essay.getEssayId());
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return roomList != null ? roomList.size() : 0;
        }

        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textDateCreated, textStatus;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textRoomName = itemView.findViewById(R.id.text_room_name);
                textDateCreated = itemView.findViewById(R.id.text_date_created);
                textStatus = itemView.findViewById(R.id.text_status);
            }
        }
    }
}
