package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);

        findViewById(R.id.btnBackScanner).setOnClickListener(v -> finish());

        //logic for manual entry button.opens manual screen
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> {
            Intent intent = new Intent(ScannerActivity.this, EnterIsbnActivity.class);
            startActivity(intent);
        });

        //mock success for now till real logic is in. clicking the white box "scans" the book
        findViewById(R.id.scannerFrame).setOnClickListener(v -> {
            Toast.makeText(this, "Barcode recognised!", Toast.LENGTH_SHORT).show();

            //go back to details screen to show the scannings done
            finish();


        });
    }
}