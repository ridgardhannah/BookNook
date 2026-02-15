package com.example.mpa_booknookapp;

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

        //sort the padding for the top/bottom bars
        //waits for system to change size. listens for it
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //logic to handle toolbar back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        //tells the little arrow at the top to just finish theis screen and go back to the map when tapped
        toolbar.setNavigationOnClickListener(v -> finish());

        //log book button logic. uses method under this one
        Button logBookBtn = findViewById(R.id.btnLogBook);
        logBookBtn.setOnClickListener(v -> showLogOptions());

    }//end of oncreate method

    //method to show popup for logging a book when log book button is presed
    private void showLogOptions() {
        //set it up
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_log_book_dialog, null);

        //will do logic here for opening scanner pick or donate buttons pressed
        view.findViewById(R.id.btnDonate).setOnClickListener(v -> {
            dialog.dismiss();
            //put logic for getting scanner here
        });

        //logic for coancel button to get rid of this popup
        view.findViewById(R.id.btnCancelLog).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }
}