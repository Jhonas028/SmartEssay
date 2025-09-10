package com.example.smartessay.TeacherHomepage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;
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

    String classroomId; // Passed from previous activity

    Button btn_post_scoree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_details_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        rvStudents = findViewById(R.id.recycler_view_rooms);

        btn_post_scoree = findViewById(R.id.btn_post_scoree);

        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");
        classroomId = getIntent().getStringExtra("roomId"); // 🔑 from intent

        tvRoomName.setText(roomName);
        tvRoomCode.setText("Room Code: " + roomCode);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(essayList, classroomId);
        rvStudents.setAdapter(adapter);

        btn_post_scoree.setOnClickListener(v -> {
            showYesNoDialog(
                    "Confirm Post",
                    "Are you sure you want to post the scores of students?",
                    this::postScoresToFirebase
            );
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // We don’t support drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EssayTeacher essay = essayList.get(position);

                // Show confirm dialog before deleting
                new AlertDialog.Builder(RoomDetails_Teacher.this)
                        .setTitle("Delete Student")
                        .setMessage("Are you sure you want to remove this student and their essay?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            deleteStudentEssay(essay, position);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Restore the item if cancel
                            adapter.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> {
                            // Handles outside touch or back button
                            adapter.notifyItemChanged(position); // restore item
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;

                    // 🔴 Red background
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    c.drawRect(
                            (float) itemView.getRight() + dX, // left bound
                            (float) itemView.getTop(),        // top
                            (float) itemView.getRight(),      // right bound
                            (float) itemView.getBottom(),     // bottom
                            paint
                    );

                    // 🗑 Delete icon
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

                // let RecyclerView draw the foreground (the actual card moving)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

// 🔗 Attach to RecyclerView
        itemTouchHelper.attachToRecyclerView(rvStudents);



        loadEssaysFromFirebase();
    }

    private void deleteStudentEssay(EssayTeacher essay, int position) {
        if (essay.getEssayId() != null) {
            // Delete essay from Firebase
            DatabaseReference essayRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(classroomId)
                    .child("essay")
                    .child(essay.getStudentId())
                    .child(essay.getEssayId());

            essayRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    essayList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(RoomDetails_Teacher.this, "Essay deleted", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position); // restore if error
                    Toast.makeText(RoomDetails_Teacher.this, "Failed to delete essay", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // If essayId is null → remove student only
            DatabaseReference memberRef = FirebaseDatabase.getInstance()
                    .getReference("classrooms")
                    .child(classroomId)
                    .child("classroom_members")
                    .child(essay.getStudentId());

            memberRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    essayList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(RoomDetails_Teacher.this, "Student removed", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position);
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
                essayList.clear();

                if (!snapshot.exists()) {
                    Log.d("FirebaseDebug", "No classroom_members found for room " + classroomId);
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String studentId = studentSnap.getKey();
                    String fullname = studentSnap.child("fullname").getValue(String.class);
                    String stats = studentSnap.child("status").getValue(String.class);
                    Log.i("statusDebug", "This is a status: " + stats);

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
                                    String essayId = essayEntry.getKey();
                                    if (essay != null) {
                                        essay.setEssayId(essayId);
                                        // ✅ attach essayId
                                        essay.setStudentId(studentId);
                                        essay.setFullname(fullname);
                                        essay.setStatus(stats);
                                        Log.d("FirebaseDebug2", "Loaded EssayId: " + essayId + " for Student: " + studentId);
                                        essayList.add(essay);
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
                                // ⚠️ no essayId here, so it will be skipped in postScores
                                essayList.add(noEssay);

                                Log.d("FirebaseDebug", "No essay found for student: " + studentId);
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("FirebaseDebug", "Error: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Error: " + error.getMessage());
            }
        });
    }

    // 🔽 Adapter Class
    public static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

        private List<EssayTeacher> essays;
        private String classroomId;

        public StudentAdapter(List<EssayTeacher> essays, String classroomId) {
            this.essays = essays;
            this.classroomId = classroomId;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.teacher_essay_item_layout, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            EssayTeacher essay = essays.get(position);

            holder.tvStudentName.setText("Student no.: " + essay.getStudentId());

            Date date = new Date(essay.getCreatedAt());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            holder.tvDateCreated.setText("Submitted: " + dateFormat.format(date));
            holder.tvTimeCreated.setText("Time: " + timeFormat.format(date));
            holder.tvFullname.setText(essay.getFullname());

            holder.text_status.setText("Status: " + essay.getStatus());


            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EssayDetails_Teacher.class);
                intent.putExtra("roomId", classroomId);
                intent.putExtra("studentId", essay.getStudentId());
                intent.putExtra("essayId", essay.getEssayId()); // ✅ send essayId
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return essays.size();
        }

        static class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvDateCreated, tvTimeCreated, text_status, tvFullname;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.text_studnum);
                tvDateCreated = itemView.findViewById(R.id.text_date_created);
                tvTimeCreated = itemView.findViewById(R.id.text_time_created);
                text_status = itemView.findViewById(R.id.text_status);
                tvFullname = itemView.findViewById(R.id.txt_name_teachere);
            }
        }
    }

    private void postScoresToFirebase() {

        String classroomId2 = getIntent().getStringExtra("roomId");

        // 1️⃣ Update essay statuses
        DatabaseReference essayRef = FirebaseDatabase.getInstance().getReference("essay");
        Query query = essayRef.orderByChild("classroom_id").equalTo(classroomId2);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().child("status").setValue("posted");
                }

                // 2️⃣ Update classroom_members statuses
                DatabaseReference membersRef = FirebaseDatabase.getInstance()
                        .getReference("classrooms")
                        .child(classroomId2)
                        .child("classroom_members");

                membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot memberSnap : snapshot.getChildren()) {
                            String studentId = memberSnap.getKey();
                            if (studentId != null) {
                                membersRef.child(studentId).child("status").setValue("posted");
                            }
                        }

                        Toast.makeText(getApplicationContext(), "All essays and classroom members updated to posted!", Toast.LENGTH_SHORT).show();
                        loadEssaysFromFirebase(); // refresh UI
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Error updating members: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error updating essays: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showYesNoDialog(String title, String message, Runnable onYes) {
        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_yes_no, null); // make sure this XML exists

        // Find views
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        // Set title and message
        tvTitle.setText(title);
        tvMessage.setText(message);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Button actions
        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            onYes.run();  // execute the action
            dialog.dismiss();
        });

        dialog.show();
    }





}
