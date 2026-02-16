package com.example.mpa_booknookapp;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class AddNookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_nook);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //toolbar back button
        findViewById(R.id.toolbarBackBtn).setOnClickListener(v -> finish());

        //submit nook button
        Button btnSubmitNook = findViewById(R.id.btnSubmitNook);

        //submit button clicky bit
        btnSubmitNook.setOnClickListener(v -> {
            // Show the Snackbar on the current screen
            Snackbar.make(findViewById(android.R.id.content),
                    "Nook submitted! Waiting for review.",
                    Snackbar.LENGTH_LONG).show();

            // WAIT before finishing, so the user sees the message
            new Handler().postDelayed(() -> {
                finish();
            }, 2000);
        });

    }
}