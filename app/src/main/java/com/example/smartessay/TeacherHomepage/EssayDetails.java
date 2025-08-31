package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EssayDetails extends AppCompatActivity {

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

        // Example student id or essay key
        String studentId = getIntent().getStringExtra("studentId");
        String roomId = getIntent().getStringExtra("roomId");

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("essay")
                .child(studentId)
                .child(roomId);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String convertedText = snapshot.child("converted_text").getValue(String.class);
                    String essayFeedback = snapshot.child("essay_feedback").getValue(String.class);
                    String score = String.valueOf(snapshot.child("score").getValue());
                    String status = snapshot.child("status").getValue(String.class);

                    Log.d("EssayDetails", "ConvertedText=" + convertedText);
                    Log.d("EssayDetails", "EssayFeedback=" + essayFeedback);
                    Log.d("EssayDetails", "Score=" + score);
                    Log.d("EssayDetails", "Status=" + status);

                    tvConvertedText.setText(convertedText != null ? convertedText : "No text");
                    tvEssayFeedback.setText(essayFeedback != null ? essayFeedback : "No feedback");
                    tvScore.setText(score != null ? "Score: " + score : "No score");
                    tvStatus.setText(status != null ? "Status: " + status : "No status");
                } else {
                    Log.e("EssayDetails", "No data found for studentId: " + studentId + " roomId: " + roomId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("EssayDetails", "Database error: " + error.getMessage());
            }
        });

    }
}
