package com.example.smartessay.Student_Fragments;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EssayResult_Student extends AppCompatActivity {

    private TextView tvEssayTitle, tvEssayText, tvScore, tvFeedback;
    private String essayId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essay_result_student);

        // bind views
        tvEssayTitle = findViewById(R.id.tvEssayTitle);
        tvEssayText = findViewById(R.id.tvEssayText);
        tvScore = findViewById(R.id.tvScore);
        tvFeedback = findViewById(R.id.tvFeedback);

        // get essayId from intent
        essayId = getIntent().getStringExtra("essayId");
        if (essayId == null) {
            Toast.makeText(this, "No essay ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEssayResult(essayId);
    }

    private void loadEssayResult(String essayId) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        db.child("essay").child(essayId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String essayText = snapshot.child("converted_text").getValue(String.class);
                            Long score = snapshot.child("score").getValue(Long.class);
                            String feedback = snapshot.child("essay_feedback").getValue(String.class);
                            String classroomName = snapshot.child("classroom_name").getValue(String.class);

                            tvEssayTitle.setText("Essay Result - " + (classroomName != null ? classroomName : ""));
                            tvEssayText.setText(essayText != null ? essayText : "No essay text");
                            tvScore.setText("Score: " + (score != null ? score : 0));
                            tvFeedback.setText("Feedback: " + (feedback != null ? feedback : "No feedback"));
                        } else {
                            Toast.makeText(EssayResult_Student.this, "Essay not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(EssayResult_Student.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
