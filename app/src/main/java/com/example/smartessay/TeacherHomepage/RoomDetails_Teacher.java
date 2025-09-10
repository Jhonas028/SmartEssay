package com.example.smartessay.TeacherHomepage;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
    TextInputEditText editSearchStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_details_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        rvStudents = findViewById(R.id.recycler_view_rooms);
        btn_post_scoree = findViewById(R.id.btn_post_scoree);
        editSearchStudent = findViewById(R.id.edit_search_student);

        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");
        classroomId = getIntent().getStringExtra("roomId");

        tvRoomName.setText(roomName);
        tvRoomCode.setText("Room Code: " + roomCode);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(filteredList, classroomId);
        rvStudents.setAdapter(adapter);

        btn_post_scoree.setOnClickListener(v -> {
            showYesNoDialog(
                    "Confirm Post",
                    "Are you sure you want to post the scores of students?",
                    this::postScoresToFirebase
            );
        });

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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

                showYesNoDialog(
                        "Delete Student",
                        "Are you sure you want to remove this student and their essay?",
                        () -> deleteStudentEssay(essay, position)
                );

                adapter.notifyItemChanged(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
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
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        loadEssaysFromFirebase();
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
                essayList.clear();

                if (!snapshot.exists()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

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
                                    String essayId = essayEntry.getKey();
                                    if (essay != null) {
                                        essay.setEssayId(essayId);
                                        essay.setStudentId(studentId);
                                        essay.setFullname(fullname);
                                        essay.setStatus(stats);
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
                                essayList.add(noEssay);
                            }
                            filterList(editSearchStudent.getText().toString());
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
            holder.tvFullname.setText(essay.getFullname());

            Date date = new Date(essay.getCreatedAt());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            holder.tvDateCreated.setText("Submitted: " + dateFormat.format(date));
            holder.tvTimeCreated.setText("Time: " + timeFormat.format(date));
            holder.text_status.setText("Status: " + essay.getStatus());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EssayDetails_Teacher.class);
                intent.putExtra("roomId", classroomId);
                intent.putExtra("studentId", essay.getStudentId());
                intent.putExtra("essayId", essay.getEssayId());
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

        DatabaseReference essayRef = FirebaseDatabase.getInstance().getReference("essay");
        Query query = essayRef.orderByChild("classroom_id").equalTo(classroomId2);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().child("status").setValue("posted");
                }

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
