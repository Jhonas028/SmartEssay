package com.example.smartessay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartessay.CreatingAccounts.ChoosingAccounts;

public class MainActivity extends AppCompatActivity {

    TextView signupTV;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        signupTV = findViewById(R.id.signupTV);

        signupTV.setOnClickListener(view->{startActivity(new Intent(getApplicationContext(), ChoosingAccounts.class));});

    }
}