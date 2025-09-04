package com.example.smartessay.TeacherHomepage;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartessay.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class FragmentHP_Teacher extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_hp_activity);

        fab = findViewById(R.id.fab);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer & NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Retrieve email
        String teacherEmail = getIntent().getStringExtra("teacherEmail");
        if (teacherEmail != null) {
            Toast.makeText(this, "Teacher: " + teacherEmail, Toast.LENGTH_SHORT).show();
        }

        // Burger toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        HomePage_Teacher homeFragment = new HomePage_Teacher();
        Bundle bundle = new Bundle();
        bundle.putString("teacherEmail", teacherEmail);
        homeFragment.setArguments(bundle);

// Load fragment
        loadFragments(homeFragment);

        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                selectedFragment = new HomePage_Teacher();
            } else if (itemId == R.id.logout) {
                Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show();
            }

            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers(); // close burger after selection
                return true;
            }
            return false;
        });

        fab.setOnClickListener(v -> {
            Fragment cameraFragment = new CameraFragment_Teacher();
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
