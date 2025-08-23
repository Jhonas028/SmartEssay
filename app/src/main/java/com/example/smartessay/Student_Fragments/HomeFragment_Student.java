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
    private List<Room> roomList;
    private Button btnJoinRoom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home__student, container, false);

        // RecyclerView setup
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize room list (sample for now)
        initializeRoomList();

        // Adapter setup
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

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

    private void initializeRoomList() {
        roomList = new ArrayList<>();

        // Sample data (later you can load actual available classes)
        roomList.add(new Room("English 101", "ENG123", "March 15, 2024", "10:30 AM", 25, 30));
        roomList.add(new Room("History 201", "HIS456", "March 16, 2024", "2:15 PM", 18, 25));
        roomList.add(new Room("Mathematics Lab", "MATH01", "March 17, 2024", "9:00 AM", 20, 25));
    }

    // Room model
    public static class Room {
        private String roomName;
        private String roomCode;
        private String dateCreated;
        private String timeCreated;
        private int availableStudents;
        private int totalStudents;

        public Room(String roomName, String roomCode, String dateCreated,
                    String timeCreated, int availableStudents, int totalStudents) {
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.dateCreated = dateCreated;
            this.timeCreated = timeCreated;
            this.availableStudents = availableStudents;
            this.totalStudents = totalStudents;
        }

        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getDateCreated() { return dateCreated; }
        public String getTimeCreated() { return timeCreated; }
        public int getAvailableStudents() { return availableStudents; }
        public int getTotalStudents() { return totalStudents; }
    }

    // RecyclerView Adapter
    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private List<Room> roomList;

        public RoomAdapter(List<Room> roomList) {
            this.roomList = roomList;
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
            Room room = roomList.get(position);

            holder.textRoomName.setText(room.getRoomName());
            holder.textRoomCode.setText("Code: " + room.getRoomCode());
            holder.textDateCreated.setText("Created: " + room.getDateCreated());
            holder.textTimeCreated.setText("Time: " + room.getTimeCreated());
            holder.textAvailableStudents.setText("Available Students: " +
                    room.getAvailableStudents() + "/" + room.getTotalStudents());
        }

        @Override
        public int getItemCount() {
            return roomList.size();
        }

        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textRoomCode, textDateCreated, textTimeCreated, textAvailableStudents;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);

                textRoomName = itemView.findViewById(R.id.text_room_name);
                textRoomCode = itemView.findViewById(R.id.text_room_code);
                textDateCreated = itemView.findViewById(R.id.text_date_created);
                textTimeCreated = itemView.findViewById(R.id.text_time_created);
                textAvailableStudents = itemView.findViewById(R.id.students_name);
            }
        }
    }
}
