package com.example.mpa_booknookapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.snackbar.Snackbar;

public class ReportIssueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        Toolbar toolbar = findViewById(R.id.toolbarReport);
        toolbar.setNavigationOnClickListener(v -> finish());

        Button submitBtn = findViewById(R.id.btnSubmitReport);
        submitBtn.setOnClickListener(v -> {
            //show report issue success snackbar
            Snackbar.make(findViewById(android.R.id.content), "Issue reported successfully", Snackbar.LENGTH_LONG).show();

            //wait 2 seconds and go back
            new android.os.Handler().postDelayed(() -> finish(), 2000);
        });
    }
}