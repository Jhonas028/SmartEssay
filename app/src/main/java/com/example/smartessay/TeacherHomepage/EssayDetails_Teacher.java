package com.example.smartessay.TeacherHomepage;

import static java.security.AccessController.getContext;

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

    private TextView tvConvertedText, tvEssayFeedback, tvStatus;
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

                                    Log.d("EssayDetails", "ConvertedText=" + convertedText);
                                    Log.d("EssayDetails", "EssayFeedback=" + essayFeedback);
                                    Log.d("EssayDetails", "Score=" + score);
                                    Log.d("EssayDetails", "Status=" + status);
                                    Log.d("EssayDetails", "Student ID=" + studentId);
                                    Log.d("EssayDetails", "RoomID=" + roomId);

                                    tvConvertedText.setText(convertedText != null ? convertedText : "No text");
                                    tvEssayFeedback.setText(essayFeedback != null ? essayFeedback : "No feedback");
                                    tvScore.setText(score != null ? "Score: " + score : "No score");
                                    tvStatus.setText(status != null ? "Status: " + status : "No status");

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
                    dbRef.orderByChild("student_id").equalTo(studentId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    for (DataSnapshot essaySnap : snapshot.getChildren()) {
                                        String essayRoomId = essaySnap.child("classroom_id").getValue(String.class);
                                        if (roomId.equals(essayRoomId)) {
                                            essaySnap.getRef().child("score").setValue(scoreValue); // store as Long
                                            break;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) { }
                            });
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "Invalid score format", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }




}
