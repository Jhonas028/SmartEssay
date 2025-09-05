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

// This activity represents the Teacher Homepage with a navigation drawer and fragments
public class FragmentHP_Teacher extends AppCompatActivity {

    private DrawerLayout drawerLayout; // Drawer layout for side navigation
    private NavigationView navView;    // The navigation menu inside the drawer
    private Toolbar toolbar;           // Top toolbar
    private FloatingActionButton fab;  // Button to open Camera fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this); // Enable modern edge-to-edge UI (status bar overlaps layout nicely)
        setContentView(R.layout.fragment_hp_activity); // Set layout for this activity

        fab = findViewById(R.id.fab); // Link FloatingActionButton from XML

        // Setup toolbar as the ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Link DrawerLayout and NavigationView from XML
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Get the teacher email passed from MainActivity via Intent
        String teacherEmail = getIntent().getStringExtra("teacherEmail");
        if (teacherEmail != null) {
            // Just show a quick toast with teacher email
            Toast.makeText(this, "Teacher: " + teacherEmail, Toast.LENGTH_SHORT).show();
        }

        // Configure hamburger menu toggle for DrawerLayout
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle); // Attach toggle to drawer
        toggle.syncState(); // Synchronize the hamburger icon with drawer state

        // Create the HomePage_Teacher fragment and pass the teacher email
        HomePage_Teacher homeFragment = new HomePage_Teacher();
        Bundle bundle = new Bundle();
        bundle.putString("teacherEmail", teacherEmail); // Pass data to fragment
        homeFragment.setArguments(bundle);

        // Load the initial fragment (homepage) into the FrameLayout container
        loadFragments(homeFragment);

        // Handle navigation drawer item clicks
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null; // Placeholder for selected fragment
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                // If Home clicked, load HomePage_Teacher fragment
                selectedFragment = new HomePage_Teacher();
            } else if (itemId == R.id.logout) {
                // If Logout clicked, show a toast (can later implement real logout)
                Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show();
            }

            // Replace fragment if a selection is made
            if (selectedFragment != null) {
                loadFragments(selectedFragment); // Swap current fragment with selected
                drawerLayout.closeDrawers(); // Close drawer after selection
                return true;
            }
            return false;
        });

        // Handle Floating Action Button click
        fab.setOnClickListener(v -> {
            // Create CameraFragment_Teacher
            Fragment cameraFragment = new CameraFragment_Teacher();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelays, cameraFragment);  // Replace container with camera fragment
            transaction.addToBackStack(null);  // Add transaction to back stack for back navigation
            transaction.commit(); // Commit the fragment transaction
        });
    }

    // Helper method to load any fragment into the FrameLayout container
    private void loadFragments(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays, fragment) // Replace content in FrameLayout with new fragment
                .commit(); // Commit changes
    }
}
