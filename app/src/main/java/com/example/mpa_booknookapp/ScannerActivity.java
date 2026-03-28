package com.example.mpa_booknookapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class ScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private DecoratedBarcodeView barcodeView;
    private String scanMode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);

        //take the modeand nook id from NookDetailsActivity
        scanMode = getIntent().getStringExtra("MODE");
        String currentNookId = getIntent().getStringExtra("NOOK_ID");

        barcodeView = findViewById(R.id.scannerFrame);

        findViewById(R.id.btnBackScanner).setOnClickListener(v -> finish());

        //manual entry button
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> {
            Intent intent = new Intent(ScannerActivity.this, EnterIsbnActivity.class);
            intent.putExtra("MODE", scanMode); //still pass details along
            intent.putExtra("NOOK_ID", currentNookId);
            startActivity(intent);
            finish();
        });

        //scanner logic
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    barcodeView.pause(); //stop scanning immediately

                    String scannedISBN = result.getText();

                    //send to confirm screen
                    Intent intent = new Intent(ScannerActivity.this, ConfirmBookActivity.class);
                    intent.putExtra("ISBN", scannedISBN);
                    intent.putExtra("MODE", scanMode);
                    intent.putExtra("NOOK_ID", currentNookId);
                    startActivity(intent);
                    finish();
                }
            }
        });

        //ask for camera permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                barcodeView.resume();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
            }
        }
    }
}