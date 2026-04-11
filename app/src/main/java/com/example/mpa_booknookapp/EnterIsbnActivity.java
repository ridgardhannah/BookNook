package com.example.mpa_booknookapp;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.appbar.MaterialToolbar;

public class EnterIsbnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_enter_isbn);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());

        //take the donate/find/wishlist mode and the nook id from previous screen
        String scanMode = getIntent().getStringExtra("MODE");
        String currentNookId = getIntent().getStringExtra("NOOK_ID");

        //find isbn inputfield and search button
        com.google.android.material.textfield.TextInputEditText etIsbn = findViewById(R.id.etIsbn);

        findViewById(R.id.btnLookUp).setOnClickListener(v -> {
            //get the number as a string
            String enteredIsbn = etIsbn.getText().toString().trim();


            //be sure user actully typed an isbn and make another intent to confirm the book
            if (!enteredIsbn.isEmpty()) {
                Intent intent = new Intent(EnterIsbnActivity.this, ConfirmBookActivity.class);
                intent.putExtra("ISBN", enteredIsbn);
                intent.putExtra("MODE", scanMode);
                intent.putExtra("NOOK_ID", currentNookId);
                startActivity(intent);
                finish();
            } else {
                etIsbn.setError("Please enter an ISBN / barcode number.");
            }
        });
    }

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
}