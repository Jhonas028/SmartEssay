package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.smartessay.MainActivity;
import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class OTPverifyTeacher extends AppCompatActivity {

    EditText code1, code2, code3, code4, code5, code6;
    TextView textTimer, testResendOTP;
    Button buttonVerify;
    OTPgenerator otpGenerator;
    private boolean isOtpValid = true;
    private String currentOtp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otpverification);

        String account = getIntent().getStringExtra("account");
        currentOtp = getIntent().getStringExtra("otp_code_teacher");

        otpGenerator = new OTPgenerator();

        code1 = findViewById(R.id.inputCode1);
        code2 = findViewById(R.id.inputCode2);
        code3 = findViewById(R.id.inputCode3);
        code4 = findViewById(R.id.inputCode4);
        code5 = findViewById(R.id.inputCode5);
        code6 = findViewById(R.id.inputCode6);
        buttonVerify = findViewById(R.id.buttonVerify);
        textTimer = findViewById(R.id.textTimer);
        testResendOTP = findViewById(R.id.testResendOTP);

        code1.addTextChangedListener(new OTPTextWatcher(code1, code2, null));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3, code1));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4, code2));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5, code3));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6, code4));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null, code5));

        startOTPTimer(60000);

        testResendOTP.setOnClickListener(v -> {
            clearInputs();
            currentOtp = otpGenerator.generateOTP();
            isOtpValid = true;

            startOTPTimer(60000);

            Intent intent = getIntent();
            String email = intent.getStringExtra("email_teacher");
            String firstName = intent.getStringExtra("first_name");
            String lastName = intent.getStringExtra("last_name");
            String pass = intent.getStringExtra("password");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            String formattedTime = sdf.format(new Date());

            long timestampRaw = System.currentTimeMillis();

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("pending_verification")
                    .child("teachers")
                    .push();

            HashMap<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("first_name", firstName);
            userData.put("last_name", lastName);
            userData.put("password", pass);
            userData.put("otp", currentOtp);
            userData.put("timestamp", formattedTime);
            userData.put("timestamp_raw", timestampRaw);

            /* API FOR EMAIL, PLEASE DO NOT REMOVE THIS
            try {
                EmailAPI.sendOtpEmail(currentOtp,email);
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



        buttonVerify.setOnClickListener(v -> {
            String enteredOtp = getEnteredOTP();

            if (!isOtpValid) {
                Toast.makeText(getApplicationContext(), "OTP expired. Please resend.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!enteredOtp.equals(currentOtp)) {
                Toast.makeText(getApplicationContext(), "Incorrect OTP.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                    .getReference("pending_verification")
                    .child("teachers");

            pendingRef.get().addOnSuccessListener(snapshot -> {
                for (DataSnapshot teacherSnap : snapshot.getChildren()) {
                    String otpFromDb = teacherSnap.child("otp").getValue(String.class);
                    if (otpFromDb != null && otpFromDb.equals(currentOtp)) {
                        String email = teacherSnap.child("email").getValue(String.class);
                        String firstName = teacherSnap.child("first_name").getValue(String.class);
                        String lastName = teacherSnap.child("last_name").getValue(String.class);
                        String password = teacherSnap.child("password").getValue(String.class);

                        DatabaseReference teacherRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child("teachers")
                                .push();

                        String teacherId = teacherRef.getKey(); // <-- Get unique ID

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                        String createdTime = sdf.format(new Date());

                        HashMap<String, Object> teacherData = new HashMap<>();
                        teacherData.put("email", email);
                        teacherData.put("first_name", firstName);
                        teacherData.put("last_name", lastName);
                        teacherData.put("password", password);
                        teacherData.put("status", "active");
                        teacherData.put("created_at", createdTime);
                        teacherData.put("updated_at", createdTime);

                        teacherRef.setValue(teacherData)
                                .addOnSuccessListener(aVoid -> {

                                    // Store ID so it can be used in other classes
                                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                    prefs.edit().putString("teacherId", teacherId).apply();

                                    teacherSnap.getRef().removeValue();
                                    Toast.makeText(getApplicationContext(), "Account verified!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                });
                        break;
                    }
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
                    return true;
                }
                return false;
            });
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().trim().isEmpty() && nextEditText != null) {
                nextEditText.requestFocus();
            }
        }
        @Override public void afterTextChanged(Editable s) {}
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

    private void clearInputs() {
        code1.setText("");
        code2.setText("");
        code3.setText("");
        code4.setText("");
        code5.setText("");
        code6.setText("");
        code1.requestFocus();
    }
}
