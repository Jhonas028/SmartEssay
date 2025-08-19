package com.example.smartessay.Teacher_Fragments;

import android.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;

    private DatabaseReference classroomsRef;

    Button btnAddRoom;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");
        loadRoomsFromFirebase();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize room list with sample data
        initializeRoomList();

        // Set up adapter
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

        // Button logic
        btnAddRoom = view.findViewById(R.id.btn_add_room);
        btnAddRoom.setOnClickListener(v -> {
            // ✅ Use the inflater passed into onCreateView
            View dialogView = inflater.inflate(R.layout.dialog_add_classroom, null);

            EditText etClassroomName = dialogView.findViewById(R.id.etRoomName);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnCreate = dialogView.findViewById(R.id.btnCreate);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(view1 -> dialog.dismiss());

            btnCreate.setOnClickListener(view12 -> {
                String classroomName = etClassroomName.getText().toString().trim();
                if (!classroomName.isEmpty()) {
                    generateUniqueCode(roomCode -> {
                        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
                        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

                        Room newRoom = new Room(classroomName, roomCode, date, time, 0);

                        String roomId = classroomsRef.push().getKey();
                        classroomsRef.child(roomId).setValue(newRoom)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Room Created: " + classroomName, Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    });
                } else {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        });


        return view;
    }

    private void loadRoomsFromFirebase() {
        classroomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Room room = dataSnapshot.getValue(Room.class);
                    if (room != null) {
                        roomList.add(room);
                    }
                }
                roomAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load rooms", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeRoomList() {
        roomList = new ArrayList<>();

        // Sample room data
        roomList.add(new Room("Room 101", "ABC123", "March 15, 2024", "10:30 AM", 25));
    }

    // Room data model class
    public static class Room {
        private String roomName;
        private String roomCode;
        private String dateCreated;
        private String timeCreated;
        private int availableStudents;

        public Room() {} // required for Firebase

        public Room(String roomName, String roomCode, String dateCreated,
                    String timeCreated, int availableStudents) {
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.dateCreated = dateCreated;
            this.timeCreated = timeCreated;
            this.availableStudents = availableStudents;
        }

        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getDateCreated() { return dateCreated; }
        public String getTimeCreated() { return timeCreated; }
        public int getAvailableStudents() { return availableStudents; }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void generateUniqueCode(OnCodeGeneratedListener listener) {
        String code = generateRoomCode();

        classroomsRef.orderByChild("roomCode").equalTo(code)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            generateUniqueCode(listener); // try again
                        } else {
                            listener.onCodeGenerated(code);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error checking code", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    interface OnCodeGeneratedListener {
        void onCodeGenerated(String code);
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
                    .inflate(R.layout.teacher_room_item_layout, parent, false);
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
                    room.getAvailableStudents());
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
                textAvailableStudents = itemView.findViewById(R.id.text_available_students);
            }
        }
    }


}

