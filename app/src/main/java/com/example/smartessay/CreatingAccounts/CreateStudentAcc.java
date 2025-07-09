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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class CreateStudentAcc extends AppCompatActivity {

    Button signupBTN;
    EditText emailET,fnameET,lnameET,snumET,passET,conpassET;
    TextInputLayout emailTV,fnameTV,lnameTV,snumTV,passTV,conpassTV;
    String email,fname,lname,stuNum,pass,confPass;
    TextView logInTV;
    OTPgenerator otp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_student_acc);

        //type of accouint, this time its student account
        String account = getIntent().getStringExtra("account");
        Log.i("myTag","account name: " + account);

        signupBTN = findViewById(R.id.signupBTN);
        //Edit Text
        emailET = findViewById(R.id.emailET);
        fnameET = findViewById(R.id.fnameET);
        lnameET = findViewById(R.id.lnameET);
        snumET = findViewById(R.id.snumET);
        passET = findViewById(R.id.passET);
        conpassET = findViewById(R.id.conpassET);


        //TextInputLayout
        emailTV = findViewById(R.id.emailTV);
        snumTV = findViewById(R.id.snumTV);
        fnameTV = findViewById(R.id.fnameTV);
        lnameTV = findViewById(R.id.lnameTV);
        passTV = findViewById(R.id.passTV);
        conpassTV = findViewById(R.id.conpassTV);

        //TextView
        logInTV = findViewById(R.id.logInTV);

        clearHelperTextOnFocus(emailET, emailTV);
        clearHelperTextOnFocus(fnameET, fnameTV);
        clearHelperTextOnFocus(lnameET, lnameTV);
        clearHelperTextOnFocus(snumET, snumTV);
        clearHelperTextOnFocus(passET, passTV);
        clearHelperTextOnFocus(conpassET, conpassTV);

        //Going back to MainActivity when user already have an account
        logInTV.setOnClickListener(view ->{startActivity(new Intent(CreateStudentAcc.this, MainActivity.class));finish();});

        signupBTN.setOnClickListener(v -> {

            if(true){
                otp = new OTPgenerator(); // Make sure it's initialized
                String myOTP = otp.generateOTP();
                String email = emailET.getText().toString().trim();
                String fullname = fnameET.getText().toString().trim() + " " + lnameET.getText().toString().trim();
                String stuNum = snumET.getText().toString().trim();
                String pass = passET.getText().toString().trim();

                // 🔐 Save to pending_verification
                DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                        .getReference("pending_verification")
                        .child(stuNum);

                HashMap<String, Object> pendingData = new HashMap<>();
                pendingData.put("email", email);
                pendingData.put("fullname", fullname);
                pendingData.put("password", pass); // Optional: hash this
                pendingData.put("otp", myOTP);
                pendingData.put("timestamp", System.currentTimeMillis());

                pendingRef.setValue(pendingData)
                        .addOnSuccessListener(aVoid -> {
                            // 🔁 Proceed to OTP page after saving
                            Intent intent = new Intent(CreateStudentAcc.this, OTPverifyStudent.class);
                            intent.putExtra("account", account);
                            intent.putExtra("email_student", email);
                            intent.putExtra("otp_code_student", myOTP);
                            intent.putExtra("fullname", fullname);
                            intent.putExtra("studentNumber", stuNum);
                            intent.putExtra("password", pass);
                            startActivity(intent);
                            Toast.makeText(getApplicationContext(), "OTP sent. Please verify.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getApplicationContext(), "Failed to start verification.", Toast.LENGTH_SHORT).show();
                            Log.e("FirebaseError", e.getMessage());
                        });
            }
            else {
                Toast.makeText(getApplicationContext(),"Sign-in failed",Toast.LENGTH_SHORT).show();
            }


        });


    }

    //validations
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

    //when fields are empty or other validation this method will appear
    private boolean setError(TextInputLayout layout, boolean condition, String message) {
        if (condition) {
            layout.setHelperText(message);
            return false;
        } else {
            layout.setHelperText(null);
            return true;
        }
    }

    //when user click the field setHelperText is clear
    private void clearHelperTextOnFocus(EditText editText, TextInputLayout layout) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                layout.setHelperText(null);
            }
        });
    }
    public boolean isValidPassword(String password){
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&.*])[A-Za-z\\d!@#$%^&.*]{8,15}$");

    }





}