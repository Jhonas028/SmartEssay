package com.example.smartessay.TeacherHomepage;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class RoomDetails_Teacher extends AppCompatActivity {

    TextView tvRoomName, tvRoomCode;
    RecyclerView rvStudents;
    StudentAdapter adapter;
    List<EssayTeacher> essayList = new ArrayList<>();
    List<EssayTeacher> filteredList = new ArrayList<>();

    String classroomId;
    Button btn_post_scoree;
    EditText editSearchStudent;
    ImageButton back_image_btn;

    private ActivityResultLauncher<Intent> essayLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_details_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        rvStudents = findViewById(R.id.recycler_view_rooms);
        btn_post_scoree = findViewById(R.id.btn_post_scoree);
        editSearchStudent = findViewById(R.id.edit_search_prompt);
        back_image_btn = findViewById(R.id.back_image_btn);

        //get room name and code from previous activity
        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");
        classroomId = getIntent().getStringExtra("roomId");

        //set room name and code to textviews
        tvRoomName.setText(roomName);
        tvRoomCode.setText("Room Code: " + roomCode);

        // 🔹 Insert here: Initialize ActivityResultLauncher for essay updates
        essayLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("scoreUpdated", false)) {
                            loadEssaysFromFirebase(); // 🔹 Refresh list when returning from EssayDetails_Teacher
                        }
                    }
                }
        );


        // 🔽 RecyclerView
        rvStudents.setLayoutManager(new LinearLayoutManager(this)); // Set layout manager
        adapter = new StudentAdapter(filteredList, classroomId); // Set adapter
        rvStudents.setAdapter(adapter); // Set adapter to RecyclerView



        back_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the current activity and go back to the previous one
                finish();
            }
        });

        btn_post_scoree.setOnClickListener(v -> { // Post scores to Firebase
            showYesNoDialog(
                    "Confirm Post",
                    "Are you sure you want to post the scores of students?",
                    this::postScoresToFirebase // Lambda expression, call postScoresToFirebase()
            );
        });
        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) { // Left swipe
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EssayTeacher essay = filteredList.get(position);

                showYesNoDialog( // Show dialog, confirm delete
                        "Delete Student",
                        "Confirming will permanently delete the essay. This action cannot be undone.",
                        () -> deleteStudentEssay(essay, position) // Lambda expression, call deleteStudentEssay()
                );
                adapter.notifyItemChanged(position); // Update adapter, notify of change
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, //show ui for , swipe, delete, icon etc...
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(
                            (float) itemView.getRight() + dX,
                            (float) itemView.getTop(),
                            (float) itemView.getRight(),
                            (float) itemView.getBottom(),
                            paint
                    );

                    Drawable icon = ContextCompat.getDrawable(RoomDetails_Teacher.this, R.drawable.ic_delete);
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
        });
        itemTouchHelper.attachToRecyclerView(rvStudents);

        // 🔍 Search function
        editSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString()); // Call filterList()
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        loadEssaysFromFirebase();
    }

    protected void onResume() {
        super.onResume();
        loadEssaysFromFirebase(); // ensures latest data when returning to this activity
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(essayList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (EssayTeacher essay : essayList) {
                String fullname = essay.getFullname().toLowerCase().replaceAll("[,]", " "); // replace comma with space
                String studentId = essay.getStudentId().toLowerCase();

                if (fullname.contains(lowerQuery) || studentId.contains(lowerQuery)) {
                    filteredList.add(essay);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }


    private void deleteStudentEssay(EssayTeacher essay, int position) {
        if (essay.getEssayId() != null) {
            DatabaseReference essayRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(classroomId)
                    .child("essay")
                    .child(essay.getStudentId())
                    .child(essay.getEssayId());

            essayRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    essayList.remove(essay);
                    filterList(editSearchStudent.getText().toString());
                    Toast.makeText(RoomDetails_Teacher.this, "Essay deleted", Toast.LENGTH_SHORT).show();
                } else {
                    filterList(editSearchStudent.getText().toString());
                    Toast.makeText(RoomDetails_Teacher.this, "Failed to delete essay", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            DatabaseReference memberRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(classroomId)
                    .child("classroom_members")
                    .child(essay.getStudentId());

            memberRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    essayList.remove(essay);
                    filterList(editSearchStudent.getText().toString());
                    Toast.makeText(RoomDetails_Teacher.this, "Student removed", Toast.LENGTH_SHORT).show();
                } else {
                    filterList(editSearchStudent.getText().toString());
                    Toast.makeText(RoomDetails_Teacher.this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadEssaysFromFirebase() {
        DatabaseReference membersRef = FirebaseDatabase.getInstance()
                .getReference("classrooms")
                .child(classroomId)
                .child("classroom_members");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                essayList.clear(); // ✅ clear list first

                if (!snapshot.exists()) {
                    filteredList.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                List<EssayTeacher> tempList = new ArrayList<>();
                int totalStudents = (int) snapshot.getChildrenCount();
                final int[] loadedCount = {0};

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String studentId = studentSnap.getKey();
                    String fullname = studentSnap.child("fullname").getValue(String.class);
                    String stats = studentSnap.child("status").getValue(String.class);

                    DatabaseReference essayRef = FirebaseDatabase.getInstance()
                            .getReference("classrooms")
                            .child(classroomId)
                            .child("essay")
                            .child(studentId);

                    essayRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot essaySnap) {
                            if (essaySnap.exists()) {
                                for (DataSnapshot essayEntry : essaySnap.getChildren()) {
                                    EssayTeacher essay = essayEntry.getValue(EssayTeacher.class);
                                    if (essay != null) {
                                        essay.setEssayId(essayEntry.getKey());
                                        essay.setStudentId(studentId);
                                        essay.setFullname(fullname);
                                        essay.setStatus(stats);
                                        tempList.add(essay);
                                    }
                                }
                            } else {
                                EssayTeacher noEssay = new EssayTeacher();
                                noEssay.setStudentId(studentId);
                                noEssay.setFullname(fullname);
                                noEssay.setConvertedText("No submission yet");
                                noEssay.setScore(0);
                                noEssay.setCreatedAt(System.currentTimeMillis());
                                noEssay.setStatus(stats);
                                tempList.add(noEssay);
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == totalStudents) {
                                // ✅ Only update list and filter once when all students are loaded
                                essayList.clear();
                                essayList.addAll(tempList);
                                filterList(editSearchStudent.getText().toString());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    // 🔽 Adapter Class
    public static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

        private List<EssayTeacher> essays; // List of essays to display in RecyclerView
        private String classroomId;     // Classroom ID to pass along when opening essay details

        public StudentAdapter(List<EssayTeacher> essays, String classroomId) {
            this.essays = essays;
            this.classroomId = classroomId;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())      // Inflate layout for each item in the RecyclerView
                    .inflate(R.layout.teacher_essay_item_layout, parent, false);
            return new StudentViewHolder(view);   // Return a new ViewHolder instance with this vi
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            // Get the essay data for the current position
            EssayTeacher essay = essays.get(position);
            // Set student number and full name in the TextViews
            holder.tvStudentName.setText("Student no.: " + essay.getStudentId());
            holder.tvFullname.setText(essay.getFullname());
            // Convert timestamp to Date object
            Date date = new Date(essay.getCreatedAt());
            // Format date and time for display
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            // Set formatted date and time in the respective TextViews
            holder.tvDateCreated.setText("Date Submitted: " + dateFormat.format(date));
            holder.tvTimeCreated.setText("Time Submitted: " + timeFormat.format(date));
            // Display the status of the essay
            holder.text_status.setText(essay.getStatus());

            // 🎨 Change color depending on status
            if (essay.getStatus() != null) {
                switch (essay.getStatus().toLowerCase()) {
                    case "pending":
                        holder.text_status.setTextColor(Color.RED);
                        break;
                    case "posted":
                        holder.text_status.setTextColor(Color.parseColor("#00C853"));
                        holder.text_status.setText("GRADED");
                        break;
                    default:
                        holder.text_status.setTextColor(Color.BLACK);
                        break;
                }
            } else {
                holder.text_status.setTextColor(Color.BLACK); // If status is null, set text color to black
            }
            // Set click listener for the entire item
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EssayDetails_Teacher.class);
                intent.putExtra("roomId", classroomId); // Pass classroom ID
                intent.putExtra("studentId", essay.getStudentId()); // Pass student ID
                intent.putExtra("essayId", essay.getEssayId()); // Pass essay ID
                // 🔹 Launch activity for result so parent can refresh when coming back
                if (v.getContext() instanceof RoomDetails_Teacher) {
                    ((RoomDetails_Teacher) v.getContext()).essayLauncher.launch(intent);
                }// Start the activity
            });

        }

        @Override
        public int getItemCount() {
            // Return the total number of essays in the list
            return essays.size();
        }

        // ViewHolder class to hold references to item views
        static class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvDateCreated, tvTimeCreated, text_status, tvFullname;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                // Bind TextViews from layout
                tvStudentName = itemView.findViewById(R.id.text_studnum);
                tvDateCreated = itemView.findViewById(R.id.text_date_created);
                tvTimeCreated = itemView.findViewById(R.id.text_time_created);
                text_status = itemView.findViewById(R.id.text_status);
                tvFullname = itemView.findViewById(R.id.txt_name_teachere);

            }
        }
    }

    private void postScoresToFirebase() {
        // Get the classroom ID passed from the previous activity via Intent
        String classroomId2 = getIntent().getStringExtra("roomId");
        // Get a reference to the "essay" node in Firebase Realtime Database
        DatabaseReference essayRef = FirebaseDatabase.getInstance().getReference("essay");
        // Create a query to find all essays where "classroom_id" equals the current classroomId2
        Query query = essayRef.orderByChild("classroom_id").equalTo(classroomId2);

        // Add a listener to the query to react whenever data changes
        //addListenerForSingleValueEvent, this is the original, the data will change once at a time
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Loop through each essay that matches the query
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Update the "status" field of each essay to "posted"
                    snapshot.getRef().child("status").setValue("posted");
                }
                // Now update all classroom members for this classroom
                DatabaseReference membersRef = FirebaseDatabase.getInstance()
                        .getReference("classrooms")// Navigate to "classrooms" node
                        .child(classroomId2) // Specific classroom
                        .child("classroom_members");// All members of this classroom

                // Use addListenerForSingleValueEvent here because we just need to update once
                membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Loop through each member in the classroom
                        for (DataSnapshot memberSnap : snapshot.getChildren()) {
                            String studentId = memberSnap.getKey();// Get student ID
                            if (studentId != null) {
                                // Set the status of each student to "posted"
                                membersRef.child(studentId).child("status").setValue("posted");
                            }
                        }
                        // Show a confirmation message
                        Toast.makeText(getApplicationContext(), "All essays and classroom members updated to posted!", Toast.LENGTH_SHORT).show();
                        loadEssaysFromFirebase();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void showYesNoDialog(String title, String message, Runnable onYes) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_yes_no, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            onYes.run();
            dialog.dismiss();
        });

        dialog.show();
    }
}
