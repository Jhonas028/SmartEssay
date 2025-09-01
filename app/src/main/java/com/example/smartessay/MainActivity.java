package com.example.smartessay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.CreatingAccounts.ChoosingAccounts;
import com.example.smartessay.StudentHomepage.FragmentHP_Student;
import com.example.smartessay.TeacherHomepage.FragmentHP_Teacher;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button signinBTN;
    private TextView signUpTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextText2);
        signinBTN = findViewById(R.id.signinBTN);
        signUpTextView = findViewById(R.id.signupTV);

        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(this, ChoosingAccounts.class));
        });

        signinBTN.setOnClickListener(v -> {
            String email = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            authenticateUser(email, password);
        });
    }

    private void authenticateUser(String email, String password) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Check teachers
        usersRef.child("teachers").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot teacherSnapshot) {
                        if (teacherSnapshot.exists()) {
                            for (DataSnapshot teacher : teacherSnapshot.getChildren()) {
                                String storedPassword = teacher.child("password").getValue(String.class);
                                String status = teacher.child("status").getValue(String.class);

                                if (storedPassword != null && storedPassword.equals(password)) {
                                    if ("active".equalsIgnoreCase(status)) {
                                        saveUserSession("teacherId", teacher.getKey());
                                        saveUserSession("teacherEmail", email);
                                        Toast.makeText(MainActivity.this, "Teacher login successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(MainActivity.this, FragmentHP_Teacher.class));
                                        finish();
                                        return;
                                    } else {
                                        Toast.makeText(MainActivity.this, "Account not active. Please verify.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                        }

                        // If not found in teachers, check students
                        usersRef.child("students").orderByChild("email").equalTo(email)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot studentSnapshot) {
                                        if (studentSnapshot.exists()) {
                                            for (DataSnapshot student : studentSnapshot.getChildren()) {
                                                String storedPassword = student.child("password").getValue(String.class);
                                                String status = student.child("status").getValue(String.class);

                                                if (storedPassword != null && storedPassword.equals(password)) {
                                                    if ("active".equalsIgnoreCase(status)) {
                                                        saveUserSession("studentId", student.getKey());
                                                        Toast.makeText(MainActivity.this, "Student login successful", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(MainActivity.this, FragmentHP_Student.class));
                                                        finish();
                                                        return;
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Account not active. Please verify.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                        Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserSession(String key, String id) {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().putString(key, id).apply();
    }
}
