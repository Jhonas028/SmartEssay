package com.example.smartessay.TeacherHomepage;

import static java.security.AccessController.getContext;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EssayDetails_Teacher extends AppCompatActivity {

    private TextView tvConvertedText, tvEssayFeedback, tvStatus,fullname;
    private EditText tvScore;

    Button btnSave;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_essay_details);

        // Initialize TextViews
        tvConvertedText = findViewById(R.id.tvConvertedText);
        tvEssayFeedback = findViewById(R.id.tvEssayFeedback);
        tvScore = findViewById(R.id.tvScore);
        tvStatus = findViewById(R.id.tvStatus);
        fullname = findViewById(R.id.fullname);

        btnSave = findViewById(R.id.btnSave);

        // Get data passed from previous activity
        String studentId = getIntent().getStringExtra("studentId");
        String roomId = getIntent().getStringExtra("roomId");

        dbRef = FirebaseDatabase.getInstance()
                .getReference("essay");

        // ✅ Query essays by studentId + roomId
        dbRef.orderByChild("student_id").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot essaySnap : snapshot.getChildren()) {
                                String essayRoomId = essaySnap.child("classroom_id").getValue(String.class);

                                // ✅ Only show if it matches the selected classroom
                                if (roomId.equals(essayRoomId)) {
                                    String convertedText = essaySnap.child("converted_text").getValue(String.class);
                                    String essayFeedback = essaySnap.child("essay_feedback").getValue(String.class);
                                    String score = String.valueOf(essaySnap.child("score").getValue());
                                    String status = essaySnap.child("status").getValue(String.class);
                                    String fullName = essaySnap.child("fullname").getValue(String.class);


                                    Log.d("EssayDetails", "ConvertedText=" + convertedText);
                                    Log.d("EssayDetails", "EssayFeedback=" + essayFeedback);
                                    Log.d("EssayDetails", "Score=" + score);
                                    Log.d("EssayDetails", "Status=" + status);
                                    Log.d("EssayDetails", "Student ID=" + studentId);
                                    Log.d("EssayDetails", "RoomID=" + roomId);
                                    Log.d("EssayDetails","fullnamee" + fullName);

                                    tvConvertedText.setText(convertedText != null ? convertedText : "No text");
                                    tvEssayFeedback.setText(essayFeedback != null ? essayFeedback : "No feedback");
                                    fullname.setText(fullName != null ? fullName : "No name exist");
                                    tvScore.setText(score != null ? score: "No score");
                                    tvStatus.setText(status != null ? status : "No status");

                                    // ✅ Change color based on status
                                    if ("pending".equalsIgnoreCase(status)) {
                                        tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                                        tvStatus.setText("PENDING");// 🔴 Red for pending
                                    } else if ("posted".equalsIgnoreCase(status)) {
                                        tvStatus.setTextColor(Color.parseColor("#00C853"));
                                        tvStatus.setText("GRADED");// 🟢 Green for posted
                                    } else {
                                        tvStatus.setTextColor(Color.parseColor("#000000")); // ⚫ Default black
                                    }

                                    break; // stop after finding the essay for this room
                                }
                            }
                        } else {
                            Log.e("EssayDetails", "No essays found for studentId: " + studentId);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("EssayDetails", "Database error: " + error.getMessage());
                    }
                });

        saveChanges(studentId,roomId);
    }

    // Inside EssayResult_Student.java
    private void saveChanges(String studentId, String roomId) {
        btnSave.setOnClickListener(v -> {
            String newScore = tvScore.getText().toString().replace("Score: ", "").trim();

            if (!newScore.isEmpty()) {
                try {
                    long scoreValue = Long.parseLong(newScore); // convert to number

                    if (scoreValue < 0 || scoreValue > 100) {
                        Toast.makeText(getApplicationContext(), "Score must be between 0 and 100", Toast.LENGTH_SHORT).show();
                        return; // stop saving
                    }

                    // Call new method to update Firebase
                    updateEssayScore(studentId, roomId, scoreValue);

                    postIndividualScore(studentId, roomId, scoreValue);

                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "Invalid score format", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ✅ New method for updating the individual essay score in Firebase
    private void updateEssayScore(String studentId, String roomId, long scoreValue) {
        dbRef.orderByChild("student_id").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String essayRoomId = essaySnap.child("classroom_id").getValue(String.class);
                            if (roomId.equals(essayRoomId)) {
                                essaySnap.getRef().child("score").setValue(scoreValue); // store as Long
                                Toast.makeText(getApplicationContext(), "Score successfully saved and posted", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    private void postIndividualScore(String studentId, String roomId, long scoreValue) {
        // Update the essay
        dbRef.orderByChild("student_id").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String essayRoomId = essaySnap.child("classroom_id").getValue(String.class);
                            if (roomId.equals(essayRoomId)) {
                                // Update score and essay status
                                essaySnap.getRef().child("score").setValue(scoreValue);
                                essaySnap.getRef().child("status").setValue("posted");

                                // Also update the classroom_members status
                                DatabaseReference memberRef = FirebaseDatabase.getInstance()
                                        .getReference("classrooms")
                                        .child(roomId)
                                        .child("classroom_members")
                                        .child(studentId)
                                        .child("status");
                                memberRef.setValue("posted");

                                // Update UI
                                tvStatus.setText("GRADED");
                                tvStatus.setTextColor(Color.parseColor("#00C853"));

                                Toast.makeText(EssayDetails_Teacher.this,
                                        "Essay score has been saved and posted", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }





}
