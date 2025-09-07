package com.example.smartessay.StudentHomepage;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.smartessay.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FragmentHP_Student extends AppCompatActivity {

    // Drawer menu components
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;

    // Profile header views
    private TextView profileName, profileSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_hpactivity);

        // 🔹 Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 🔹 Setup DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // 🔹 Setup burger menu (hamburger icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Access header views (Full name + Email in drawer header)
        android.view.View headerView = navView.getHeaderView(0);
        profileName = headerView.findViewById(R.id.profile_name);
        profileSubtitle = headerView.findViewById(R.id.profile_subtitle);

        // 🔹 Get student email passed from MainActivity
        String studentEmail = getIntent().getStringExtra("studentEmail");
        Log.i("StudentHomepage", "Received student email: " + studentEmail);
        // 🔹 Display student details in the drawer header
        displayStudentDetails(studentEmail);

        // 🔹 Load default home fragment
        HomePage_Student homeFragment = new HomePage_Student();
        Bundle bundle = new Bundle();
        bundle.putString("studentEmail", studentEmail);
        homeFragment.setArguments(bundle);
        loadFragments(homeFragment);

        // 🔹 Handle navigation drawer item clicks
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.homeStudent) {
                selectedFragment = new HomePage_Student();
                Bundle b = new Bundle();
                b.putString("studentEmail", studentEmail);
                selectedFragment.setArguments(b);

            } else if (itemId == R.id.logoutStudent) {
                Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
                // TODO: clear SharedPreferences + go back to LoginActivity
            }

            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });
    }

    // 🔹 Display student info from Firebase into nav drawer header
// 🔹 Display student info from Firebase into nav drawer header
    private void displayStudentDetails(String studentEmail) {
        if (studentEmail == null) return;

        DatabaseReference studentsRef = FirebaseDatabase.getInstance()
                .getReference("users").child("students");

        studentsRef.orderByChild("email").equalTo(studentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot student : snapshot.getChildren()) {
                                // 🔹 Match your DB fields (first_name, last_name, email)
                                String firstName = student.child("first_name").getValue(String.class);
                                String lastName = student.child("last_name").getValue(String.class);
                                String email = student.child("email").getValue(String.class);

                                if (firstName != null && lastName != null) {
                                    profileName.setText(firstName + " " + lastName);
                                } else {
                                    profileName.setText("Unknown Student");
                                }

                                profileSubtitle.setText(email != null ? email : "No email");
                            }
                        } else {
                            Log.d("StudentHomepage", "No student data found for email: " + studentEmail);
                            profileName.setText("Unknown Student");
                            profileSubtitle.setText("No email");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("StudentHomepage", "Database error: " + error.getMessage());
                    }
                });
    }


    // 🔹 Load fragments into main container
    private void loadFragments(Fragment fragment) {
        String studentEmail = getIntent().getStringExtra("studentEmail");
        if (studentEmail != null) {
            Bundle bundle = new Bundle();
            bundle.putString("studentEmail", studentEmail);
            fragment.setArguments(bundle);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays, fragment)
                .commit();
    }
}
