package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class AddRoomActivity extends AppCompatActivity {

    EditText etRoomName, etRubricContent, etRubricOrganization, etRubricDevelopment,
            etRubricGrammar, etRubricCritical, etRubricOther;
    Button btnCreate, btnCancel;

    DatabaseReference classroomsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_room);

        // Firebase reference
        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");

        // Views
        etRoomName = findViewById(R.id.etRoomName);
        etRubricContent = findViewById(R.id.etRubricContent);
        etRubricOrganization = findViewById(R.id.etRubricOrganization);
        etRubricDevelopment = findViewById(R.id.etRubricDevelopment);
        etRubricGrammar = findViewById(R.id.etRubricGrammar);
        etRubricCritical = findViewById(R.id.etRubricCritical);
        etRubricOther = findViewById(R.id.etRubricOther);

        btnCreate = findViewById(R.id.btnCreate);
        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> {
            String roomName = etRoomName.getText().toString().trim();
            if (roomName.isEmpty()) {
                Toast.makeText(this, "Enter Room Name", Toast.LENGTH_SHORT).show();
                return;
            }

            String teacherEmail = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getEmail() : "unknown";

            // Date and time
            String dateTime = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                    .format(new Date());

            generateUniqueRoomCode(roomCode -> {
                String roomId = classroomsRef.push().getKey();

                // Rubrics map (Firebase-safe keys)
                Map<String, Object> rubrics = new HashMap<>();
                rubrics.put("content_ideas", etRubricContent.getText().toString().trim());
                rubrics.put("organization_structure", etRubricOrganization.getText().toString().trim());
                rubrics.put("development_support", etRubricDevelopment.getText().toString().trim());
                rubrics.put("grammar_formatting", etRubricGrammar.getText().toString().trim());
                rubrics.put("critical_thinking", etRubricCritical.getText().toString().trim());
                rubrics.put("other", etRubricOther.getText().toString().trim());

                // Classroom map
                Map<String, Object> classroomMap = new HashMap<>();
                classroomMap.put("classroom_name", roomName);
                classroomMap.put("classroom_owner", teacherEmail);
                classroomMap.put("status", "active");
                classroomMap.put("created_at", dateTime);
                classroomMap.put("updated_at", dateTime);
                classroomMap.put("room_code", roomCode);
                classroomMap.put("rubrics", rubrics);

                // Save to Firebase
                classroomsRef.child(roomId).setValue(classroomMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Room Created Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        });
    }

    // Generate unique room code
    private void generateUniqueRoomCode(OnCodeGeneratedListener listener) {
        String code = generateRoomCode();

        classroomsRef.orderByChild("room_code").equalTo(code)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            generateUniqueRoomCode(listener); // try again
                        } else {
                            listener.onCodeGenerated(code);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddRoomActivity.this, "Error checking code", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    interface OnCodeGeneratedListener {
        void onCodeGenerated(String code);
    }
}
