package com.example.smartessay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.CreatingAccounts.ChoosingAccounts;
import com.example.smartessay.StudentHomepage.StudentHPActivity;
import com.example.smartessay.TeacherHomepage.TeacherHPActivity;
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

        // Initialize views
        usernameEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextText2);
        signinBTN = findViewById(R.id.signinBTN);
        signUpTextView = findViewById(R.id.signupTV);

        // Set click listeners
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
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("user");

        // Check teacher credentials first
        DatabaseReference teachersRef = userRef.child("teacher");
        teachersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                                String storedPassword = teacherSnapshot.child("password").getValue(String.class);
                                if (storedPassword != null && storedPassword.equals(password)) {
                                    // Teacher login successful
                                    Toast.makeText(MainActivity.this, "Teacher login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(MainActivity.this, TeacherHPActivity.class));
                                    finish();
                                    return;
                                }
                            }
                        }

                        // If not teacher, check student credentials
                        DatabaseReference studentsRef = userRef.child("student");
                        studentsRef.orderByChild("email").equalTo(email)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                                                String storedPassword = studentSnapshot.child("password").getValue(String.class);
                                                if (storedPassword != null && storedPassword.equals(password)) {
                                                    // Student login successful
                                                    Toast.makeText(MainActivity.this, "Student login successful", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(MainActivity.this, StudentHPActivity.class));
                                                    finish();
                                                    return;
                                                }
                                            }
                                        }
                                        // If we reach here, login failed
                                        Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
