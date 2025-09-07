package com.example.smartessay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

// MainActivity is the login screen for both students and teachers
public class MainActivity extends AppCompatActivity {

    // UI elements for login
    private EditText usernameEditText, passwordEditText;
    private Button signinBTN;
    private TextView signUpTextView;

    // Dialog to show loading animation while authenticating
    private android.app.Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link XML UI elements to Java code
        usernameEditText = findViewById(R.id.emailLoginTV);
        passwordEditText = findViewById(R.id.passwordLoginTV);
        signinBTN = findViewById(R.id.signinBTN);
        signUpTextView = findViewById(R.id.signupTV);

        // When the "Sign Up" text is clicked, go to ChoosingAccounts activity
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(this, ChoosingAccounts.class));
        });

        // When "Sign In" button is clicked
        signinBTN.setOnClickListener(v -> {
            String email = usernameEditText.getText().toString().trim(); // Get input email
            String password = passwordEditText.getText().toString().trim(); // Get input password

            // Simple check: make sure both fields are filled
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Authenticate user with Firebase
            authenticateUser(email, password);
        });
    }

    // Method to authenticate user against Firebase Realtime Database
    private void authenticateUser(String email, String password) {
        showLoadingDialog("Signing in..."); // Show loading spinner

        // Firebase syntax:
        // Get a reference to the "users" node in Realtime Database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // First, check if the email belongs to a teacher
        // orderByChild("email") -> orders children by their "email" field
        // equalTo(email) -> filters nodes where email matches
        usersRef.child("teachers").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() { // Attach a one-time listener
                    @Override
                    public void onDataChange(DataSnapshot teacherSnapshot) {
                        // onDataChange is called when Firebase returns the data
                        if (teacherSnapshot.exists()) { // If a teacher with that email exists
                            for (DataSnapshot teacher : teacherSnapshot.getChildren()) {
                                // Get the password stored in Firebase for this teacher
                                String storedPassword = teacher.child("password").getValue(String.class);
                                String status = teacher.child("status").getValue(String.class);

                                // Compare input password with Firebase password
                                if (storedPassword != null && storedPassword.equals(password)) {
                                    // Check if account is active
                                    if ("active".equalsIgnoreCase(status)) {
                                        hideLoadingDialog(); // Close loading spinner
                                        saveUserSession("teacherId", teacher.getKey()); // Save teacher's unique ID locally
                                        saveUserSession("teacherEmail", email); // Save teacher email locally
                                        Toast.makeText(MainActivity.this, "Teacher login successful", Toast.LENGTH_SHORT).show();

                                        // Start teacher homepage and send email via Intent
                                        Intent intent = new Intent(MainActivity.this, FragmentHP_Teacher.class);
                                        intent.putExtra("teacherEmail", email); // pass email
                                        Log.i("emailTeaher", "Email sent: " + email);
                                        startActivity(intent);
                                        finish(); // Close login activity
                                        return;
                                    } else {
                                        hideLoadingDialog();
                                        Toast.makeText(MainActivity.this, "Account not active. Please verify.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                        }

                        // If email not found in teachers, check students
                        usersRef.child("students").orderByChild("email").equalTo(email)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot studentSnapshot) {
                                        if (studentSnapshot.exists()) { // If a student with that email exists
                                            for (DataSnapshot student : studentSnapshot.getChildren()) {
                                                // Get password and status from Firebase
                                                String storedPassword = student.child("password").getValue(String.class);
                                                String status = student.child("status").getValue(String.class);

                                                // Compare input password with Firebase password
                                                if (storedPassword != null && storedPassword.equals(password)) {
                                                    // Check if account is active
                                                    if ("active".equalsIgnoreCase(status)) {
                                                        saveUserSession("studentId", student.getKey()); // Save student ID locally

                                                        Toast.makeText(MainActivity.this, "Student login successful", Toast.LENGTH_SHORT).show();
                                                        // Start student homepage and pass email
                                                        Intent intent = new Intent(MainActivity.this, FragmentHP_Student.class);
                                                        intent.putExtra("studentEmail", email); // pass email
                                                        startActivity(intent);
                                                        finish(); // Close login activity
                                                        return;
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Account not active. Please verify.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                        // If no teacher or student found with this email
                                        Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                        hideLoadingDialog();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        // Called if Firebase operation fails
                                        hideLoadingDialog();
                                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Called if Firebase operation fails
                        hideLoadingDialog();
                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Save user session locally using SharedPreferences
    private void saveUserSession(String key, String id) {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().putString(key, id).apply(); // Save key-value pair
    }

    // Show a loading dialog while authenticating
    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_creating_room_loading, null);
        ((android.widget.TextView) dialogView.findViewById(R.id.tvLoadingMessage)).setText(message);

        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    // Hide the loading dialog when done
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
