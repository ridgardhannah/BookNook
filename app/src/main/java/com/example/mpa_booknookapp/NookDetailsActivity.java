package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class NookDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nook_details);

        //sort the padding for the top/bottom bars and edge-to-edge
        //waits for system to change size. listens for it
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //find the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        //logic to handle toolbar back button
        //tells the little arrow at the top to just finish theis screen and go back to the map when tapped
        toolbar.setNavigationOnClickListener(v -> finish());

        //donate button opens the scanner
        Button btnDonate = findViewById(R.id.btnDonate);
        btnDonate.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            // Note: Later we will add: intent.putExtra("MODE", "DONATE");
            startActivity(intent);
        });

        // 2.find button opens the scanner
        Button btnFind = findViewById(R.id.btnFind);
        btnFind.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            // Note: Later we will add: intent.putExtra("MODE", "FIND");
            startActivity(intent);
        });

        //directions button vurrently just placeholder
        Button btnDirections = findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(v -> {
            // Logic to open Google Maps will go here later
        });

        //report button opens report screen
        findViewById(R.id.btnReport).setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ReportIssueActivity.class);
            startActivity(intent);
        });
    }//end of oncreate method
}