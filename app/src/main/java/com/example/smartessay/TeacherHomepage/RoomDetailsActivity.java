package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;

import java.util.ArrayList;
import java.util.List;

public class RoomDetailsActivity extends AppCompatActivity {

    TextView tvRoomName, tvRoomCode;
    RecyclerView rvStudents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_teacher);

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        rvStudents = findViewById(R.id.recycler_view_rooms); // Use RecyclerView ID, not TextView ID

        String roomName = getIntent().getStringExtra("roomName");
        String roomCode = getIntent().getStringExtra("roomCode");

        tvRoomName.setText("Room Name: " + roomName);
        tvRoomCode.setText("Room Code: " + roomCode);

        // Dummy student list
        List<String> dummyStudents = new ArrayList<>();
        dummyStudents.add("Juan Dela Cruz");
        dummyStudents.add("Maria Clara");
        dummyStudents.add("Jose Rizal");
        dummyStudents.add("Andres Bonifacio");
        dummyStudents.add("Emilio Aguinaldo");

        // Setup RecyclerView
        StudentAdapter adapter = new StudentAdapter(dummyStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);
    }

    public static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

        private List<String> students;

        public StudentAdapter(List<String> students) {
            this.students = students;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.teacher_essay_item_layout, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            holder.tvStudentName.setText(students.get(position));
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        static class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.text_student_name);
            }
        }
    }
}
