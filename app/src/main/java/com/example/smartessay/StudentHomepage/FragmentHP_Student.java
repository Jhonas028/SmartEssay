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

public class FragmentHP_Student extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_hpactivity);


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

        String studentEmail = getIntent().getStringExtra("studentEmail");

        HomePage_Student homeFragment = new HomePage_Student();
        Bundle bundle = new Bundle();
        bundle.putString("studentEmail", studentEmail);
        homeFragment.setArguments(bundle);

        loadFragments(homeFragment);

        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.homeStudent) {
                selectedFragment = new HomePage_Student();
            } else if (itemId == R.id.logoutStudent) {
                Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
            }


            if (selectedFragment != null) {
                loadFragments(selectedFragment);
                drawerLayout.closeDrawers(); // close burger after selection
                return true;
            }

            return false;
        });

    }

    private void loadFragments(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelays, fragment)
                .commit();
    }
}
