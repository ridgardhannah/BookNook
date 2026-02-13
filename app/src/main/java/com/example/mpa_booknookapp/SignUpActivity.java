package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button createAccountBtn = findViewById(R.id.btnCreateAccount);
        Button loginLinkBtn = findViewById(R.id.btnLoginLink);

        createAccountBtn.setOnClickListener(v -> {
            //takes user to main app screen (map)
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });

        loginLinkBtn.setOnClickListener(v -> {
            //this will close this activity to go back to the login screen
            finish();
        });



    }
}