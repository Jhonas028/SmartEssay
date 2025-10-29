package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.API.SendBulkEmail;
import com.example.smartessay.MainActivity;
import com.example.smartessay.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class CreateTeacherAcc extends AppCompatActivity {

    // Declare UI elements
    Button signupBTN;
    EditText emailET, fnameET, lnameET, passET, conpassET;
    TextInputLayout emailTV, fnameTV, lnameTV, passTV, conpassTV;
    String email, fname, lname, pass, confPass;
    TextView logInTV;
    OTPgenerator otp; // For generating OTP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_teacher_acc);

        // Account type passed from previous activity
        String account = getIntent().getStringExtra("account");

        // Connect UI components with layout IDs
        signupBTN = findViewById(R.id.signupBTN);
        emailET = findViewById(R.id.emailET);
        fnameET = findViewById(R.id.fnameET);
        lnameET = findViewById(R.id.lnameET);
        passET = findViewById(R.id.passET);
        conpassET = findViewById(R.id.conpassET);

        emailTV = findViewById(R.id.emailTV);
        fnameTV = findViewById(R.id.fnameTV);
        lnameTV = findViewById(R.id.lnameTV);
        passTV = findViewById(R.id.passTV);
        conpassTV = findViewById(R.id.conpassTV);

        logInTV = findViewById(R.id.logInTV);

        // Clears helper text when the input box is clicked
        clearHelperTextOnFocus(emailET, emailTV);
        clearHelperTextOnFocus(fnameET, fnameTV);
        clearHelperTextOnFocus(lnameET, lnameTV);
        clearHelperTextOnFocus(passET, passTV);
        clearHelperTextOnFocus(conpassET, conpassTV);

        // When "Log in" text is clicked, go back to MainActivity
        logInTV.setOnClickListener(view -> {
            startActivity(new Intent(CreateTeacherAcc.this, MainActivity.class));
            finish();
        });

        // When "Sign Up" button is clicked
        signupBTN.setOnClickListener(v -> {

            // If validation fails, show a message and stop the process

            /*            if (!validateInputs()) {
                Toast.makeText(getApplicationContext(), "Validation failed", Toast.LENGTH_SHORT).show();
                return; // exit the function
            }


            // If valid, get the email and check if it already exists in Firebase
            email = emailET.getText().toString().trim();
            checkEmailExists(account, email);*/
            if (true) {
                email = emailET.getText().toString().trim();
                checkEmailExists(account, email); // check Firebase if email already exists
            } else {
                Toast.makeText(getApplicationContext(), "Validation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Check if email already exists in Firebase (students or teachers)
    private void checkEmailExists(String account, String email) {
        // Firebase reference for students node: users/students
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("users").child("students");
        // Firebase reference for teachers node: users/teachers
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("users").child("teachers");

        // Search in students first
        studentsRef.orderByChild("email").equalTo(email) // look for child "email" == input email
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // If snapshot has data → email already exists as student
                        if (snapshot.exists()) {
                            emailTV.setHelperText("Email already exists (student).");
                            Toast.makeText(getApplicationContext(), "Email already registered as Student.", Toast.LENGTH_SHORT).show();
                        } else {
                            // If not found in students, check in teachers
                            teachersRef.orderByChild("email").equalTo(email)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot2) {
                                            // If snapshot2 has data → email already exists as teacher
                                            if (snapshot2.exists()) {
                                                emailTV.setHelperText("Email already exists (teacher).");
                                                Toast.makeText(getApplicationContext(), "Email already registered as Teacher.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // If email not found in both students and teachers → register teacher
                                                registerTeacher(account);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            // If Firebase failed to read teachers node
                                            Toast.makeText(getApplicationContext(), "Error checking teachers.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // If Firebase failed to read students node
                        Toast.makeText(getApplicationContext(), "Error checking students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Register teacher if email is new
    private void registerTeacher(String account) {

        otp = new OTPgenerator(); // create OTP object
        String myOTP = otp.generateOTP(); // generate OTP

        try {
            SendBulkEmail.sendOtpEmail(myOTP, emailET.getText().toString().trim());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("SendBulkEmail", "testAPI", e);
            Toast.makeText(this, "Failed to send OTP email.", Toast.LENGTH_SHORT).show();
        }

        // Get input values
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        pass = passET.getText().toString().trim();

        // Format current time in Manila timezone
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String formattedTime = sdf.format(new Date());
        long timestampRaw = System.currentTimeMillis(); // raw milliseconds

        // Firebase reference: pending_verification/teachers
        DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                .getReference("pending_verification")
                .child("teachers")
                .push(); // push = create unique random key

        // Data to store in Firebase
        HashMap<String, Object> pendingData = new HashMap<>();
        pendingData.put("email", email);
        pendingData.put("first_name", fname);
        pendingData.put("last_name", lname);
        pendingData.put("password", pass);
        pendingData.put("status", "pending");
        pendingData.put("otp", myOTP);
        pendingData.put("created_at", formattedTime);
        pendingData.put("updated_at", formattedTime);
        pendingData.put("timestamp_raw", timestampRaw);

        // Save to Firebase
        pendingRef.setValue(pendingData)
                .addOnSuccessListener(aVoid -> {
                    // If successful, go to OTP verification screen
                    Intent intent = new Intent(CreateTeacherAcc.this, OTPverifyTeacher.class);
                    intent.putExtra("account", account);
                    intent.putExtra("email_teacher", email);
                    intent.putExtra("otp_code_teacher", myOTP);
                    intent.putExtra("first_name", fname);
                    intent.putExtra("last_name", lname);
                    intent.putExtra("password", pass);
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "OTP sent. Please verify.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // If failed, show error
                    Toast.makeText(getApplicationContext(), "Failed to start verification.", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", e.getMessage());
                });
    }

    // Check if inputs are valid
    private boolean validateInputs() {
        // Get all input values
        email = emailET.getText().toString().trim();
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        pass = passET.getText().toString().trim();
        confPass = conpassET.getText().toString().trim();

        boolean isValid = true;

        // Validate each field
        isValid &= setError(emailTV, email.isEmpty() || !email.matches("^[a-z]+\\.[a-z]+@sanpablo\\.sti\\.edu\\.ph$"), "Enter your STI San Pablo teacher email address.");
        isValid &= setError(fnameTV, fname.isEmpty(), "First name is required.");
        isValid &= setError(lnameTV, lname.isEmpty(), "Last name is required.");

        String passwordError = getPasswordError(pass);
        if (pass.isEmpty()) {
            passTV.setHelperText("Password is required.");
            isValid = false;
        } else if (!passwordError.isEmpty()) {
            passTV.setHelperText(passwordError);
            isValid = false;
        } else {
            passTV.setHelperText(null);
        }

        // Confirm password check
        if (confPass.isEmpty()) {
            conpassTV.setHelperText("Confirm password is required.");
            isValid = false;

        } else if (!confPass.equals(pass)) { // If confirm password does not match
            conpassTV.setHelperText("Passwords do not match.");
            isValid = false;
        } else {
            conpassTV.setHelperText(null);
        }

        return isValid; // true if all fields are valid
    }

    // Helper function for setting error messages
    private boolean setError(TextInputLayout layout, boolean condition, String message) {
        if (condition) { // if condition true → show error
            layout.setHelperText(message);
            return false;
        } else { // if condition false → clear error
            layout.setHelperText(null);
            return true;
        }
    }

    // Clears helper text when input is focused
    private void clearHelperTextOnFocus(EditText editText, TextInputLayout layout) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                layout.setHelperText(null);
            }
        });
    }

    // Check if password meets requirements
// ✅ NEW: Improved password feedback system
    public String getPasswordError(String password) {
        StringBuilder error = new StringBuilder();

        if (password.length() < 8 || password.length() > 15) {
            error.append("• Password must be 8–15 characters long.\n");
        }
        if (!password.matches(".*[A-Za-z].*")) {
            error.append("• Password must include at least one letter.\n");
        }
        if (!password.matches(".*\\d.*")) {
            error.append("• Password must include at least one number.\n");
        }
        if (!password.matches(".*[!@#$%^&.*].*")) {
            error.append("• Password must include at least one special character (!@#$%^&.*).\n");
        }

        return error.toString().trim();
    }
}
