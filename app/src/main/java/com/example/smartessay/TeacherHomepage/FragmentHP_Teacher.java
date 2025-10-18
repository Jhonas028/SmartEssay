package com.example.smartessay.TeacherHomepage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartessay.MainActivity;
import com.example.smartessay.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Teacher Homepage Activity
 * - Contains navigation drawer
 * - Loads teacher fragments (Home, Camera, etc.)
 * - Displays teacher profile info in drawer header
 */
public class FragmentHP_Teacher extends AppCompatActivity {

    // UI components
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    TextView profileName;
    TextView profileSubtitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable full edge-to-edge layout (status bar overlap handling)
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_hp_activity);

        // 🔗 Link FloatingActionButton
        fab = findViewById(R.id.fab);

        // 🔗 Setup Toolbar as ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 🔗 Link DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // ==========================
        // 🔹 NAV HEADER (Profile Info)
        // ==========================
        android.view.View headerView = navView.getHeaderView(0);
        profileName = headerView.findViewById(R.id.profile_name);
        profileSubtitle = headerView.findViewById(R.id.profile_subtitle);
        String teacherEmail = getIntent().getStringExtra("teacherEmail");        // Get teacher email from login intent
        profileSubtitle.setText(teacherEmail);

        //call method to display teacher details
        displayTeacherDetails(teacherEmail, profileName);

        // Debugging toast (shows email)
        if (teacherEmail != null) {
            Toast.makeText(this, "Teacher: " + teacherEmail, Toast.LENGTH_SHORT).show();
        }

        //call this method to setup drawer toggle
        setupDrawerToggle();

        // ==========================
        // 🔹 INITIAL FRAGMENT (Homepage)
        // ==========================
        HomePage_Teacher homeFragment = new HomePage_Teacher();
        Bundle bundle = new Bundle();
        bundle.putString("teacherEmail", teacherEmail); // Pass email to fragment
        homeFragment.setArguments(bundle);

        loadFragments(homeFragment); // Load homepage on startup

        // ==========================
        // 🔹 NAVIGATION ITEM SELECTION
        // ==========================
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                //the code will be executed when the home button is clicked, it displays the and refresh the room details
                selectedFragment = new HomePage_Teacher();
                Bundle b = new Bundle();
                b.putString("teacherEmail", teacherEmail);
                selectedFragment.setArguments(bundle);
            } else if (itemId == R.id.logout) {
                // ✅ Clear saved session
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                prefs.edit().clear().apply();

                // ✅ Redirect to login
                Intent intent = new Intent(FragmentHP_Teacher.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

                // ✅ Finish current activity
                finish();

                return true;
            }

            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        //call camera method
        cameraFab();
    }

    private void setupDrawerToggle() {
        // ==========================
        // 🔹 DRAWER TOGGLE (Hamburger menu)
        // ==========================
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }


    private void displayTeacherDetails(String teacherEmail, TextView profileName){
        // Fetch teacher full name from Firebase
        DatabaseReference teachersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child("teachers");

        teachersRef.orderByChild("email").equalTo(teacherEmail)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (com.google.firebase.database.DataSnapshot teacher : snapshot.getChildren()) {
                                String firstName = teacher.child("first_name").getValue(String.class);
                                String lastName = teacher.child("last_name").getValue(String.class);

                                if (firstName != null && lastName != null) {
                                    // 👇 Display full name in nav header
                                    profileName.setText(firstName + " " + lastName);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Log.e("FirebaseError", "Error: " + error.getMessage());
                    }
                });
    }

    private void cameraFab(){
        // ==========================
        // 🔹 FLOATING ACTION BUTTON (Open Camera Fragment)
        // ==========================

        fab.setOnClickListener(v -> {
            Fragment cameraFragment = new CameraFragment_Teacher();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelays, cameraFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    // ==========================
    // 🔹 HELPER: Load any fragment
    // ==========================
    private void loadFragments(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays, fragment)
                .commit();
    }
}
