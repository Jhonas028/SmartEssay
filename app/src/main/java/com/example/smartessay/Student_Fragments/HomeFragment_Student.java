package com.example.smartessay.Student_Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

public class HomeFragment_Student extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<EssayInfo> roomList;
    private Button btnJoinRoom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home__student, container, false);


// RecyclerView setup
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

// ✅ Initialize list before adapter
        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

// Load student essays
        loadStudentEssays();

        // Button setup (Join Room for student)
        btnJoinRoom = view.findViewById(R.id.btn_add_room); // ✅ updated id
        btnJoinRoom.setOnClickListener(v -> {
            // Inflate custom layout
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_add_classroom_student, null);

            EditText etRoomCode = dialogView.findViewById(R.id.etRoomCode); // ✅ fixed id
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnJoin = dialogView.findViewById(R.id.btnCreate);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false) // don’t dismiss by tapping outside
                    .create();

            btnCancel.setOnClickListener(view1 -> dialog.dismiss());

            btnJoin.setOnClickListener(view12 -> {
                String roomCode = etRoomCode.getText().toString().trim();

                Log.d("RoomCheck", "Entered code: " + roomCode);

                if (!roomCode.isEmpty()) {
                    DatabaseReference classroomsRef = FirebaseDatabase.getInstance(
                            "https://smartessay-79d91-default-rtdb.firebaseio.com/"
                    ).getReference("classrooms");

                    classroomsRef.orderByChild("room_code").equalTo(roomCode)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Log.d("FirebaseCheck", "Snapshot: " + snapshot);

                                    if (snapshot.exists()) {
                                        // ✅ Match found
                                        dialog.dismiss();

                                        Intent intent = new Intent(getContext(), Camera_Student.class);

                                        // Optional: send classroom info
                                        for (DataSnapshot classroom : snapshot.getChildren()) {
                                            String roomName = classroom.child("classroom_name").getValue(String.class);
                                            String code = classroom.child("room_code").getValue(String.class);

                                            intent.putExtra("roomName", roomName);
                                            intent.putExtra("roomCode", code);

                                            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                            String currentStudentId = prefs.getString("studentId", null);

                                            if (currentStudentId == null) {
                                                Toast.makeText(getContext(), "Student not logged in", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            intent.putExtra("roomName", roomName);
                                            intent.putExtra("roomCode", code);
                                            intent.putExtra("studentId", currentStudentId);      // ✅ from SharedPreferences
                                            intent.putExtra("classroomId", classroom.getKey());   // push key of classroom
                                        }

                                        startActivity(intent);
                                    } else {
                                        // ❌ No match
                                        Toast.makeText(getContext(), "Invalid Room Code", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("FirebaseError", error.getMessage());
                                }
                            });


                } else {
                    Toast.makeText(getContext(), "Please enter a room code", Toast.LENGTH_SHORT).show();
                }

            });

            dialog.show();
        });


        return view;
    }

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

        essayRef.orderByChild("student_id").equalTo(currentStudentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomList.clear();

                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String classroomName = essaySnap.child("classroom_name").getValue(String.class);
                            Long createdAt = essaySnap.child("created_at").getValue(Long.class);
                            String status = essaySnap.child("status").getValue(String.class);

                            EssayInfo essayInfo = new EssayInfo(
                                    classroomName != null ? classroomName : "Unknown",
                                    createdAt != null ? createdAt : 0,
                                    status != null ? status : "N/A"
                            );

                            roomList.add(essayInfo);
                        }

                        roomAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", error.getMessage());
                    }
                });
    }



    // Room model
// Model for essay submission
    public static class EssayInfo {
        private String classroomName;
        private long createdAt;
        private String status;

        public EssayInfo() {} // Needed for Firebase

        public EssayInfo(String classroomName, long createdAt, String status) {
            this.classroomName = classroomName;
            this.createdAt = createdAt;
            this.status = status;
        }

        public String getClassroomName() { return classroomName; }
        public long getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
    }


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

            // format timestamps into readable date/time
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a");
            String created = (essay.getCreatedAt() > 0) ? sdf.format(new java.util.Date(essay.getCreatedAt())) : "N/A";

            holder.textDateCreated.setText("created: " + created);
            holder.textStatus.setText("status: " + essay.getStatus());
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
                textStatus = itemView.findViewById(R.id.text_sname);
            }
        }
    }

}
