package com.example.smartessay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartessay.CreatingAccounts.ChoosingAccounts;
import com.example.smartessay.TeacherHomepage.TeacherHPActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    TextView signupTV;
    Button signinBTN;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        signupTV = findViewById(R.id.signupTV);
        signinBTN = findViewById(R.id.signinBTN);

        signupTV.setOnClickListener(view->{
            startActivity(new Intent(getApplicationContext(), ChoosingAccounts.class));
        });

        //override
        signinBTN.setOnClickListener(view->{startActivity(new Intent(getApplicationContext(), TeacherHPActivity.class));});

    }
}