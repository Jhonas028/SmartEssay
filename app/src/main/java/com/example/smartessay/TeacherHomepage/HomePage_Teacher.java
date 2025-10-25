package com.example.smartessay.TeacherHomepage;

import android.animation.ValueAnimator;
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

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Fragment showing the teacher's classrooms in a RecyclerView
public class HomePage_Teacher extends Fragment {

    private RecyclerView recyclerView; // RecyclerView to show classroom list
    private RoomAdapter roomAdapter;   // Adapter for RecyclerView
    private List<Room> roomList;       // Local list of rooms for adapter
    private DatabaseReference classroomsRef; // Firebase reference to "classrooms" node
    private ImageButton btnAddRoom;         // Button to add a new classroom
    private EditText editSearch;
// Search input field

    LinearLayout layoutRubrics;
    ImageView arrow;
    String teacherEmail; // Teacher's email passed from previous fragment/activity

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

        // Button to add a new room
        btnAddRoom = view.findViewById(R.id.btn_add_room);

        layoutRubrics = view.findViewById(R.id.layout_rubrics);



        /*ImageView arrow = view.findViewById(R.id.iv_swipe_arrow);
        //call animation method for swipeup
        animateSwipeArrow(arrow);*/



        // Search input
        editSearch = view.findViewById(R.id.edit_search_prompt);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                roomAdapter.filter(s.toString()); // Filter rooms by input text
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Get teacher email from parent fragment/activity
        Bundle args = getArguments();
        if (args != null) { // Check if arguments exist
            teacherEmail = args.getString("teacherEmail"); // store teacher email
            Log.d("HomePage_Teacher", "Email: " + teacherEmail);
        }

