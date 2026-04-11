package com.example.mpa_booknookapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity {

    //photos variables
    //temp photo store for up to 5 pics
    private List<Uri> photoUris = new ArrayList<>();
    //intent to open camera
    private ActivityResultLauncher<Intent> cameraLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        //set up back button in toolbar
        Toolbar toolbar = findViewById(R.id.toolbarReport);
        toolbar.setNavigationOnClickListener(v -> finish());

        //set up gallery ui
        LinearLayout llPhotoGallery = findViewById(R.id.llReportPhotoGallery);
        MaterialCardView cvAddPhoto = findViewById(R.id.cvAddReportPhoto);

        //listens for a pic being taken and adds it to the gallery
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String uriString = result.getData().getStringExtra("imageUri");
                        if (uriString != null) {
                            Uri imageUri = Uri.parse(uriString);
                            //add new pic to list
                            photoUris.add(imageUri);
                            //make photo square and add to screen
                            addPhotoToGalleryUI(imageUri, llPhotoGallery, cvAddPhoto);
                        }
                    }
                }
        );

        //user taps add photos
        cvAddPhoto.setOnClickListener(v -> {
            if (photoUris.size() >= 5) {
                Toast.makeText(this, "Maximum 5 photos allowed!", Toast.LENGTH_SHORT).show();
            } else {
                //launch the camera screen
                Intent intent = new Intent(this, NookCameraActivity.class);
                cameraLauncher.launch(intent);
            }
        });

        //set up submit button
        Button submitBtn = findViewById(R.id.btnSubmitReport);
        submitBtn.setOnClickListener(v -> {

            //find all the checkboxes and text fields
            MaterialCheckBox damaged = findViewById(R.id.cbDamaged);
            MaterialCheckBox notExist = findViewById(R.id.cbNotExist);
            MaterialCheckBox inappropriate = findViewById(R.id.cbInappropriate);
            MaterialCheckBox other = findViewById(R.id.cbOther);
            EditText detailsBox = findViewById(R.id.etReportDetails);

            //create a list of whatever the user checked
            List<String> issuesList = new ArrayList<>();
            if (damaged.isChecked()) issuesList.add("Damaged / Vandalised");
            if (notExist.isChecked()) issuesList.add("Nook doesn't exist");
            if (inappropriate.isChecked()) issuesList.add("Inappropriate content");
            if (other.isChecked()) issuesList.add("Other");

            String details = detailsBox.getText().toString().trim();

            //there has to be at least one issue or details
            if (issuesList.isEmpty() && details.isEmpty()) {
                Toast.makeText(this, "Please select an issue or provide details.", Toast.LENGTH_SHORT).show();
                return;
            }

            //lock the button so they don't double-click and submit twice
            submitBtn.setEnabled(false);
            submitBtn.setText("Submitting...");

            //data for firebase
            Map<String, Object> reportData = new HashMap<>();

            //grab nook id from previous screen
            String nookId = getIntent().getStringExtra("NOOK_ID");
            reportData.put("nookId", nookId != null ? nookId : "Unknown Nook");

            //who is logged in. attach their id as they are doing the complaining
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                reportData.put("reporterId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            }

            //add data to report
            reportData.put("issues", issuesList);
            reportData.put("details", details);
            reportData.put("status", "Pending Review");
            reportData.put("timestamp", FieldValue.serverTimestamp());

            //turn photos into textt. put in report data box
            List<String> localPhotoPaths = new ArrayList<>();
            for (Uri uri : photoUris) {
                localPhotoPaths.add(uri.toString());
            }
            reportData.put("photoUrls", localPhotoPaths);

            //send to firebase
            FirebaseFirestore.getInstance().collection("reports").add(reportData)
                    .addOnSuccessListener(documentReference -> {
                        //success dialog
                        new MaterialAlertDialogBuilder(ReportIssueActivity.this)
                                .setTitle("Report Submitted")
                                .setMessage("Thank you for flagging this issue. We will review your report shortly to keep the BookNook community safe.")
                                .setCancelable(false)
                                .setPositiveButton("Done", (dialog, which) -> finish()) //closes screen when done
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        //if it fails, unlock the button so they can try again
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        submitBtn.setEnabled(true);
                        submitBtn.setText("Submit Report");
                    });
        });
    } //end of onCreate


    //draw the photo thumbnails on the screen
    private void addPhotoToGalleryUI(Uri imageUri, LinearLayout galleryLayout, View addBtn) {
        ImageView newImage = new ImageView(this);

        //same size as add photo button
        int sizeInPx = (int) (100 * getResources().getDisplayMetrics().density);
        int marginInPx = (int) (8 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
        params.setMarginEnd(marginInPx);
        newImage.setLayoutParams(params);
        newImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        newImage.setImageURI(imageUri);

        //let user tap a photo to delete it if they took a bad shot!
        newImage.setOnClickListener(v -> {
            galleryLayout.removeView(newImage); //remove from thescreen
            photoUris.remove(imageUri);         //remove from memory
            addBtn.setVisibility(View.VISIBLE); //show add photo button again
        });

        //add the picture to the layout right before the empty add photo square
        int insertIndex = galleryLayout.getChildCount() - 1;
        galleryLayout.addView(newImage, insertIndex);

        //if photos => 5 then hide add button
        if (photoUris.size() >= 5) {
            addBtn.setVisibility(View.GONE);
        }
    }

   //keyboard hiding
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
            Rect outRect = new Rect();
            v.getGlobalVisibleRect(outRect);

            //if tap is outside a text box
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
    }
}