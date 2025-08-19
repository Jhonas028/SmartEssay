package com.example.smartessay.Teacher_Fragments;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartessay.R;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment_Teacher extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;
    Button btnAddRoom;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            View dialogView = inflater.inflate(R.layout.dialog_add_classroom_teacher, null);

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
                    Toast.makeText(requireContext(), "Classroom Created: " + classroomName, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        });


        return view;
    }

    private void initializeRoomList() {
        roomList = new ArrayList<>();

        // Sample room data
        roomList.add(new Room("Room 101", "ABC123", "March 15, 2024", "10:30 AM", 25, 30));
        roomList.add(new Room("Room 102", "DEF456", "March 16, 2024", "2:15 PM", 18, 25));
        roomList.add(new Room("Room 103", "GHI789", "March 17, 2024", "9:00 AM", 30, 30));
        roomList.add(new Room("Mathematics Lab", "MATH01", "March 18, 2024", "11:45 AM", 15, 20));
        roomList.add(new Room("Science Lab", "SCI02", "March 19, 2024", "1:30 PM", 22, 28));
    }

    // Room data model class
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

        // Getters
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
                textAvailableStudents = itemView.findViewById(R.id.text_available_students);
            }
        }
    }
}

