package com.example.smartessay.Archive;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FragmentArchive_Teacher extends Fragment {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList;
    private DatabaseReference classroomsRef;
    private String teacherEmail;

    public FragmentArchive_Teacher() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive__teacher, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(requireContext(), roomList);
        recyclerView.setAdapter(roomAdapter);

        // ✅ Get the teacher email from arguments
        if (getArguments() != null) {
            teacherEmail = getArguments().getString("teacherEmail");
            Log.d("ArchiveDebug", "Teacher email received: " + teacherEmail);
        } else {
            Log.d("ArchiveDebug", "No teacherEmail argument received!");
        }

        classroomsRef = FirebaseDatabase.getInstance().getReference("classrooms");
        loadArchivedRooms();



        return view;
    }

    private void loadArchivedRooms() {
        classroomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    // 🔍 Raw snapshot for debugging
                    Log.d("ArchiveDebug", "Raw data: " + data.getValue());

                    Room room = data.getValue(Room.class);

                    if (room != null) {
                        Log.d("ArchiveDebug", "Room found: "
                                + room.getClassroom_name() + " | status=" + room.getStatus()
                                + " | owner=" + room.getClassroom_owner());
                    } else {
                        Log.d("ArchiveDebug", "⚠️ Room object is null for key: " + data.getKey());
                    }

                    // ✅ Safe and case-insensitive comparison
                    if (room != null
                            && "archived".equalsIgnoreCase(room.getStatus())
                            && teacherEmail != null
                            && teacherEmail.equalsIgnoreCase(room.getClassroom_owner())) {

                        roomList.add(room);
                    }
                }

                roomAdapter.notifyDataSetChanged();
                Log.d("ArchiveDebug", "✅ Total archive rooms loaded: " + roomList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Failed to load archives: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
