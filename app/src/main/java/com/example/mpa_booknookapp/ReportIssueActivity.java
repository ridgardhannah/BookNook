package com.example.mpa_booknookapp;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ReportIssueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        Toolbar toolbar = findViewById(R.id.toolbarReport);
        toolbar.setNavigationOnClickListener(v -> finish());

        Button submitBtn = findViewById(R.id.btnSubmitReport);
        submitBtn.setOnClickListener(v -> {
            //changed for change Id 12
            //success dialog box instead of snackbar for 2 seconds
            new MaterialAlertDialogBuilder(ReportIssueActivity.this)
                    .setTitle("Report Submitted")
                    .setMessage("Thank you for flagging this issue. We will review your report shortly to keep the BookNook community safe.")
                    .setCancelable(false) //now user has to click done to get rid of it. will be better than snackbar. no time constraint
                    .setPositiveButton("Done", (dialog, which) -> {
                        //only close when user clicks done
                        finish();
                    })
                    .show();
        }); //end of listener
    }//end of oncreate
}