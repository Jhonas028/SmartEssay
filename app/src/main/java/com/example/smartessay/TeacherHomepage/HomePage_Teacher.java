package com.example.smartessay.TeacherHomepage;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.Map;

// Fragment showing the teacher's classrooms in a RecyclerView
public class HomePage_Teacher extends Fragment {

    private RecyclerView recyclerView; // List of classrooms
    private RoomAdapter roomAdapter;   // RecyclerView Adapter
    private List<Room> roomList;       // Data for the Adapter
    private DatabaseReference classroomsRef; // Firebase reference to classrooms
    private Button btnAddRoom;         // Button to add new classroom

    String teacherEmail; // Will hold teacher email passed from previous fragment/activity

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate fragment layout
        View view = inflater.inflate(R.layout.home_page_teacher, container, false);

        // Firebase reference pointing to "classrooms" node
        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize room list and adapter
        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList);
        recyclerView.setAdapter(roomAdapter);

        // Add Room button
        btnAddRoom = view.findViewById(R.id.btn_add_room);

        // Get arguments passed from parent FragmentHP_Teacher/activity
        Bundle args = getArguments();
        if (args != null) {
            teacherEmail = args.getString("teacherEmail"); // teacher email
            Log.d("HomePage_Teacher", "Email: " + teacherEmail);
        }


        //  click listener launching AddRoom_Teacher activity
        btnAddRoom.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddRoom_Teacher.class)));

        // Load classrooms from Firebase
        loadRoomsFromFirebase();

        // Handle clicks on each room and pass it to the next activity
        roomAdapter.setOnItemClickListener(room -> {
            Intent intent = new Intent(requireContext(), RoomDetails_Teacher.class);
            intent.putExtra("roomName", room.getRoomName());
            intent.putExtra("roomCode", room.getRoomCode());
            intent.putExtra("roomId", room.getRoomId());
            startActivity(intent);
        });

        //calling this method for deleting recycler view.
        swipeDelete();


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // This code will be executed when the fragment is resumed. Helps the code displays the updated data.
        // Clear old data safely
        roomList.clear();
        roomAdapter.notifyDataSetChanged(); // notify adapter before loading new data

        // Reload from Firebase
        loadRoomsFromFirebase();
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


    public void swipeDelete(){
        // Swipe left to delete a room
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // No drag & drop, so return false
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
               // Check if position is valid
                if (position < 0 || position >= roomList.size()) {
                    roomAdapter.notifyDataSetChanged(); // Reset swipe
                    return;
                }
                Log.d("SwipeDelete", "Swiped position: " + position);
                Log.d("RoomList", "RoomList: " + roomList.size());
                // Get room to delete
                Room roomToDelete = roomList.get(position);

                // Confirm deletion
                showYesNoDialog(
                        "Delete Room",
                        "Are you sure you want to delete this room?",
                        () -> {
                            // ✅ Yes → delete room
                            classroomsRef.child(roomToDelete.getRoomId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        if (position < roomList.size()) {
                                            roomList.remove(position);
                                            roomAdapter.notifyItemRemoved(position);
                                            loadRoomsFromFirebase();
                                            Toast.makeText(requireContext(), "Room deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        roomAdapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                                    });
                        },
                        () -> {
                            // ❌ No or canceled → restore item visually
                            roomAdapter.notifyItemChanged(position);
                        }
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

                    // Draw red background when swiping left
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
                return 0.7f; // Must swipe 70% to trigger delete
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    // 🔹 Load classrooms owned by this teacher from Firebase
    private void loadRoomsFromFirebase() {
        // Query classrooms where classroom_owner equals teacherEmail
        classroomsRef.orderByChild("classroom_owner").equalTo(teacherEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Clear current list before reloading
                        roomList.clear();

                        // Loop through each classroom
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String roomId = ds.getKey();
                            String roomName = ds.child("classroom_name").getValue(String.class);
                            String roomCode = ds.child("room_code").getValue(String.class);
                            String createdAt = ds.child("created_at").getValue(String.class);
                            String updatedAt = ds.child("updated_at").getValue(String.class);

                            // Optional: rubrics stored as Map in Firebase
                            Map<String, String> rubrics = null;
                            if (ds.child("rubrics").exists()) {
                                rubrics = (Map<String, String>) ds.child("rubrics").getValue();
                            }

                            // Add to local list
                            roomList.add(new Room(roomId, roomName, roomCode, createdAt, updatedAt, rubrics));
                        }

                        // ✅ Refresh the whole adapter once after all items are added
                        roomAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load rooms", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // 🔹 Room model class representing a classroom
    public static class Room {
        private String roomId;   // Firebase key
        private String roomName;
        private String roomCode;
        private String createdAt;
        private String updatedAt;
        private Map<String, String> rubrics;

        public Room() {} // Empty constructor required by Firebase

        // Full constructor
        public Room(String roomId, String roomName, String roomCode,
                    String createdAt, String updatedAt, Map<String, String> rubrics) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.rubrics = rubrics;
        }

        // Getters
        public String getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Map<String, String> getRubrics() { return rubrics; }
    }

    // 🔹 RecyclerView Adapter for Room list
    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private final List<Room> roomList;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Room room);
        }

        public void setOnItemClickListener(OnItemClickListener listener) { this.listener = listener; }

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

            // Display room info
            holder.textRoomName.setText(room.getRoomName());
            holder.textRoomCode.setText("Code: " + room.getRoomCode());
            holder.textCreatedAt.setText("Created: " + room.getCreatedAt());

            // Toggle rubrics visibility
            holder.btnShowRubrics.setOnClickListener(v -> {
                if (holder.textRubrics.getVisibility() == View.GONE) {
                    holder.textRubrics.setVisibility(View.VISIBLE);
                    holder.btnShowRubrics.setText("Hide Rubrics");
                } else {
                    holder.textRubrics.setVisibility(View.GONE);
                    holder.btnShowRubrics.setText("Show Rubrics");
                }
            });

            // Show rubrics text
            if (room.getRubrics() != null) {
                StringBuilder summary = new StringBuilder();
                for (Map.Entry<String, String> e : room.getRubrics().entrySet()) {
                    summary.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                }
                holder.textRubrics.setText(summary.toString().trim());
            } else {
                holder.textRubrics.setText("No Rubrics");
            }

            // 🔹 Count submitted essays (members) using Firebase
            DatabaseReference membersRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(room.getRoomId())
                    .child("classroom_members");

            membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long memberCount = snapshot.getChildrenCount();
                    holder.textUploads.setText("Submitted Essays: " + memberCount);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.textUploads.setText("Submitted Essays: ");
                }
            });

            // Click listener for whole item
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(room);
            });
        }

        @Override
        public int getItemCount() { return roomList.size(); }

        // ViewHolder class
        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textRoomCode, textCreatedAt, textUpdatedAt, textRubrics, textUploads;
            Button btnShowRubrics;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textRoomName = itemView.findViewById(R.id.text_room_name);
                textRoomCode = itemView.findViewById(R.id.text_room_code);
                textCreatedAt = itemView.findViewById(R.id.text_date_created);
                textUpdatedAt = itemView.findViewById(R.id.text_time_created);
                textRubrics = itemView.findViewById(R.id.text_rubrics);
                textUploads = itemView.findViewById(R.id.text_uploads);
                btnShowRubrics = itemView.findViewById(R.id.btn_show_rubrics);
            }
        }
    }

}
