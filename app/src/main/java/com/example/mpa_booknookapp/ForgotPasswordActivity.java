package com.example.mpa_booknookapp;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;


public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        //edge to edge setup bits
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        //find buttons etc
        Button sendResetLinkButton = findViewById(R.id.btnSendLink);
        Button backButton = findViewById(R.id.btnBack);
        EditText etEmail = findViewById(R.id.etEmail);


        //logic to go back to login screen
        backButton.setOnClickListener(v -> finish());


        //logic to send reset link when button clicked
        sendResetLinkButton.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter the email associated with this account", Toast.LENGTH_SHORT).show();
                return;
            }

            //built-in password reset logic from firebase
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            //tell user it was sent
                            Toast.makeText(ForgotPasswordActivity.this, "Reset link sent! Please check your inbox.", Toast.LENGTH_LONG).show();
                            //wait 2 seconds
                            new android.os.Handler().postDelayed(() -> {
                                finish();
                            }, 2000);
                            //take user to Login screen automatically after 2 seconds
                        } else {
                            //tell user the problem
                            Toast.makeText(ForgotPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            //wait 2 seconds
                            new android.os.Handler().postDelayed(() -> {
                                finish();
                            }, 2000);
                            //take user to Login screen automatically after 2 seconds
                        }
                    });
        });//end of clicky for button
    }//end of onCreate()

    //to hide keyboard when tapping away from it
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hideKeyboard(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard(MotionEvent event) {
        View v = getCurrentFocus();
        if (v instanceof EditText) {
            Rect outRect = new android.graphics.Rect();
            v.getGlobalVisibleRect(outRect);

            //if tap is outside a text box to type in
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0); //hide it
                }
            }
        }
    }

}//end of class

