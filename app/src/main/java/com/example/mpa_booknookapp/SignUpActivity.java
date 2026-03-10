package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    //first get shared instance of FirebaseAuth
    private FirebaseAuth mAuth;

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


        //initialise firebase auth
        mAuth = FirebaseAuth.getInstance();

        //find buttons and views like edit text etc

        EditText etNameInput = findViewById(R.id.etName);
        Button backBtn = findViewById(R.id.btnBack);
        Button createAccountBtn = findViewById(R.id.btnCreateAccount);
        Button loginLinkBtn = findViewById(R.id.btnLoginLink);

        //logic: create account. calls the method, which captures the text. takes user to main map
        createAccountBtn.setOnClickListener(v -> {
            createAccount();
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


    //chheck if user is currently signed in
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            //ie user is already signed in so currentUser has a value in it, go straight to map
            updateUI(currentUser);
        }
    }

    private void createAccount() {
        //needs to get the EditTexts
        EditText etEmailField = findViewById(R.id.etEmail);
        EditText etPasswordField = findViewById(R.id.etPassword);
        EditText etConfirmField = findViewById(R.id.etConfirmPassword);

        //set them as strings
        String email = etEmailField.getText().toString();
        String password = etPasswordField.getText().toString();
        String confirm = etConfirmField.getText().toString();

        //validation for inputs
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        //creates a new user with email address aand password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        //sign up worked. this user is now also logged in too now
                        Log.d("SignUp", "createUserWithEmail:success");

                        //gets user obj
                        FirebaseUser user = mAuth.getCurrentUser();  //the one now signed in
                        updateUI(user);
                    } else {
                        //if the sign in fails, display message to the user
                        Log.w("SignUp", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) { //someone is signed in
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class); //go to map screen
            startActivity(intent); //do it now
            finish();
        }
    }
}

