package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        //padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //top nav/back button
        findViewById(R.id.toolbarSettings).setOnClickListener(v -> finish());

        //logout logic dialog box
        Button logoutBtn = findViewById(R.id.btnLogout);
        logoutBtn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Logout?")
                    .setMessage("Are you sure you want to log out?")
                    .setNeutralButton("Cancel", (dialog, which) -> {
                        //closes the dialog
                    })
                    .setPositiveButton("Logout", (dialog, which) -> {
                        //clears app history. returns to login screen
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .show();
        });
    }
}