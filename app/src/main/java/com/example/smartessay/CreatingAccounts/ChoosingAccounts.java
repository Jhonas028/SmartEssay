package com.example.smartessay.CreatingAccounts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartessay.R;

public class ChoosingAccounts extends AppCompatActivity {

    LinearLayout stuBTN,educBTN;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choosing_accounts);

        stuBTN = findViewById(R.id.stuBTN);
        educBTN = findViewById(R.id.educBTN);

        //proceed to student activity
        stuBTN.setOnClickListener(v -> {
                Intent intent = new Intent(ChoosingAccounts.this, CreateStudentAcc.class);
                intent.putExtra("account", "student");
                startActivity(intent);
        }

        );

        //proceed to teacher activity
        educBTN.setOnClickListener(v -> {
            Intent intent = new Intent(ChoosingAccounts.this, CreateTeacherAcc.class);
            intent.putExtra("account", "educator");
            startActivity(intent);
        });

    }
}
