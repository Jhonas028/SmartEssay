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
import android.widget.ImageButton;
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

// Fragment that shows student's essays and allows joining classrooms
public class HomePage_Student extends Fragment {

    private RecyclerView recyclerView;       // RecyclerView showing essays
    private RoomAdapter roomAdapter;         // Adapter for RecyclerView
    private List<EssayInfo> roomList;        // List holding essay data
    private ImageButton btnJoinRoom;              // Button to join new classroom

    private EditText editSearch;             // 🔹 Search input
    private List<EssayInfo> fullRoomList;    // 🔹 Full copy for search filtering

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home__student, container, false);

        // 🔹 Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🔹 Initialize list and adapter
        roomList = new ArrayList<>();
        fullRoomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

        // 🔹 Setup search input
        editSearch = view.findViewById(R.id.edit_search_prompt);
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 🔹 Filter RecyclerView whenever user types
                filterSearch(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 🔹 Load essays from Firebase when fragment opens
        loadStudentEssays();

        // 🔹 Join Room button opens dialog to input room code
        btnJoinRoom = view.findViewById(R.id.btn_add_room);
        btnJoinRoom.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_add_classroom_student, null);

            EditText etRoomCode = dialogView.findViewById(R.id.etRoomCode);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnJoin = dialogView.findViewById(R.id.btnCreate);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            // Cancel button closes dialog
            btnCancel.setOnClickListener(view1 -> dialog.dismiss());

            // Join button checks input and queries Firebase
            btnJoin.setOnClickListener(view12 -> {
                String roomCode = etRoomCode.getText().toString().trim();

                // 🔹 IF input is not empty
                if (!roomCode.isEmpty()) {
                    // 🔹 Reference Firebase "classrooms" node
                    DatabaseReference classroomsRef = FirebaseDatabase.getInstance(
                            "https://smartessay-79d91-default-rtdb.firebaseio.com/"
                    ).getReference("classrooms");

                    // 🔹 Query Firebase for classrooms with matching "room_code"
                    classroomsRef.orderByChild("room_code").equalTo(roomCode)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    // 🔹 IF classroom exists
                                    if (snapshot.exists()) {
                                        dialog.dismiss();

                                        Intent intent = new Intent(getContext(), Camera_Student.class);

                                        // Loop through classrooms found (usually 1)
                                        for (DataSnapshot classroom : snapshot.getChildren()) {
                                            String roomName = classroom.child("classroom_name").getValue(String.class);
                                            String code = classroom.child("room_code").getValue(String.class);

                                            // Get studentId from SharedPreferences
                                            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                            String currentStudentId = prefs.getString("studentId", null);

                                            // 🔹 IF student not logged in
                                            if (currentStudentId == null) {
                                                Toast.makeText(getContext(), "Student not logged in", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // 🔹 Pass data to Camera_Student
                                            intent.putExtra("roomName", roomName);
                                            intent.putExtra("roomCode", code);
                                            intent.putExtra("studentId", currentStudentId);
                                            intent.putExtra("classroomId", classroom.getKey());
                                        }

                                        startActivity(intent);

                                    } else {
                                        // 🔹 ELSE → room code not found
                                        Toast.makeText(getContext(), "Invalid Room Code", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("FirebaseError", error.getMessage());
                                }
                            });

                } else {
                    // 🔹 ELSE → input is empty
                    Toast.makeText(getContext(), "Please enter a room code", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        });

        // 🔹 Swipe to delete essays
        swipeDelete();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentEssays(); // refresh essays on return
    }

    // 🔹 Filter method for search bar
    private void filterSearch(String query) {
        roomList.clear();
        if (query.isEmpty()) {
            roomList.addAll(fullRoomList); // show all if empty
        } else {
            query = query.toLowerCase();
            for (EssayInfo essay : fullRoomList) {
                // 🔹 match classroom name with query
                if (essay.getClassroomName().toLowerCase().contains(query)) {
                    roomList.add(essay);
                }
            }
        }
        roomAdapter.notifyDataSetChanged();
    }

    public void swipeDelete(){
        // 🔹 Swipe-to-delete logic (left/right)
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // drag & drop not supported
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // 🔹 If invalid swipe position
                if (position < 0 || position >= roomList.size()) {
                    roomAdapter.notifyDataSetChanged();
                    return;
                }

                EssayInfo essayToDelete = roomList.get(position);

                // 🔹 Confirm delete dialog
                showYesNoDialog(
                        "Delete Essay",
                        "Are you sure you want to delete this essay?",
                        () -> {
                            DatabaseReference rootRef = FirebaseDatabase.getInstance(
                                    "https://smartessay-79d91-default-rtdb.firebaseio.com/"
                            ).getReference();

                            // Delete essay from "essay" node
                            rootRef.child("essay").child(essayToDelete.getEssayId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Also remove student from classroom_members
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
                                        roomAdapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                                    });
                        },
                        () -> roomAdapter.notifyItemChanged(position)
                );
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    // Draw red background
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

                    // Draw trash icon
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

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void showYesNoDialog(String title, String message, Runnable onYes, Runnable onCancel) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_yes_no, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnYes.setOnClickListener(v -> { if (onYes != null) onYes.run(); dialog.dismiss(); });
        btnNo.setOnClickListener(v -> { if (onCancel != null) onCancel.run(); dialog.dismiss(); });
        dialog.setOnCancelListener(d -> { if (onCancel != null) onCancel.run(); });

        dialog.show();
    }

    // 🔹 Load essays from Firebase for the current student
    private void loadStudentEssays() {
        roomList.clear();

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentStudentId = prefs.getString("studentId", null);

        if (currentStudentId == null) {
            Toast.makeText(getContext(), "Student not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference essayRef = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference("essay");

        // 🔹 Query Firebase for essays of this student
        essayRef.orderByChild("student_id").equalTo(currentStudentId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomList.clear();
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String essayId = essaySnap.getKey();
                            String classroomId = essaySnap.child("classroom_id").getValue(String.class);
                            String studentId = essaySnap.child("student_id").getValue(String.class);
                            String classroomName = essaySnap.child("classroom_name").getValue(String.class);
                            Long createdAt = essaySnap.child("created_at").getValue(Long.class);
                            String status = essaySnap.child("status").getValue(String.class);

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

                        // 🔹 Save full list for search filtering
                        fullRoomList.clear();
                        fullRoomList.addAll(roomList);

                        roomAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", error.getMessage());
                    }
                });
    }

    // 🔹 Essay model
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

    // 🔹 RecyclerView adapter
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

            holder.textRoomName.setText(essay.getClassroomName());

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a");
            String created = (essay.getCreatedAt() > 0) ? sdf.format(new java.util.Date(essay.getCreatedAt())) : "N/A";
            holder.textDateCreated.setText("Date submitted: " + created);

            holder.textStatus.setText("status: " + essay.getStatus());

            // 🟢 Set and color the status text
            String status = essay.getStatus();
            holder.textStatus.setText(status);

            if (status != null) {
                switch (status.toLowerCase()) {
                    case "pending":
                        holder.textStatus.setTextColor(Color.RED);    // 🔴 Pending
                        holder.textStatus.setText("PENDING");
                        break;
                    case "posted":
                        holder.textStatus.setTextColor(Color.parseColor("#00C853"));
                        holder.textStatus.setText("GRADED");
                        // 🟢 Posted
                        break;
                    default:
                        holder.textStatus.setTextColor(Color.BLACK);  // ⚫ Default
                        break;
                }
            } else {
                holder.textStatus.setTextColor(Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                if ("pending".equalsIgnoreCase(essay.getStatus())) {
                    Toast.makeText(v.getContext(),
                            "Your essay is still under review. Please wait for the teacher to post your score.",
                            Toast.LENGTH_SHORT).show();
                } else {
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
