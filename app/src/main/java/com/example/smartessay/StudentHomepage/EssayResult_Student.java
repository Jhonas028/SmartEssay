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

        // Bind views to XML elements
        tvEssayTitle = findViewById(R.id.tvEssayTitle);
        tvEssayText = findViewById(R.id.tvEssayText);
        tvScore = findViewById(R.id.tvScore);
        tvFeedback = findViewById(R.id.tvFeedback);

        // Get essayId from the Intent (this is passed from the previous activity)
        essayId = getIntent().getStringExtra("essayId");

        // If essayId is missing, show error and close this page
        if (essayId == null) {
            Toast.makeText(this, "No essay ID provided", Toast.LENGTH_SHORT).show();
            finish(); // close activity
            return;
        }

        // If essayId is available, load the essay result from Firebase
        loadEssayResult(essayId);
    }

    private void loadEssayResult(String essayId) {
        // Get a reference to the Firebase Realtime Database
        // Here, we connect to the "root" of the database
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        // Navigate to the path: "essay" -> essayId
        // Example: essay/12345
        db.child("essay").child(essayId)
                // This reads the data only once (not continuously)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // This runs when Firebase returns data for essayId

                        // If the essayId exists in Firebase
                        if (snapshot.exists()) {
                            // Get each field inside that essay
                            String essayText = snapshot.child("converted_text").getValue(String.class);
                            Long score = snapshot.child("score").getValue(Long.class);
                            String feedback = snapshot.child("essay_feedback").getValue(String.class);
                            String classroomName = snapshot.child("classroom_name").getValue(String.class);

                            // Update the UI with the data from Firebase
                            // (Use default text if the value is missing)
                            tvEssayTitle.setText("Essay Result - " + (classroomName != null ? classroomName : ""));
                            tvEssayText.setText(essayText != null ? essayText : "No essay text");
                            tvScore.setText("Score: " + (score != null ? score : 0));
                            tvFeedback.setText("Feedback: " + (feedback != null ? feedback : "No feedback"));
                        }
                        // If the essayId does NOT exist in Firebase
                        else {
                            Toast.makeText(EssayResult_Student.this, "Essay not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // This runs if something goes wrong (e.g. no permission, no internet)
                        Toast.makeText(EssayResult_Student.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
