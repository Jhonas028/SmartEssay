package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartessay.R;
import com.example.smartessay.Teacher_Fragments.CameraFragment;
import com.example.smartessay.Teacher_Fragments.CheckedFragment;
import com.example.smartessay.Teacher_Fragments.HomeFragment;
import com.example.smartessay.Teacher_Fragments.RoomFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class TeacherHPActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_hpactivity);

        fab = findViewById(R.id.fab);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer & NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Burger toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Default fragment
        loadFragments(new HomeFragment());

        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.logout) {
                selectedFragment = new CheckedFragment();
            }

            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers(); // close burger after selection
                return true;
            }

            return false;
        });

        fab.setOnClickListener(v -> {
            Fragment cameraFragment = new CameraFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelays, cameraFragment);  // framelays = your FrameLayout container
            transaction.addToBackStack(null);  // allows back navigation
            transaction.commit();
        });
    }

    private void loadFragments(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays, fragment)
                .commit();
    }
}
