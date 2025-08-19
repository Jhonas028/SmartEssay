package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartessay.R;
import com.example.smartessay.Teacher_Fragments.HomeFragment_Teacher;

public class RoomDetailsActivity extends AppCompatActivity {

    TextView tvRoomName, tvRoomCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);

        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");

        tvRoomName.setText("Room Name: " + roomName);
        tvRoomCode.setText("Room Code: " + roomCode);




    }
}

