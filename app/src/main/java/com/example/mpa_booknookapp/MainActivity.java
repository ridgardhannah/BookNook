package com.example.mpa_booknookapp;

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

         //default screen (map)
        //if app is just opened for first time then load the map fragment
        //check so we dont load new map just because user rotates phone. only happens when openeg app
        if (savedInstanceState == null) {
            //load map fragment
            getSupportFragmentManager().beginTransaction()
                    //find the fragment container in the layout and replace it with the map fragment
                    .replace(R.id.fragment_container, new MapFragment())
                    //do it now
                    .commit();
        }

        //set up listener to switch to each fragment when user taps its icon
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null; //start null, ready for one to be selected
            int id = item.getItemId();  //get the id of the item that was tspped

            //work out which fragment to prep
            if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();     //prepare the fragment with the id that matches
            } else if (id == R.id.nav_wishlist) {
                selectedFragment = new WishlistFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            //now have a selected fragment so replace the fragment container with it
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}