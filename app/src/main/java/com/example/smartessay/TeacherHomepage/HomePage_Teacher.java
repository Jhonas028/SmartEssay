package com.example.smartessay.TeacherHomepage;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomePage_Teacher extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;
    private DatabaseReference classroomsRef;
    private Button btnAddRoom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.home_page_teacher, container, false);



        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");

        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

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

        btnAddRoom.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddRoom_Teacher.class)));


        loadRoomsFromFirebase();

        roomAdapter.setOnItemClickListener(room -> {
            Intent intent = new Intent(requireContext(), RoomDetails_Teacher.class);
            intent.putExtra("roomName", room.getRoomName());  // pass room name
            intent.putExtra("roomCode", room.getRoomCode()); // pass the entire room object
            intent.putExtra("roomId", room.getRoomId());
            startActivity(intent);
        });

        // Attach ItemTouchHelper for swipe actions
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // We are not doing drag & drop, so return false
                return false;
            }

            @Override
            //delete function
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (position < 0 || position >= roomList.size()) {
                    roomAdapter.notifyDataSetChanged(); // reset swipe
                    return;
                }

                Room roomToDelete = roomList.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Room")
                        .setMessage("Are you sure you want to delete this room?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            classroomsRef.child(roomToDelete.getRoomId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        if (position < roomList.size()) {
                                            roomList.remove(position);
                                            roomAdapter.notifyItemRemoved(position);
                                            Toast.makeText(requireContext(), "Room deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        roomAdapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            roomAdapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .show();
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.7f; // swipe must travel 70% of item width
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 1f; // slower swipe allowed
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 1f; // slower swipe allowed
            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        return view;
    }

    private void loadRoomsFromFirebase() {
        classroomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String roomId = ds.getKey();
                    String roomName = ds.child("classroom_name").getValue(String.class);
                    String roomCode = ds.child("room_code").getValue(String.class);
                    String createdAt = ds.child("created_at").getValue(String.class);
                    String updatedAt = ds.child("updated_at").getValue(String.class);

                    Map<String, String> rubrics = null;
                    if (ds.child("rubrics").exists()) {
                        rubrics = (Map<String, String>) ds.child("rubrics").getValue();
                    }

                    roomList.add(new Room(roomId, roomName, roomCode, createdAt, updatedAt, rubrics));
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
        private String roomId;   // Firebase key (-OYL4rE...)
        private String roomName;
        private String roomCode;
        private String createdAt;
        private String updatedAt;
        private Map<String, String> rubrics;

        public Room() {
            // empty constructor needed for Firebase
        }

        // ✅ Full constructor including roomId
        public Room(String roomId, String roomName, String roomCode,
                    String createdAt, String updatedAt, Map<String, String> rubrics) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.rubrics = rubrics;
        }

        // ✅ Old constructor without roomId (optional, but avoids "cannot resolve constructor")
        public Room(String roomName, String roomCode, String createdAt,
                    String updatedAt, Map<String, String> rubrics) {
            this(null, roomName, roomCode, createdAt, updatedAt, rubrics);
        }

        // Getters
        public String getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Map<String, String> getRubrics() { return rubrics; }
    }



    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private final List<Room> roomList;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Room room);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }
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
           // holder.text_room_id.setText("Room id: " + room.getRoomId());

            holder.btnShowRubrics.setOnClickListener(v -> {
                if (holder.textRubrics.getVisibility() == View.GONE) {
                    holder.textRubrics.setVisibility(View.VISIBLE);
                    holder.btnShowRubrics.setText("Hide Rubrics");
                } else {
                    holder.textRubrics.setVisibility(View.GONE);
                    holder.btnShowRubrics.setText("Show Rubrics");
                }
            });

            if (room.getRubrics() != null) {
                StringBuilder summary = new StringBuilder();
                for (Map.Entry<String, String> e : room.getRubrics().entrySet()) {
                    summary.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                }
                holder.textRubrics.setText(summary.toString().trim());
            } else {
                holder.textRubrics.setText("No Rubrics");
            }

            // ---- NEW: Load classroom member count ----
            DatabaseReference membersRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(room.getRoomId())
                    .child("classroom_members");

            membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long memberCount = snapshot.getChildrenCount();
                    holder.textUploads.setText("Submitted Essays: " + memberCount); // replace 30 with max if you have it
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.textUploads.setText("Submitted Essays: ");
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(room);
            });
        }


        @Override
        public int getItemCount() { return roomList.size(); }

        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textRoomCode, textCreatedAt, textUpdatedAt, textRubrics,text_room_id,textUploads;
            Button btnShowRubrics;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textRoomName = itemView.findViewById(R.id.text_room_name);
                textRoomCode = itemView.findViewById(R.id.text_room_code);
                textCreatedAt = itemView.findViewById(R.id.text_date_created);
                textUpdatedAt = itemView.findViewById(R.id.text_time_created);
                textRubrics = itemView.findViewById(R.id.text_rubrics);
                //text_room_id = itemView.findViewById(R.id.text_room_id);
                textUploads = itemView.findViewById(R.id.text_uploads);
                btnShowRubrics = itemView.findViewById(R.id.btn_show_rubrics);
            }
        }
    }

}
