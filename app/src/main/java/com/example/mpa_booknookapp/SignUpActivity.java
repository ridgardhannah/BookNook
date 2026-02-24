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

        //edge to edge padding stuff
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //initialise buttons
        Button backBtn = findViewById(R.id.btnBack);
        Button createAccountBtn = findViewById(R.id.btnCreateAccount);
        Button loginLinkBtn = findViewById(R.id.btnLoginLink);

        //logic: create account. go to main map
        createAccountBtn.setOnClickListener(v -> {
            //takes user to main app screen (map)
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });

        //logic: login link. closes this screen and returns to login screen
        loginLinkBtn.setOnClickListener(v -> {
            //this will close this activity to go back to the login screen
            finish();
        });

        //logic: back button. closes this screen.
        backBtn.setOnClickListener(v -> {
            finish();
        });

    }//end of onCreate()
}