        // Click listener: open AddRoom_Teacher activity when button is clicked
        btnAddRoom.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddRoom_Teacher.class)));

        // Load classrooms from Firebase
        loadRoomsFromFirebase();

        // Click listener for each room item to open RoomDetails_Teacher activity //
        roomAdapter.setOnItemClickListener(room -> {
            Intent intent = new Intent(requireContext(), RoomDetails_Teacher.class);
            intent.putExtra("roomName", room.getRoomName());
            intent.putExtra("roomCode", room.getRoomCode());
            intent.putExtra("roomId", room.getRoomId());
            startActivity(intent);
        });

        // Enable swipe-to-delete feature
        swipeDelete();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Clear old data to avoid duplicates when fragment resumes
        roomList.clear();
        roomAdapter.notifyDataSetChanged();

        // Reload classrooms from Firebase
        loadRoomsFromFirebase();
    }

    // Custom Yes/No dialog
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

        // Cancel button clicked
        btnNo.setOnClickListener(v -> {
            if (onCancel != null) onCancel.run(); // run cancel action if exists
            dialog.dismiss();
        });

        // Yes button clicked
        btnYes.setOnClickListener(v -> {
            onYes.run(); // run yes action
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> {
            if (onCancel != null) onCancel.run(); // handle if dialog canceled
        });

        dialog.show();
    }

    // Swipe-to-delete method
    public void swipeDelete(){
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // Drag & drop not used
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= roomList.size()) {
                    // If position is invalid, reset swipe
                    roomAdapter.notifyDataSetChanged();
                    return;
                }

                Room roomToDelete = roomList.get(position);

                // Show confirmation dialog
                showYesNoDialog(
                        "Delete Room",
                        "Confirming will permanently delete the selected room. This action cannot be undone.",
                        () -> {
                            // Yes → remove room from Firebase
                            classroomsRef.child(roomToDelete.getRoomId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        if (position < roomList.size()) { // Check position is valid
                                            roomList.remove(position); // remove locally
                                            roomAdapter.notifyItemRemoved(position); // update UI
                                            loadRoomsFromFirebase(); // reload updated data
                                            Toast.makeText(requireContext(), "Room deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        roomAdapter.notifyItemChanged(position); // restore item visually
                                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                                    });
                        },
                        () -> roomAdapter.notifyItemChanged(position) // No → restore item visually
                );
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    // Draw red background and trash icon when swiping left
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

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
                return 0.7f; // 70% swipe required to trigger delete
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // Load classrooms from Firebase where classroom_owner equals teacherEmail
    private void loadRoomsFromFirebase() {
        classroomsRef.orderByChild("classroom_owner").equalTo(teacherEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomList.clear(); // Clear current list

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String roomId = ds.getKey(); // Firebase key
                            String roomName = ds.child("classroom_name").getValue(String.class); // get name
                            String roomCode = ds.child("room_code").getValue(String.class); // get code
                            String createdAt = ds.child("created_at").getValue(String.class); // get created date
                            String updatedAt = ds.child("updated_at").getValue(String.class); // get updated date

                            Map<String, String> rubrics = new LinkedHashMap<>();
                            if (ds.child("rubrics").exists()) {
                                Map<String, Object> rawRubrics = (Map<String, Object>) ds.child("rubrics").getValue();

                                // Add in the correct order manually
                                String[] keys = {"Topic", "Content and Ideas", "Organization and Structure",
                                        "Language Use and Style", "Subject Relevance", "Other Criteria", "Notes"};

                                for (String key : keys) {
                                    if (rawRubrics.containsKey(key)) {
                                        rubrics.put(key, String.valueOf(rawRubrics.get(key)));
                                    }
                                }
                            }

                            // Add room to local list
                            roomList.add(new Room(roomId, roomName, roomCode, createdAt, updatedAt, rubrics));
                        }

                        // Update fullRoomList in adapter for search functionality
                        roomAdapter.fullRoomList.clear();
                        roomAdapter.fullRoomList.addAll(roomList);

                        roomAdapter.notifyDataSetChanged(); // refresh UI
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load rooms", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Room model class
    public static class Room {
        private String roomId;
        private String roomName;
        private String roomCode;
        private String createdAt;
        private String updatedAt;
        private Map<String, String> rubrics;

        public Room() {} // Empty constructor required by Firebase

        public Room(String roomId, String roomName, String roomCode,
                    String createdAt, String updatedAt, Map<String, String> rubrics) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.roomCode = roomCode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.rubrics = rubrics;
        }

        public String getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public String getRoomCode() { return roomCode; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Map<String, String> getRubrics() { return rubrics; }
    }

    // RecyclerView Adapter
    public static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
        private final List<Room> fullRoomList; // Full copy for search
        private final List<Room> roomList;     // Displayed list
        private OnItemClickListener listener;

        public interface OnItemClickListener { void onItemClick(Room room); }

        public void setOnItemClickListener(OnItemClickListener listener) { this.listener = listener; }

        public RoomAdapter(List<Room> roomList) {
            this.roomList = roomList;
            this.fullRoomList = new ArrayList<>(roomList); // Copy for filtering
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
            holder.textCreatedAt.setText("Created: " + room.getCreatedAt());

            // Toggle rubrics visibility when button clicked
            holder.btnShowRubrics.setOnClickListener(v -> {
                if (holder.textRubrics.getVisibility() == View.GONE) { // If hidden
                    holder.textRubrics.setVisibility(View.VISIBLE);
                    holder.layoutRubrics.setVisibility(View.VISIBLE);
                    // show
                    holder.btnShowRubrics.setText("Hide Rubrics");
                } else { // If visible
                    holder.textRubrics.setVisibility(View.GONE);
                    holder.layoutRubrics.setVisibility(View.GONE);// hide
                    holder.btnShowRubrics.setText("Show Rubrics");
                }
            });

            if (room.getRubrics() != null) {
                StringBuilder summary = new StringBuilder();
                String[] order = {"Topic", "Content and Ideas", "Organization and Structure",
                        "Language Use and Style", "Subject Relevance", "Other Criteria", "Notes"};

                for (String key : order) {
                    if (room.getRubrics().containsKey(key)) {
                        summary.append(key).append(": ").append(room.getRubrics().get(key)).append("\n");
                    }
                }
                holder.textRubrics.setText(summary.toString().trim());
            } else {
                holder.textRubrics.setText("No Rubrics");
            }

            // Count classroom members using Firebase
            DatabaseReference membersRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(room.getRoomId())
                    .child("classroom_members");

            membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long memberCount = snapshot.getChildrenCount(); // Count children
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

        // Filter method for search
        public void filter(String query) {
            roomList.clear();
            if (query.isEmpty()) { //If search is empty
                roomList.addAll(fullRoomList); //show all rooms
            } else {
                query = query.toLowerCase();
                for (Room room : fullRoomList) {
                    // Check if roomName contains query (ignore case)
                    if (room.getRoomName() != null && room.getRoomName().toLowerCase().contains(query)) {
                        roomList.add(room); //add matching room
                    }
                }
            }
            notifyDataSetChanged(); // refresh UI
        }

        public static class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView textRoomName, textRoomCode, textCreatedAt, textUpdatedAt, textRubrics, textUploads;
            Button btnShowRubrics;
            LinearLayout layoutRubrics;

            public RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textRoomName = itemView.findViewById(R.id.text_room_name);
                textRoomCode = itemView.findViewById(R.id.text_room_code);
                textCreatedAt = itemView.findViewById(R.id.text_date_created);
                textUpdatedAt = itemView.findViewById(R.id.text_time_created);
                textRubrics = itemView.findViewById(R.id.text_rubrics);
                textUploads = itemView.findViewById(R.id.text_uploads);
                btnShowRubrics = itemView.findViewById(R.id.btn_show_rubrics);
                layoutRubrics = itemView.findViewById(R.id.layout_rubrics);
            }
        }
    }

}
