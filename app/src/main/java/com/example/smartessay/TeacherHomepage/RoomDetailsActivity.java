package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RoomDetailsActivity extends AppCompatActivity {

    TextView tvRoomName, tvRoomCode;
    RecyclerView rvStudents;
    StudentAdapter adapter;
    List<EssayTeacher> essayList = new ArrayList<>();

    String classroomId; // Passed from previous activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        rvStudents = findViewById(R.id.recycler_view_rooms);

        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");
        classroomId = getIntent().getStringExtra("roomId"); // 🔑 from intent

        tvRoomName.setText("Room Name: " + roomName);
        tvRoomCode.setText("Room Code: " + roomCode);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(essayList);
        rvStudents.setAdapter(adapter);

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
                    Log.d("FirebaseDebug", "Found studentId: " + studentId);

                    DatabaseReference essayRef = FirebaseDatabase.getInstance()
                            .getReference("classrooms")
                            .child(classroomId)
                            .child("essay")
                            .child(studentId);

                    // Check if this student has essays
                    essayRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot essaySnap) {
                            if (essaySnap.exists()) {
                                // A student may have multiple essays, loop them
                                for (DataSnapshot essayEntry : essaySnap.getChildren()) {
                                    EssayTeacher essay = essayEntry.getValue(EssayTeacher.class);
                                    if (essay != null) {
                                        essayList.add(essay);
                                        Log.d("FirebaseDebug", "Essay found for " + studentId + ": " + essay.getConvertedText());
                                    }
                                }
                            } else {
                                // Add a placeholder if no essay yet
                                EssayTeacher noEssay = new EssayTeacher();
                                noEssay.setStudentId(studentId);
                                noEssay.setConvertedText("No submission yet");
                                noEssay.setScore(0);
                                noEssay.setCreatedAt(System.currentTimeMillis()); // or joined_at
                                essayList.add(noEssay);

                                Log.d("FirebaseDebug", "No essay for studentId: " + studentId);
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

        public StudentAdapter(List<EssayTeacher> essays) {
            this.essays = essays;
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

            holder.tvStudentName.setText("Student ID: " + essay.getStudentId());

            Date date = new Date(essay.getCreatedAt());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            holder.tvDateCreated.setText("Submitted: " + dateFormat.format(date));
            holder.tvTimeCreated.setText("Time: " + timeFormat.format(date));
            holder.tvScore.setText("Score: " + essay.getScore());
        }


        @Override
        public int getItemCount() {
            return essays.size();
        }

        static class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvDateCreated, tvTimeCreated, tvScore;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.text_student_name);
                tvDateCreated = itemView.findViewById(R.id.text_date_created);
                tvTimeCreated = itemView.findViewById(R.id.text_time_created);
                tvScore = itemView.findViewById(R.id.text_available_students);
            }
        }
    }
}
