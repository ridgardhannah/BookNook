package com.example.mpa_booknookapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;


public class ConfirmBookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_book);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //confirm button -> finish flow (later will set to save to firestore)
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            //logic for now. go back to map screen
            Intent intent = new Intent(ConfirmBookActivity.this, WishlistFragment.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //prevent swiping back a page
            startActivity(intent);
        });

        //scan again button -> go back to scanner/manual isbn screen
        findViewById(R.id.btnScanAgain).setOnClickListener(v -> finish());
    }
}