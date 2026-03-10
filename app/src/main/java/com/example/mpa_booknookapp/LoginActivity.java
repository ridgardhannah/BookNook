package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    //declare it here as a class attr
    //first get shared instance of FirebaseAuth
    //it exists for as long as this activity exists
    //means every method in the class talks to same firebase connection
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        mAuth = FirebaseAuth.getInstance();

        //find all buttons and text etc
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button loginButton = findViewById(R.id.btnLogin);      //using in login logic
        Button SignUpButton = findViewById(R.id.btnSignUp);
        Button forgotPasswordButton = findViewById(R.id.btnForgotPassword);

        //logic to login a user
        loginButton.setOnClickListener(v -> {
            loginUser(etEmail.getText().toString(), etPassword.getText().toString());
        });


        //logic to nav to sign up screen
        SignUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });


        // Logic: Navigate to Forgot Password
        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

    }//end of onCreate()

    @Override
    public void onStart() {
        super.onStart();
        //first checksif user is already signed in/persistence check
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) { //i.e if theres someone loggied in
            updateUI(currentUser);
        }
    }//end of onStart()


    private void loginUser(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show(); //validation checks
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password) //firebase bit
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("Login", "signInWithEmail:success");  //login successful
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        Log.w("Login", "signInWithEmail:failure", task.getException());  //not successful
                        //gonna use a toast for now, fmight change to snackbar later. loo at plans
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }//end of loginUSer()


    private void updateUI(FirebaseUser user) {
        if (user != null) { //if there is a user logged in
            Intent intent = new Intent(LoginActivity.this, MainActivity.class); //to go to map screen
            startActivity(intent);
            finish(); //stops user going back to login wth back button
        }
    }//end of updateUI()

}
