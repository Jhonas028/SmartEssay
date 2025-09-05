package com.example.smartessay.StudentHomepage;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.smartessay.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

// Main Activity for Student Homepage (with drawer navigation)
public class FragmentHP_Student extends AppCompatActivity {

    // Drawer menu components
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    //private FloatingActionButton fab; // currently not used but can be for quick actions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_hpactivity);

        // 🔹 Setup toolbar at the top
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 🔹 Setup DrawerLayout (the sliding menu) and NavigationView (menu items)
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // 🔹 Setup burger icon (three-line menu) to toggle the drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Get student email passed from login/previous screen
        String studentEmail = getIntent().getStringExtra("studentEmail");

        // 🔹 Load the default "HomePage_Student" fragment and pass studentEmail
        HomePage_Student homeFragment = new HomePage_Student();
        Bundle bundle = new Bundle();
        bundle.putString("studentEmail", studentEmail);
        homeFragment.setArguments(bundle);

        loadFragments(homeFragment);

        // 🔹 Handle clicks on drawer menu items
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // IF user clicks "Home"
            if (itemId == R.id.homeStudent) {
                selectedFragment = new HomePage_Student();
                // optional: pass studentEmail again if needed
                Bundle b = new Bundle();
                b.putString("studentEmail", studentEmail);
                selectedFragment.setArguments(b);

                // IF user clicks "Logout"
            } else if (itemId == R.id.logoutStudent) {
                Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
                // here you could: clear SharedPreferences + go back to LoginActivity
            }

            // 🔹 If a fragment is chosen, replace current fragment with it
            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers(); // close drawer after selection
                return true;
            }

            return false; // if no valid menu clicked
        });
    }

    // 🔹 Utility function to load/replace fragments inside the FrameLayout
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
