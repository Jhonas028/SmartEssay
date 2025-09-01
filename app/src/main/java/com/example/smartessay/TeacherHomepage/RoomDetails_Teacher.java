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
            // Create the AlertDialog
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Post")
                    .setMessage("Are you sure you want to post the scores of students?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // User confirmed, post scores
                        postScoresToFirebase();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // User canceled, dismiss dialog
                        dialog.dismiss();
                    })
                    .show();
        });

        loadEssaysFromFirebase();
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





}
