package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        //tell nav bar what to do when a button is clicked
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (id == R.id.nav_wishlist) {
                selectedFragment = new WishlistFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();

                if (getIntent().getBooleanExtra("OPEN_ACHIEVEMENTS", false)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("SHOW_ACHIEVEMENTS", true);
                    selectedFragment.setArguments(bundle); //stuffs the note into the fragment
                    getIntent().removeExtra("OPEN_ACHIEVEMENTS"); //deletes original note
                }
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        //check which intent was used to get here
        if (getIntent().getBooleanExtra("GO_TO_PROFILE", false)) {
            //user came from achievement dialogueAchievement Dialog. nav bar selectts profile
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if (savedInstanceState == null) {
            //opened app normaly so force the navbar to select map
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    //runs if main activity is already open in the background and gets a new intent. use with achievement dialog
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent); //updates the activities memory with new intent.

        if (intent.getBooleanExtra("GO_TO_PROFILE", false)) {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

}