package com.example.smartessay.CreatingAccounts;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.google.android.material.textfield.TextInputLayout;

public class CreateStudentAcc extends AppCompatActivity {

    Button signupBTN;
    EditText emailET,fnameET,lnameET,snumET,passET,conpassET;
    TextInputLayout emailTV,fnameTV,lnameTV,snumTV,passTV,conpassTV;
    String email,fname,lname,stuNum,pass,confPass;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_student_acc);

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

        //TextView
        emailTV = findViewById(R.id.emailTV);
        snumTV = findViewById(R.id.snumTV);
        fnameTV = findViewById(R.id.fnameTV);
        lnameTV = findViewById(R.id.lnameTV);
        passTV = findViewById(R.id.passTV);
        conpassTV = findViewById(R.id.conpassTV);

        email = emailET.getText().toString().trim();
        fname = fnameET.getText().toString().trim();
        lname = lnameET.getText().toString().trim();
        stuNum = snumET.getText().toString().trim();
        pass = passET.getText().toString().trim();
        confPass = conpassET.getText().toString().trim();

        signupBTN.setOnClickListener(v -> {
            //validation for email
            validateInputs();
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

        isValid &= setError(emailTV, email.isEmpty() || !email.matches("^[a-z]+\\.\\d{6}@sanapblo\\.sti\\.edu\\.ph$"), "Enter a valid STI San Pablo email.");
        isValid &= setError(fnameTV, fname.isEmpty(), "First name is required.");
        isValid &= setError(lnameTV, lname.isEmpty(), "Last name is required.");
        isValid &= setError(snumTV, stuNum.isEmpty() || !stuNum.matches("^\\d{10}$"), "Must be exactly 10 digits.");
        isValid &= setError(passTV, pass.isEmpty() || pass.length() < 6, "Password must be at least 6 characters.");
        isValid &= setError(conpassTV, !confPass.equals(pass), "Passwords do not match.");

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




}