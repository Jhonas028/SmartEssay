package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smartessay.Fragments.CameraFragment;
import com.example.smartessay.Fragments.CheckedFragment;
import com.example.smartessay.Fragments.HomeFragment;
import com.example.smartessay.Fragments.RoomFragment;
import com.example.smartessay.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherHPActivity extends AppCompatActivity {

    BottomNavigationView botnav;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_hpactivity);

        botnav = findViewById(R.id.botnav);

        loadFragments(new HomeFragment());

        botnav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home) {selectedFragment = new HomeFragment();
            } else if (itemId == R.id.room) { selectedFragment = new RoomFragment();
            } else if (itemId == R.id.camera) {
                selectedFragment = new CameraFragment();
            }
            else if (itemId == R.id.checked) {selectedFragment = new CheckedFragment();}

            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                return true;}

            return false;

        });

    }

    public void loadFragments(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays,fragment)
                .commit();
    }

}