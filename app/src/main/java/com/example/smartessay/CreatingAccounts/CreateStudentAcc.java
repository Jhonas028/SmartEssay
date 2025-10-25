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

import com.example.smartessay.API.EmailAPI;
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

public class CreateStudentAcc extends AppCompatActivity {

    // Declare UI elements
    Button signupBTN;
    EditText emailET, fnameET, lnameET, snumET, passET, conpassET;
    TextInputLayout emailTV, fnameTV, lnameTV, snumTV, passTV, conpassTV;
    String email, fname, lname, stuNum, pass, confPass;
    TextView logInTV;
    OTPgenerator otp; // OTP generator object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_student_acc);

        // Account type (student/teacher) passed from previous activity
        String account = getIntent().getStringExtra("account");

        // Connect UI components with layout IDs
        signupBTN = findViewById(R.id.signupBTN);
        emailET = findViewById(R.id.emailET);
        fnameET = findViewById(R.id.fnameET);
        lnameET = findViewById(R.id.lnameET);
        snumET = findViewById(R.id.snumET);
        passET = findViewById(R.id.passET);
        conpassET = findViewById(R.id.conpassET);

        emailTV = findViewById(R.id.emailTV);
        snumTV = findViewById(R.id.snumTV);
        fnameTV = findViewById(R.id.fnameTV);
        lnameTV = findViewById(R.id.lnameTV);
        passTV = findViewById(R.id.passTV);
        conpassTV = findViewById(R.id.conpassTV);

        logInTV = findViewById(R.id.logInTV);

        // Clear error/helper text when field is focused
        clearHelperTextOnFocus(emailET, emailTV);
        clearHelperTextOnFocus(fnameET, fnameTV);
        clearHelperTextOnFocus(lnameET, lnameTV);
        clearHelperTextOnFocus(snumET, snumTV);
        clearHelperTextOnFocus(passET, passTV);
        clearHelperTextOnFocus(conpassET, conpassTV);

        // When "Log In" is clicked, go back to main activity
        logInTV.setOnClickListener(view -> {
            startActivity(new Intent(CreateStudentAcc.this, MainActivity.class));
            finish();
        });

        // When "Sign Up" button is clicked
        signupBTN.setOnClickListener(v -> {
            // NOTE: This code currently uses "if (true)" → it will always run signup
            // Normally, validateInputs() should be here to check form correctness
            if (validateInputs()) {
                email = emailET.getText().toString().trim();
                checkEmailExists(account, email); // check Firebase if email already exists
            } else {
                Toast.makeText(getApplicationContext(), "Validation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Check if the email already exists in Firebase
    private void checkEmailExists(String account, String email) {
        // Reference to users/students node
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("users").child("students");
        // Reference to users/teachers node
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("users").child("teachers");

        // Check in students node first
        studentsRef.orderByChild("email").equalTo(email) // search for email field in students
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // If snapshot has data, email exists in students
                        if (snapshot.exists()) {
                            emailTV.setHelperText("Email already exists (student).");
                            Toast.makeText(getApplicationContext(), "Email already registered as Student.", Toast.LENGTH_SHORT).show();
                        } else {
                            // If not found in students, check teachers
                            teachersRef.orderByChild("email").equalTo(email)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot2) {
                                            // If snapshot2 has data, email exists in teachers
                                            if (snapshot2.exists()) {
                                                emailTV.setHelperText("Email already exists (teacher).");
                                                Toast.makeText(getApplicationContext(), "Email already registered as Teacher.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // If not found in both → proceed with student registration
                                                registerStudent(account);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            // If Firebase fails to check teachers node
                                            Toast.makeText(getApplicationContext(), "Error checking teachers.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // If Firebase fails to check students node
                        Toast.makeText(getApplicationContext(), "Error checking students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Register new student in pending_verification node
    private void registerStudent(String account) {
        otp = new OTPgenerator(); // create OTP object
        String myOTP = otp.generateOTP(); // generate OTP

        try {
            SendBulkEmail.sendOtpEmail(myOTP, emailET.getText().toString().trim());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("SendBulkEmail", "testAPI", e);
            Toast.makeText(this, "Failed to send OTP email.", Toast.LENGTH_SHORT).show();
        }

        // Get form input values
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        stuNum = snumET.getText().toString().trim();
        pass = passET.getText().toString().trim();

        // Current time in Manila timezone
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String formattedTime = sdf.format(new Date());

        // Firebase reference: pending_verification/students/{studentNumber}
        DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                .getReference("pending_verification")
                .child("students")
                .child(stuNum); // use student number as unique ID

        // Prepare data to save
        HashMap<String, Object> pendingData = new HashMap<>();
        pendingData.put("email", email);
        pendingData.put("first_name", fname);
        pendingData.put("last_name", lname);
        pendingData.put("password", pass);
        pendingData.put("status", "pending");
        pendingData.put("otp", myOTP);
        pendingData.put("created_at", formattedTime);
        pendingData.put("updated_at", formattedTime);

        // Save data into Firebase
        pendingRef.setValue(pendingData)
                .addOnSuccessListener(aVoid -> {
                    // If success, go to OTP verification screen
                    Intent intent = new Intent(CreateStudentAcc.this, OTPverifyStudent.class);
                    intent.putExtra("account", account);
                    intent.putExtra("email_student", email);
                    intent.putExtra("otp_code_student", myOTP);
                    intent.putExtra("first_name", fname);
                    intent.putExtra("last_name", lname);
                    intent.putExtra("studentNumber", stuNum);
                    intent.putExtra("password", pass);
                    intent.putExtra("created_at", formattedTime);
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "OTP sent. Please verify.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // If failed to save in Firebase
                    Toast.makeText(getApplicationContext(), "Failed to start verification.", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", e.getMessage());
                });
    }

    // Validate form inputs
    private boolean validateInputs() {
        // Get all form inputs
        email = emailET.getText().toString().trim();
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        stuNum = snumET.getText().toString().trim();
        pass = passET.getText().toString().trim();
        confPass = conpassET.getText().toString().trim();

        boolean isValid = true;

        // Check each field and set error if invalid
        isValid &= setError(emailTV, email.isEmpty() || !email.matches("^[a-z]+\\.\\d{1,10}@sanpablo\\.sti\\.edu\\.ph$"), "Enter a valid STI San Pablo email.");
        isValid &= setError(fnameTV, fname.isEmpty(), "First name is required.");
        isValid &= setError(lnameTV, lname.isEmpty(), "Last name is required.");
        isValid &= setError(snumTV, stuNum.isEmpty() || !stuNum.matches("^\\d{10}$"), "Must be exactly 10 digits.");

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
        } else if (!confPass.equals(pass)) { // if confirm password does not match password
            conpassTV.setHelperText("Passwords do not match.");
            isValid = false;
        } else {
            conpassTV.setHelperText(null);
        }

        return isValid; // true if all fields valid
    }

    // Helper function for setting error messages
    private boolean setError(TextInputLayout layout, boolean condition, String message) {
        if (condition) { // if condition is true → show error
            layout.setHelperText(message);
            return false;
        } else { // if false → clear error
            layout.setHelperText(null);
            return true;
        }
    }

    // Clear helper text when input field is focused
    private void clearHelperTextOnFocus(EditText editText, TextInputLayout layout) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                layout.setHelperText(null);
            }
        });
    }
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

        return error.toString().trim(); // return all missing rules as one string
    }

    // Check if password is valid: must have letters, numbers, special chars, length 8–15
    public boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&.*])[A-Za-z\\d!@#$%^&.*]{8,15}$");
    }


}
