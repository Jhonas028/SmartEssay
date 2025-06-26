package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.smartessay.API.EmailAPI;
import com.example.smartessay.API.MailGunEmail;
import com.example.smartessay.MainActivity;
import com.example.smartessay.R;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;


public class CreateStudentAcc extends AppCompatActivity {

    Button signupBTN;
    EditText emailET,fnameET,lnameET,snumET,passET,conpassET;
    TextInputLayout emailTV,fnameTV,lnameTV,snumTV,passTV,conpassTV;
    String email,fname,lname,stuNum,pass,confPass;
    TextView logInTV;
    OTPverification otp;

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

            //validation for email
            if(validateInputs()){

                //generate OTP
                String myOTP = otp.generateOTP();
                Log.i("myOTP",myOTP);
                //user email
                email = emailET.getText().toString().trim();

                //call EmailAPI from API folder
                try {
                    EmailAPI.sendOtpEmail(myOTP, email);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                Toast.makeText(getApplicationContext(),"Sign-in",Toast.LENGTH_SHORT).show();
            } else {
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
        isValid &= setError(passTV, pass.isEmpty() || !isValidPassword(pass), "Please input valid password.");

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
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,15}$");
    }





}