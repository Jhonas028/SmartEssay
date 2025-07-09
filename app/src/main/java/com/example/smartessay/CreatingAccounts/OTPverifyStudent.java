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

import com.example.smartessay.API.EmailAPI;
import com.example.smartessay.R;
import com.example.smartessay.StudentHomepage.StudentHPActivity;

import org.json.JSONException;

import java.util.Locale;

public class OTPverifyStudent extends AppCompatActivity {

    // Input fields for 6-digit OTP
    EditText code1, code2, code3, code4, code5, code6;

    // UI elements for timer and resend functionality
    TextView textTimer, testResendOTP;
    Button buttonVerify;

    // OTP generator class
    OTPgenerator otpGenerator;

    // Boolean flag to track if the OTP is still valid
    private boolean isOtpValid = true;

    // The OTP that was originally generated or resent
    private String currentOtp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enables full-screen layout
        setContentView(R.layout.activity_otpverification);

        // Find all views by their IDs
        code1 = findViewById(R.id.inputCode1);
        code2 = findViewById(R.id.inputCode2);
        code3 = findViewById(R.id.inputCode3);
        code4 = findViewById(R.id.inputCode4);
        code5 = findViewById(R.id.inputCode5);
        code6 = findViewById(R.id.inputCode6);
        buttonVerify = findViewById(R.id.buttonVerify);
        textTimer = findViewById(R.id.textTimer);
        testResendOTP = findViewById(R.id.testResendOTP);

        // Initialize the OTP generator and get the OTP from intent

        otpGenerator = new OTPgenerator();

        //these are the informations need to store in database
        String account = getIntent().getStringExtra("account");
        String email = getIntent().getStringExtra("email_student");
        currentOtp = getIntent().getStringExtra("otp_code_student");
        String fullname = getIntent().getStringExtra("fullname");
        String studentNumber = getIntent().getStringExtra("studentNumber");
        String password = getIntent().getStringExtra("password");

        // Add TextWatchers to handle input navigation and backspacing
        code1.addTextChangedListener(new OTPTextWatcher(code1, code2, null));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3, code1));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4, code2));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5, code3));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6, code4));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null, code5));

        // Start the OTP countdown timer (e.g. 60 seconds)
        startOTPTimer(60000);

        // Resend OTP button logic
        testResendOTP.setOnClickListener(v -> {

            // Clear the input fields
            clearInputs();

            currentOtp = otpGenerator.generateOTP(); // Generate a new OTP
            Log.i("new_student_otp", currentOtp);    // Log it for debugging
            isOtpValid = true;  // Reset validity

            /* ***** Just comment this to save API usage *****
            //call EmailAPI from API folder, this time the OTP already emailed the user
            try {
                EmailAPI.sendOtpEmail(currentOtp, email);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }*/

            startOTPTimer(60000);                     // Restart timer
        });

        // Verification button logic
        buttonVerify.setOnClickListener(v -> {
            String otp = getEnteredOTP(); // Get user input
            Log.i("otp_input : ", otp);   // Log for debugging

            // Check if the OTP is still valid
            if (!isOtpValid) {
                Toast.makeText(getApplicationContext(), "OTP has expired. Please resend.", Toast.LENGTH_LONG).show();
                return;
            }

            // Compare entered OTP with actual
            if (currentOtp.equals(otp)) {
                startActivity(new Intent(getApplicationContext(), StudentHPActivity.class)); // Success
            } else {
                Toast.makeText(getApplicationContext(), "OTP didn't match.", Toast.LENGTH_LONG).show(); // Fail
            }
        });
    }

    // Combines all EditText fields into a single OTP string
    private String getEnteredOTP() {
        return code1.getText().toString().trim() +
                code2.getText().toString().trim() +
                code3.getText().toString().trim() +
                code4.getText().toString().trim() +
                code5.getText().toString().trim() +
                code6.getText().toString().trim();
    }

    // Clear the input fields
    public void clearInputs(){
        code1.setText("");
        code2.setText("");
        code3.setText("");
        code4.setText("");
        code5.setText("");
        code6.setText("");
        code1.requestFocus();
    }

    /**
     * Handles text input focus shifting and backspace logic for OTP fields.
     * When the user enters a digit, focus moves forward.
     * When user presses backspace on an empty field, focus moves backward.
     */
    private class OTPTextWatcher implements TextWatcher {
        private final EditText currentEditText;
        private final EditText nextEditText;
        private final EditText previousEditText;

        OTPTextWatcher(EditText currentEditText, EditText nextEditText, EditText previousEditText) {
            this.currentEditText = currentEditText;
            this.nextEditText = nextEditText;
            this.previousEditText = previousEditText;

            // Handle backspace to move focus to the previous field
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

        // Automatically move to the next field when user enters a digit
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().trim().isEmpty() && nextEditText != null) {
                nextEditText.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    /**
     * Starts a countdown timer to track OTP expiration.
     * Disables the resend button during countdown, then enables it when time is up.
     */
    private void startOTPTimer(long durationInMillis) {
        testResendOTP.setEnabled(false); // Disable "Resend OTP" button initially

        new CountDownTimer(durationInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                // Format time as mm:ss and display
                String time = String.format(Locale.getDefault(), "%02d:%02d",
                        millisUntilFinished / 60000,
                        (millisUntilFinished % 60000) / 1000);
                textTimer.setText(time);
            }

            public void onFinish() {
                // Timer is done
                textTimer.setText("00:00");
                testResendOTP.setEnabled(true);  // Allow resend
                testResendOTP.setVisibility(View.VISIBLE);
                isOtpValid = false;              // Invalidate current OTP
            }
        }.start();
    }
}
