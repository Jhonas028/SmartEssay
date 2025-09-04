package com.example.smartessay.TeacherHomepage;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
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

public class AddRoom_Teacher extends AppCompatActivity {

    EditText etRoomName, etRubricContent, etRubricOrganization, etRubricDevelopment,
            etRubricGrammar, etRubricCritical, etRubricOther;
    Button btnCreate, btnCancel;
    DatabaseReference classroomsRef;

    private AlertDialog loadingDialog;

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
        //etRubricDevelopment = findViewById(R.id.etRubricDevelopment);
        etRubricGrammar = findViewById(R.id.etRubricGrammar);
        etRubricCritical = findViewById(R.id.etRubricCritical);
        etRubricOther = findViewById(R.id.etRubricOther);

        btnCreate = findViewById(R.id.btnCreate);
        btnCancel = findViewById(R.id.btnCancel);



        btnCancel.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> {

            btnCreate.setEnabled(false);//disable immediately to prevent re-click

            String roomName = etRoomName.getText().toString().trim();
            if (roomName.isEmpty()) {
                Toast.makeText(this, "Enter Room Name", Toast.LENGTH_SHORT).show();
                btnCreate.setEnabled(true); // re-enable if validation fails
                return;
            }



            // RUBRIC VALIDATION
            int content = parseEditText(etRubricContent);
            int organization = parseEditText(etRubricOrganization);
           // int development = parseEditText(etRubricDevelopment);
            int grammar = parseEditText(etRubricGrammar);
            int critical = parseEditText(etRubricCritical);

            int total = content + organization + grammar + critical;

            if (total != 100) {
                Toast.makeText(this, "Rubrics must sum exactly to 100%", Toast.LENGTH_SHORT).show();
                btnCreate.setEnabled(true);
                return;
            }

            showLoadingDialog();


            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String teacherEmail = prefs.getString("teacherEmail", "unknown");

            // Date and time
            String dateTime = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                    .format(new Date());

            generateUniqueRoomCode(roomCode -> {
                String roomId = classroomsRef.push().getKey();


                // Rubrics map (Firebase-safe keys)
                Map<String, Object> rubrics = new HashMap<>();
                rubrics.put("Content and Ideas", etRubricContent.getText().toString().trim());
                rubrics.put("Organization and Structure", etRubricOrganization.getText().toString().trim());
               // rubrics.put("Development and Support", etRubricDevelopment.getText().toString().trim());
                rubrics.put("Language Use and Style", etRubricGrammar.getText().toString().trim());
                rubrics.put("Grammar, Mechanics, and Formatting", etRubricCritical.getText().toString().trim());
                rubrics.put("Notes", etRubricOther.getText().toString().trim());

                // Classroom map
                Map<String, Object> classroomMap = new HashMap<>();
                classroomMap.put("classroom_name", roomName);
                classroomMap.put("classroom_owner", teacherEmail);
                classroomMap.put("status", "active");
                classroomMap.put("created_at", dateTime);
                classroomMap.put("room_code", roomCode);
                classroomMap.put("rubrics", rubrics);

                // Save to Firebase
                classroomsRef.child(roomId).setValue(classroomMap)
                        .addOnSuccessListener(aVoid -> {
                            hideLoadingDialog();
                            Toast.makeText(this, "Room Created Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            hideLoadingDialog();
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnCreate.setEnabled(true); // allow retry only if fail
                        });
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
                        Toast.makeText(AddRoom_Teacher.this, "Error checking code", Toast.LENGTH_SHORT).show();
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

    private int parseEditText(EditText et) {
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_creating_room_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            loadingDialog = builder.create();
        }
        loadingDialog.show();
    }


    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
