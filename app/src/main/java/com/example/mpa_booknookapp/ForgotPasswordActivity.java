package com.example.mpa_booknookapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Button sendLinkBtn = findViewById(R.id.btnSendLink);
        Button backBtn = findViewById(R.id.btnBack);

        sendLinkBtn.setOnClickListener(v -> {
            //to test send toast to confirm link sent
            Toast.makeText(ForgotPasswordActivity.this, "Password reset link sent!", Toast.LENGTH_LONG).show();

            //handler to wait 2000 milliseconds
            new android.os.Handler().postDelayed(() -> {

                // codebelow runs after the 2 second wait
                //go to login page and close this activity
                finish();
            }, 2000);
        });


        backBtn.setOnClickListener(v -> finish());


    }
}