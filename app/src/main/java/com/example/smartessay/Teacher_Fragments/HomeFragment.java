package com.example.smartessay.Teacher_Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartessay.R;
import com.example.smartessay.TeacherHomepage.AddRoomActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;
    private DatabaseReference classroomsRef;
    private Button btnAddRoom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");

        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

        btnAddRoom = view.findViewById(R.id.btn_add_room);
        btnAddRoom.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddRoomActivity.class)));

        loadRoomsFromFirebase();

        return view;
    }

    private void loadRoomsFromFirebase() {
        classroomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String roomName = ds.child("classroom_name").getValue(String.class);
                    String roomCode = ds.child("room_code").getValue(String.class);
                    String createdAt = ds.child("created_at").getValue(String.class);
                    String updatedAt = ds.child("updated_at").getValue(String.class);

                    Map<String, String> rubrics = null;
                    if (ds.child("rubrics").exists()) {
                        rubrics = (Map<String, String>) ds.child("rubrics").getValue();
                    }

                    roomList.add(new Room(roomName, roomCode, createdAt, updatedAt, rubrics));
                }
                roomAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load rooms", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class Room {
        private String roomName;
        private String roomCode;
        private String createdAt;
        private String updatedAt;
        private Map<String, String> rubrics;

        public Room() {}
        public Room(String roomName, String roomCode, String createdAt, String updatedAt,
                    Map<String, String> rubrics) {
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.rubrics = rubrics;
        }

        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Map<String, String> getRubrics() { return rubrics; }
    }

    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private final List<Room> roomList;

        public RoomAdapter(List<Room> roomList) { this.roomList = roomList; }

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
            holder.textCreatedAt.setText("Created: " + room.getCreatedAt());
            holder.textUpdatedAt.setText("Updated: " + room.getUpdatedAt());

            if (room.getRubrics() != null) {
                StringBuilder summary = new StringBuilder();
                for (Map.Entry<String, String> e : room.getRubrics().entrySet()) {
                    summary.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                }
                holder.textRubrics.setText(summary.toString().trim());
            } else {
                holder.textRubrics.setText("No Rubrics");
            }
        }

        @Override
        public int getItemCount() { return roomList.size(); }

        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textRoomCode, textCreatedAt, textUpdatedAt, textRubrics;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textRoomName = itemView.findViewById(R.id.text_room_name);
                textRoomCode = itemView.findViewById(R.id.text_room_code);
                textCreatedAt = itemView.findViewById(R.id.text_date_created);
                textUpdatedAt = itemView.findViewById(R.id.text_time_created);
                textRubrics = itemView.findViewById(R.id.text_rubrics);
            }
        }
    }
}
