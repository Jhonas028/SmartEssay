package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.example.smartessay.StudentHomepage.FragmentHP_Student;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class OTPverifyStudent extends AppCompatActivity {

    EditText code1, code2, code3, code4, code5, code6;
    TextView textTimer, testResendOTP;
    Button buttonVerify;
    OTPgenerator otpGenerator;

    private boolean isOtpValid = true;
    private String currentOtp = "";
    private String studentNumber;
    private String formattedTime = "";

    // Data from CreateStudentAcc
    private String email, firstName, lastName, password, createdAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otpverification);

        // Get extras
        String account = getIntent().getStringExtra("account");
        studentNumber = getIntent().getStringExtra("studentNumber");
        currentOtp = getIntent().getStringExtra("otp_code_student");
        email = getIntent().getStringExtra("email_student");
        firstName = getIntent().getStringExtra("first_name");
        lastName = getIntent().getStringExtra("last_name");
        password = getIntent().getStringExtra("password");
        createdAt = getIntent().getStringExtra("created_at");

        otpGenerator = new OTPgenerator();

        // Init views
        code1 = findViewById(R.id.inputCode1);
        code2 = findViewById(R.id.inputCode2);
        code3 = findViewById(R.id.inputCode3);
        code4 = findViewById(R.id.inputCode4);
        code5 = findViewById(R.id.inputCode5);
        code6 = findViewById(R.id.inputCode6);
        buttonVerify = findViewById(R.id.buttonVerify);
        textTimer = findViewById(R.id.textTimer);
        testResendOTP = findViewById(R.id.testResendOTP);

        // Set OTP watchers
        code1.addTextChangedListener(new OTPTextWatcher(code1, code2, null));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3, code1));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4, code2));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5, code3));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6, code4));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null, code5));

        startOTPTimer(60000);

        // Resend OTP
        testResendOTP.setOnClickListener(v -> {
            clearInputs();
            currentOtp = otpGenerator.generateOTP();
            Log.i("new_student_otp", currentOtp);
            isOtpValid = true;
            startOTPTimer(60000);

            // Update Firebase pending_verification
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            formattedTime = sdf.format(new Date());

            DatabaseReference myRef = FirebaseDatabase.getInstance()
                    .getReference("pending_verification")
                    .child("students")
                    .child(studentNumber);

            HashMap<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("first_name", firstName);
            userData.put("last_name", lastName);
            userData.put("password", password);
            userData.put("status", "pending");
            userData.put("otp", currentOtp);
            userData.put("created_at", createdAt);
            userData.put("updated_at", formattedTime);

            /* Uncomment to send OTP email
            try {
                EmailAPI.sendOtpEmail(currentOtp, email);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }*/


            myRef.setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getApplicationContext(), "OTP re-sent and updated in pending.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getApplicationContext(), "Failed to update OTP.", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", e.getMessage());
                    });
        });

        // Verify OTP
        buttonVerify.setOnClickListener(v -> {
            String enteredOtp = getEnteredOTP();

            if (!isOtpValid) {
                Toast.makeText(getApplicationContext(), "OTP has expired. Please resend.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!enteredOtp.equals(currentOtp)) {
                Toast.makeText(getApplicationContext(), "OTP didn't match.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                    .getReference("pending_verification")
                    .child("students")
                    .child(studentNumber);

            pendingRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    // Create student account in users/students
                    DatabaseReference studentRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child("students")
                            .child(studentNumber);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                    String accountCreatedTime = sdf.format(new Date());

                    HashMap<String, Object> studentData = new HashMap<>();
                    studentData.put("email", email);
                    studentData.put("first_name", firstName);
                    studentData.put("last_name", lastName);
                    studentData.put("studentNumber", studentNumber);
                    studentData.put("password", password);
                    studentData.put("status", "active");
                    studentData.put("created_at", createdAt);
                    studentData.put("updated_at", accountCreatedTime);

                    studentRef.setValue(studentData)
                            .addOnSuccessListener(aVoid -> {

                                getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("studentNumber", studentNumber)
                                        .apply();


                                pendingRef.removeValue();
                                Toast.makeText(getApplicationContext(), "Account verified!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), FragmentHP_Student.class));
                                finish();
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "No pending record found.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private String getEnteredOTP() {
        return code1.getText().toString().trim() +
                code2.getText().toString().trim() +
                code3.getText().toString().trim() +
                code4.getText().toString().trim() +
                code5.getText().toString().trim() +
                code6.getText().toString().trim();
    }

    public void clearInputs() {
        code1.setText("");
        code2.setText("");
        code3.setText("");
        code4.setText("");
        code5.setText("");
        code6.setText("");
        code1.requestFocus();
    }

    private class OTPTextWatcher implements TextWatcher {
        private final EditText currentEditText;
        private final EditText nextEditText;
        private final EditText previousEditText;

        OTPTextWatcher(EditText currentEditText, EditText nextEditText, EditText previousEditText) {
            this.currentEditText = currentEditText;
            this.nextEditText = nextEditText;
            this.previousEditText = previousEditText;

            this.currentEditText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        currentEditText.getText().toString().isEmpty() &&
                        previousEditText != null) {
                    previousEditText.requestFocus();
                    previousEditText.setSelection(previousEditText.getText().length());
                    return true;
                }
                return false;
            });
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().trim().isEmpty() && nextEditText != null) {
                nextEditText.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    private void startOTPTimer(long durationInMillis) {
        testResendOTP.setEnabled(false);

        new CountDownTimer(durationInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                String time = String.format(Locale.getDefault(), "%02d:%02d",
                        millisUntilFinished / 60000,
                        (millisUntilFinished % 60000) / 1000);
                textTimer.setText(time);
            }

            public void onFinish() {
                textTimer.setText("00:00");
                testResendOTP.setEnabled(true);
                testResendOTP.setVisibility(View.VISIBLE);
                isOtpValid = false;
            }
        }.start();
    }
}
