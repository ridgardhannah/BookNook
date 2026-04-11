package com.example.mpa_booknookapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPrefs;
    private static final String PREFS_NAME = "BookNookSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        //general ui setup
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(view1 -> finish());

        //logout and delete buttons
        Button logoutButton = findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(view2 -> showLogoutConfirmation());
        Button btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
        findViewById(R.id.btnResetStats).setOnClickListener(v -> resetAllStatsToZero());

        //theme and dark mode logic
        AutoCompleteTextView themeDropdown = findViewById(R.id.autoCompleteTheme);
        String[] themes = {"System Default", "Light", "Dark"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, themes);
        themeDropdown.setAdapter(themeAdapter);

        //tells the dropdown to show all 3 items each time its clicked
        themeDropdown.setThreshold(100);

        //to show full list when clicked on
        themeDropdown.setOnClickListener(v -> themeDropdown.showDropDown());

        //load saved theme
        int savedThemeIndex = sharedPrefs.getInt("app_theme", 0);
        themeDropdown.setText(themes[savedThemeIndex], false);

        themeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            sharedPrefs.edit().putInt("app_theme", position).apply();

            if (position == 0) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (position == 1) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (position == 2) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

            //force the adapter to reset
            themeDropdown.setAdapter(themeAdapter);
        });

        //button to replay tutorial
        Button btnReplayTutorial = findViewById(R.id.btnReplayTutorial);

        btnReplayTutorial.setOnClickListener(v -> {
            //send them back to the map screen
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);

            //attach the command to replay the tutorial
            intent.putExtra("REPLAY_TUTORIAL", true);

            startActivity(intent);

            //close settings screen
            finish();
        });


        //font logic
        Slider fontSlider = findViewById(R.id.sliderFontSize);

        //load saved font scale (50 for normal size)
        float savedFontSliderValue = sharedPrefs.getFloat("font_slider_value", 50.0f);
        fontSlider.setValue(savedFontSliderValue);

        fontSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                //calculate the actual scale multiplier to work out size
                float newScale = 0.8f + ((value / 100.0f) * 0.5f);

                //save slider position and the maths to scale it
                sharedPrefs.edit()
                        .putFloat("font_slider_value", value)
                        .putFloat("font_scale_multiplier", newScale)
                        .apply();
            }
        });

        //button to apply font changes to refresh screen
        Button btnApplyFont = findViewById(R.id.btnApplyFont);
        if(btnApplyFont != null) {
            btnApplyFont.setOnClickListener(fontBtnView -> {
                //reloads the activity so the new font size is shown
                recreate();
            });
        }

        //radius slider logic
        Slider sliderRadius = findViewById(R.id.sliderRadius);
        TextView tvRadiusValue = findViewById(R.id.tvRadiusValue);

        float savedRadius = sharedPrefs.getFloat("notify_radius", 5.0f);
        sliderRadius.setValue(savedRadius);
        tvRadiusValue.setText((int)savedRadius + " km");

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            int radius = (int) value;
            tvRadiusValue.setText(radius + " km");
            sharedPrefs.edit().putFloat("notify_radius", value).apply();
        });

        //wishlist switch logic
        MaterialSwitch switchWishlist = findViewById(R.id.switchWishlist);

        boolean savedWishlist = sharedPrefs.getBoolean("wishlist_alerts", true);
        switchWishlist.setChecked(savedWishlist);

        switchWishlist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("wishlist_alerts", isChecked).apply();
            String status = isChecked ? "Alerts Enabled" : "Alerts Muted";
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        });

        //window padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (mainView, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mainView.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        //get font scale
        SharedPreferences prefs = newBase.getSharedPreferences("BookNookSettings", Context.MODE_PRIVATE);
        float fontScale = prefs.getFloat("font_scale_multiplier", 1.00f);

        //apply to the context
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = fontScale;
        Context newContext = newBase.createConfigurationContext(config);

        super.attachBaseContext(newContext);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of BookNook?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    //when user presses delete account
    private void showDeleteConfirmationDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account?")
                .setMessage("Are you sure you want to permanently delete your BookNook account? All your saved preferences and data will be lost. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            //delete user data from firestore first
            db.collection("users").document(userId).delete().addOnCompleteListener(task -> {

                //delete their login info from firebase Auth
                user.delete().addOnCompleteListener(authTask -> {

                    if (authTask.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Account fully deleted.", Toast.LENGTH_SHORT).show();
                    } else {
                        //get error message from firebase to debug :(
                        String errorMsg = authTask.getException() != null ? authTask.getException().getMessage() : "Unknown error";
                        Log.e("DeleteAccount", "Auth delete failed: " + errorMsg);

                        Toast.makeText(SettingsActivity.this, "Auth error: " + errorMsg + " (You may need to log out and log back in first)", Toast.LENGTH_LONG).show();
                    }

                    //safety net. force log out and send to login screen regardless of successful delete or not.
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            });
        }
    }

    private void resetAllStatsToZero() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset All Stats?")
                .setMessage("Are you absolutely sure? This will wipe your stats back to zero and all unequipped titles will be re-locked.")
                .setPositiveButton("Reset", (dialog, which) -> {

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    //put all the 0s in a box
                    Map<String, Object> resets = new HashMap<>();
                    resets.put("donatedCount", 0);
                    resets.put("foundCount", 0);
                    resets.put("nooksCount", 0);
                    resets.put("wishlistAddedCount", 0);
                    resets.put("wishlistFoundCount", 0);

                    //send the box to firebase
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update(resets)
                            .addOnSuccessListener(aVoid -> {
                                android.widget.Toast.makeText(this, "Stats wiped clean!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}