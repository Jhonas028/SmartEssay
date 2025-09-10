package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    private TextView tvConvertedText, tvEssayFeedback, tvScore, tvStatus;

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

        // Get data passed from previous activity
        String studentId = getIntent().getStringExtra("studentId");
        String roomId = getIntent().getStringExtra("roomId");

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
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
    }



}
