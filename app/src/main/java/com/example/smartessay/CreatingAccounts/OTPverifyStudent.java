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
import com.example.smartessay.StudentHomepage.StudentHPActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;

public class OTPverifyStudent extends AppCompatActivity {

    EditText code1, code2, code3, code4, code5, code6;
    TextView textTimer, testResendOTP;
    Button buttonVerify;
    OTPgenerator otpGenerator;

    private boolean isOtpValid = true;
    private String currentOtp = "";
    private String studentNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otpverification);

        // Get data from previous screen
        String account = getIntent().getStringExtra("account");
        studentNumber = getIntent().getStringExtra("studentNumber");
        currentOtp = getIntent().getStringExtra("otp_code_student");

        // Initialize OTP Generator
        otpGenerator = new OTPgenerator();

        // Find views
        code1 = findViewById(R.id.inputCode1);
        code2 = findViewById(R.id.inputCode2);
        code3 = findViewById(R.id.inputCode3);
        code4 = findViewById(R.id.inputCode4);
        code5 = findViewById(R.id.inputCode5);
        code6 = findViewById(R.id.inputCode6);
        buttonVerify = findViewById(R.id.buttonVerify);
        textTimer = findViewById(R.id.textTimer);
        testResendOTP = findViewById(R.id.testResendOTP);

        // Setup OTP field focus
        code1.addTextChangedListener(new OTPTextWatcher(code1, code2, null));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3, code1));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4, code2));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5, code3));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6, code4));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null, code5));

        // Start timer
        startOTPTimer(60000);

        // Resend OTP
        testResendOTP.setOnClickListener(v -> {
            clearInputs();
            currentOtp = otpGenerator.generateOTP();
            Log.i("new_student_otp", currentOtp);
            isOtpValid = true;

            // TODO: Re-send email here (optional)
            startOTPTimer(60000);
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

            // ✅ Fetch from pending_verification and move to user/student
            DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                    .getReference("pending_verification")
                    .child(studentNumber);

            pendingRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String email = snapshot.child("email").getValue(String.class);
                    String fullname = snapshot.child("fullname").getValue(String.class);
                    String password = snapshot.child("password").getValue(String.class);

                    DatabaseReference studentRef = FirebaseDatabase.getInstance()
                            .getReference("user")
                            .child("student")
                            .child(studentNumber);

                    HashMap<String, Object> studentData = new HashMap<>();
                    studentData.put("email", email);
                    studentData.put("fullname", fullname);
                    studentData.put("studentNumber", studentNumber);
                    studentData.put("password", password);
                    studentData.put("timestamp", System.currentTimeMillis());

                    studentRef.setValue(studentData)
                            .addOnSuccessListener(aVoid -> {
                                pendingRef.removeValue(); // 🧹 Clean up
                                Toast.makeText(getApplicationContext(), "Account verified!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), StudentHPActivity.class));
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
