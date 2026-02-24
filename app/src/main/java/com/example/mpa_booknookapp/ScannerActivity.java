package com.example.mpa_booknookapp;

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

        //mock success for now till real logic is in. clicking the white box "scans" the book
        findViewById(R.id.scannerFrame).setOnClickListener(v -> {
            Toast.makeText(this, "Barcode recognised!", Toast.LENGTH_SHORT).show();

            //go back to Details screen to show the scannings done
            finish();
        });
    }
}