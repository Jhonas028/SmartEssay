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

import com.example.smartessay.MainActivity;
import com.example.smartessay.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class CreateStudentAcc extends AppCompatActivity {

    Button signupBTN;
    EditText emailET, fnameET, lnameET, snumET, passET, conpassET;
    TextInputLayout emailTV, fnameTV, lnameTV, snumTV, passTV, conpassTV;
    String email, fname, lname, stuNum, pass, confPass;
    TextView logInTV;
    OTPgenerator otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_student_acc);

        String account = getIntent().getStringExtra("account");

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

        clearHelperTextOnFocus(emailET, emailTV);
        clearHelperTextOnFocus(fnameET, fnameTV);
        clearHelperTextOnFocus(lnameET, lnameTV);
        clearHelperTextOnFocus(snumET, snumTV);
        clearHelperTextOnFocus(passET, passTV);
        clearHelperTextOnFocus(conpassET, conpassTV);

        logInTV.setOnClickListener(view -> {
            startActivity(new Intent(CreateStudentAcc.this, MainActivity.class));
            finish();
        });

        signupBTN.setOnClickListener(v -> {
            //correct code
            /*            if (validateInputs()) {
                email = emailET.getText().toString().trim();

                checkEmailExists(account, email);
            } else {
                Toast.makeText(getApplicationContext(), "Validation failed", Toast.LENGTH_SHORT).show();
            }*/
            if (true) {
                email = emailET.getText().toString().trim();

                checkEmailExists(account, email);
            } else {
                Toast.makeText(getApplicationContext(), "Validation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEmailExists(String account, String email) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("users").child("students");
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("users").child("teachers");

        // First check in students
        studentsRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            emailTV.setHelperText("Email already exists (student).");
                            Toast.makeText(getApplicationContext(), "Email already registered as Student.", Toast.LENGTH_SHORT).show();
                        } else {
                            // If not in students, check teachers
                            teachersRef.orderByChild("email").equalTo(email)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot2) {
                                            if (snapshot2.exists()) {
                                                emailTV.setHelperText("Email already exists (teacher).");
                                                Toast.makeText(getApplicationContext(), "Email already registered as Teacher.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // Not found in both → proceed
                                                registerStudent(account);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            Toast.makeText(getApplicationContext(), "Error checking teachers.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Error checking students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerStudent(String account) {
        otp = new OTPgenerator();
        String myOTP = otp.generateOTP();
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        stuNum = snumET.getText().toString().trim();
        pass = passET.getText().toString().trim();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        String formattedTime = sdf.format(new Date());

        DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                .getReference("pending_verification")
                .child("students")
                .child(stuNum);

        HashMap<String, Object> pendingData = new HashMap<>();
        pendingData.put("email", email);
        pendingData.put("first_name", fname);
        pendingData.put("last_name", lname);
        pendingData.put("password", pass);
        pendingData.put("status", "pending");
        pendingData.put("otp", myOTP);
        pendingData.put("created_at", formattedTime);
        pendingData.put("updated_at", formattedTime);

        pendingRef.setValue(pendingData)
                .addOnSuccessListener(aVoid -> {
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
                    Toast.makeText(getApplicationContext(), "Failed to start verification.", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", e.getMessage());
                });
    }

    private boolean validateInputs() {
        email = emailET.getText().toString().trim();
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        stuNum = snumET.getText().toString().trim();
        pass = passET.getText().toString().trim();
        confPass = conpassET.getText().toString().trim();

        boolean isValid = true;

        isValid &= setError(emailTV, email.isEmpty() || !email.matches("^[a-z]+\\.\\d{1,10}@sanpablo\\.sti\\.edu\\.ph$"), "Enter a valid STI San Pablo email.");
        isValid &= setError(fnameTV, fname.isEmpty(), "First name is required.");
        isValid &= setError(lnameTV, lname.isEmpty(), "Last name is required.");
        isValid &= setError(snumTV, stuNum.isEmpty() || !stuNum.matches("^\\d{10}$"), "Must be exactly 10 digits.");
        isValid &= setError(passTV, pass.isEmpty() || !isValidPassword(pass), "Password must be 8–15 characters long, and include letters, numbers, and special characters.");

        if (confPass.isEmpty()) {
            conpassTV.setHelperText("Confirm password is required.");
            isValid = false;
        } else if (!confPass.equals(pass)) {
            conpassTV.setHelperText("Passwords do not match.");
            isValid = false;
        } else {
            conpassTV.setHelperText(null);
        }

        return isValid;
    }

    private boolean setError(TextInputLayout layout, boolean condition, String message) {
        if (condition) {
            layout.setHelperText(message);
            return false;
        } else {
            layout.setHelperText(null);
            return true;
        }
    }

    private void clearHelperTextOnFocus(EditText editText, TextInputLayout layout) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                layout.setHelperText(null);
            }
        });
    }

    public boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&.*])[A-Za-z\\d!@#$%^&.*]{8,15}$");
    }
}
