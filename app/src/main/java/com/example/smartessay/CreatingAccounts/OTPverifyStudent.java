package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.example.smartessay.StudentHomepage.StudentHPActivity;

import java.util.Locale;

public class OTPverifyStudent extends AppCompatActivity {

    EditText code1,code2,code3,code4,code5,code6;
    TextView textTimer,testResendOTP;
    Button buttonVerify;
    OTPgenerator otpGenerator;

    private boolean isOtpValid = true;
    private String currentOtp = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otpverification);

         code1 = findViewById(R.id.inputCode1);
         code2 = findViewById(R.id.inputCode2);
         code3 = findViewById(R.id.inputCode3);
         code4 = findViewById(R.id.inputCode4);
         code5 = findViewById(R.id.inputCode5);
         code6 = findViewById(R.id.inputCode6);

        buttonVerify = findViewById(R.id.buttonVerify);

        textTimer = findViewById(R.id.textTimer);
        testResendOTP = findViewById(R.id.testResendOTP);

        //otp from student
        otpGenerator = new OTPgenerator();
        currentOtp = getIntent().getStringExtra("otp_code_student");


        // Set TextWatchers to move focus automatically
        code1.addTextChangedListener(new OTPTextWatcher(code2));
        code2.addTextChangedListener(new OTPTextWatcher(code3));
        code3.addTextChangedListener(new OTPTextWatcher(code4));
        code4.addTextChangedListener(new OTPTextWatcher(code5));
        code5.addTextChangedListener(new OTPTextWatcher(code6));

        // Start the OTP timer
       // startOTPTimer(180000); 3 minute
        startOTPTimer(60000);

        // Resend OTP functionality
        testResendOTP.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                currentOtp = otpGenerator.generateOTP();
                Log.i("new_student_otp", currentOtp);

                isOtpValid = true;
                startOTPTimer(60000);
            }
        });

        //verify button
        buttonVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = getEnteredOTP();

                Log.i("otp_input : ",otp);

                if (!isOtpValid) {
                    Toast.makeText(getApplicationContext(), "OTP has expired. Please resend.", Toast.LENGTH_LONG).show();
                    return;
                }

                //verify student acc
                if(currentOtp.equals(otp)){
                    startActivity(new Intent(getApplicationContext(), StudentHPActivity.class));
                } else {Toast.makeText(getApplicationContext(),"OTP didn't matched.",Toast.LENGTH_LONG).show();}

            }
        });
    }

    //The output is the entered OTP From user
    private String getEnteredOTP() {
        return code1.getText().toString().trim() +
                code2.getText().toString().trim() +
                code3.getText().toString().trim() +
                code4.getText().toString().trim() +
                code5.getText().toString().trim() +
                code6.getText().toString().trim();
    }

    // Inner class to move to next EditText
    private class OTPTextWatcher implements TextWatcher {
        private final EditText nextEditText;

        OTPTextWatcher(EditText nextEditText) {
            this.nextEditText = nextEditText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().trim().isEmpty()) {
                nextEditText.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    // Timer method for OTP
    private void startOTPTimer(long durationInMillis) {
        testResendOTP.setEnabled(false); // Disable Resend OTP button initially

        new CountDownTimer(durationInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                String time = String.format(Locale.getDefault(), "%02d:%02d",
                        millisUntilFinished / 60000, (millisUntilFinished % 60000) / 1000);
                textTimer.setText(time);
            }

            public void onFinish() {
                textTimer.setText("00:00");
                testResendOTP.setEnabled(true);
                isOtpValid = false; //  OTP is no longer valid
            }
        }.start();
    }



}
