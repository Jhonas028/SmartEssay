package com.example.smartessay.CreatingAccounts;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartessay.R;

public class CreateStudentAcc extends AppCompatActivity {

    Button signupBTN;
    EditText emailTV,fnameTV,lnameTV,snumTV,passTV,conpassTV;
    String email,fname,lname,stuNum,pass,confPass;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_student_acc);

        String account = getIntent().getStringExtra("account");
        Log.i("myTag","account name: " + account);

        signupBTN = findViewById(R.id.signupBTN);
        emailTV = findViewById(R.id.emailTV);
        fnameTV = findViewById(R.id.fnameTV);
        lnameTV = findViewById(R.id.lnameTV);
        snumTV = findViewById(R.id.snumTV);
        passTV = findViewById(R.id.passTV);
        conpassTV = findViewById(R.id.conpassTV);

        email = emailTV.getText().toString().trim();
        fname = fnameTV.getText().toString().trim();
        lname = lnameTV.getText().toString().trim();
        stuNum = snumTV.getText().toString().trim();
        pass = passTV.getText().toString().trim();
        confPass = conpassTV.getText().toString().trim();

        signupBTN.setOnClickListener(v -> {

        });

    }
}