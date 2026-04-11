package com.example.mpa_booknookapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddNookActivity extends AppCompatActivity {
    //global variables for methods to use
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private double nookLat, nookLng;
    private TextInputEditText etNookName, etDescription;
    private AutoCompleteTextView typeDropdown;
    private Chip chipWheelchair, chipStepFree, chipWellLit, chipSeating, chipToilet, chipParking, chipTransit, chipKidFriendly, chipDogFriendly, chipQuietZone;
    private MaterialButton btnSubmitNook;
    private MaterialSwitch switchOpen247;

    private ActivityResultLauncher<Intent> cameraLauncher;

    //list will hold up to 5 photos
    private List<Uri> photoUris = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_nook);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //initialise ui bits
        etNookName = findViewById(R.id.etNookName);
        etDescription = findViewById(R.id.etDescription);
        typeDropdown = findViewById(R.id.nookType);
        switchOpen247 = findViewById(R.id.switchOpen247);
        etNookName = findViewById(R.id.etNookName);
        etDescription = findViewById(R.id.etDescription);
        typeDropdown = findViewById(R.id.nookType);
        switchOpen247 = findViewById(R.id.switchOpen247);
        chipWheelchair = findViewById(R.id.chipWheelchair);
        chipWellLit = findViewById(R.id.chipWellLit);
        chipSeating = findViewById(R.id.chipSeating);
        chipToilet = findViewById(R.id.chipToilets);
        chipParking = findViewById(R.id.chipParking);
        chipTransit = findViewById(R.id.chipTransit);
        chipKidFriendly = findViewById(R.id.chipKidFriendly);
        chipDogFriendly = findViewById(R.id.chipDogFriendly);
        chipQuietZone = findViewById(R.id.chipQuietZone);
        btnSubmitNook = findViewById(R.id.btnSubmitNook);
        btnSubmitNook = findViewById(R.id.btnSubmitNook);

        findViewById(R.id.toolbarBackBtn).setOnClickListener(v -> finish());


        //camera logic
        LinearLayout llPhotoGallery = findViewById(R.id.llPhotoGallery);
        MaterialCardView cvAddPhoto = findViewById(R.id.cvAddPhoto);

        //will grab the uri when the camera closes
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String uriString = result.getData().getStringExtra("imageUri");
                        if (uriString != null) {
                            android.net.Uri imageUri = android.net.Uri.parse(uriString);

                            //add it to memory list
                            photoUris.add(imageUri);

                            //make new image swaure and add to screen
                            addPhotoToGalleryUI(imageUri, llPhotoGallery, cvAddPhoto);
                        }
                    }
                }
        );

        //for when the add photo square is clicked
        cvAddPhoto.setOnClickListener(v -> {
            if (photoUris.size() >= 5) {
                Toast.makeText(AddNookActivity.this, "Maximum 5 photos allowed!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(AddNookActivity.this, NookCameraActivity.class);
                cameraLauncher.launch(intent);
            }
        });

        //location stuff

        //setup location data
        nookLat = getIntent().getDoubleExtra("lat", 0.0);
        nookLng = getIntent().getDoubleExtra("lng", 0.0);

        //set up mini map fragment doodad
        SupportMapFragment miniMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.miniMap);
        if (miniMapFragment != null) { //do not get rid of the if
            miniMapFragment.getMapAsync(googleMap -> { //async good for ux. can take time to load so make it a background thing, then give it to the {...}
                LatLng nookLocation = new LatLng(nookLat, nookLng);//create coord object
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nookLocation, 16f)); //move camera to the coord object and zoom
                googleMap.getUiSettings().setAllGesturesEnabled(false); //keeps it looking like an image. dont let user move it.
            });
        }

        //set up dropdown for nook types. could add more late but this seems fine for now. will make each a diff colour
        String[] nookTypes = {
                "Community Bookcase (Cafe)",
                "Community Bookcase (Supermarket)",
                "Converted Phone Box",
                "Converted Bus Shelter",
                "Outdoor Reading Spot",
                "Indoor Reading Spot",
                "Council-owned Library",
                "Other"
        };
        //bridge for the list of nook types to the dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nookTypes); //how one row will look. put it in this screen. fill it with types from that list
        typeDropdown.setAdapter(adapter); //attach the data to the autocompletetextview so ser can pick one

        //user taps submit button
        btnSubmitNook.setOnClickListener(button -> submitNookToFirebase()); //click button then execute submit method

        //user taps back button
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(AddNookActivity.this)
                        .setTitle("Discard Nook?")
                        .setMessage("Your progress will be lost. Are you sure?")
                        .setNegativeButton("Keep Editing", null)
                        .setPositiveButton("Discard", (dialog, which) -> finish())
                        .show();
            }
        });
    }

    //builds new ImageView and slots it into the horizontal scroll gallery of pics
    private void addPhotoToGalleryUI(android.net.Uri imageUri, android.widget.LinearLayout galleryLayout, android.view.View addBtn) {
        //create new image view as needed
        android.widget.ImageView newImage = new android.widget.ImageView(this);

        //make size same as add photo button
        int sizeInPx = (int) (100 * getResources().getDisplayMetrics().density);
        int marginInPx = (int) (8 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
        params.setMarginEnd(marginInPx);
        newImage.setLayoutParams(params);

        //set photo
        newImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        newImage.setImageURI(imageUri);

        //add it to layout before the add photo button
        int insertIndex = galleryLayout.getChildCount() - 1;
        galleryLayout.addView(newImage, insertIndex);

        //hide add photo button if up to 5 photos
        if (photoUris.size() >= 5) {
            addBtn.setVisibility(android.view.View.GONE);
        }
    }


    private void submitNookToFirebase() {
        String name = etNookName.getText().toString().trim();
        String type = typeDropdown.getText().toString();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etNookName.setError("Give your Nook a name!");
            return;
        }

        boolean isOpen247 = switchOpen247.isChecked();

        java.util.List<String> selectedAccessibility = new java.util.ArrayList<>();

        if (chipWheelchair.isChecked()) selectedAccessibility.add("Wheelchair Accessible");
        if (chipWellLit.isChecked()) selectedAccessibility.add("Well Lit Area");
        if (chipSeating.isChecked()) selectedAccessibility.add("Seating Available");
        if (chipToilet.isChecked()) selectedAccessibility.add("Weatherproof");
        if (chipParking.isChecked()) selectedAccessibility.add("On-site Parking");
        if (chipTransit.isChecked()) selectedAccessibility.add("Near Public Transit");
        if (chipKidFriendly.isChecked()) selectedAccessibility.add("Kid-Friendly");
        if (chipDogFriendly.isChecked()) selectedAccessibility.add("Dog Friendly");
        if (chipQuietZone.isChecked()) selectedAccessibility.add("Quiet Zone");

        //tell user to wait while it uploads
        btnSubmitNook.setText("Saving Nook...");
        btnSubmitNook.setEnabled(false);

        //put all photo uris into a list of strings
        List<String> localPhotoPaths = new ArrayList<>();
        for (Uri uri : photoUris) {
            localPhotoPaths.add(uri.toString());
        }

        //send list to firebase
        saveNookDataToFirestore(name, type, description, isOpen247, selectedAccessibility, localPhotoPaths);
    }

    private void saveNookDataToFirestore(String name, String type, String desc, boolean is247, java.util.List<String> access, List<String> photoUrls) {
        java.util.Map<String, Object> nookData = new java.util.HashMap<>();
        java.util.List<String> activityTimeline = new java.util.ArrayList<>();
        activityTimeline.add("Nothing has happened at this nook yet. Be the first!");

        nookData.put("name", name);
        nookData.put("nookType", type);
        nookData.put("description", desc);
        nookData.put("isOpen247", is247);
        nookData.put("accessibility", access);
        nookData.put("lat", nookLat);
        nookData.put("lng", nookLng);
        nookData.put("colour", getColourForType(type));
        nookData.put("activityLog", activityTimeline);
        nookData.put("createdAt", com.google.firebase.Timestamp.now());
        nookData.put("imageUrls", photoUrls);

        db.collection("nooks").add(nookData)
                .addOnSuccessListener(documentReference -> {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(AddNookActivity.this)
                            .setTitle("Nook Added!")
                            .setMessage("Your new Nook is now live on the map.")
                            .setCancelable(false)
                            .setPositiveButton("Back to Map", (dialog, which) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitNook.setText("Submit Nook");
                    btnSubmitNook.setEnabled(true);
                });
    }


    //basic colours for pins for now. maybe pick better/brand colours later if time?
    private String getColourForType(String type) {
        switch (type) {
            case "Converted Phone Box":
                return "#D32F2F"; //red
            case "Community Bookcase (Cafe)":
                return "#FF9800"; //orange
            case "Community Bookcase (Supermarket)":
                return "#FBC02D"; //yellow
            case "Outdoor Reading Spot":
                return "#388E3C"; //green
            case "Indoor Reading Spot":
                return "#7B1FA2"; //purple
            case "Converted Bus Shelter":
                return "#F98CB9"; //pink
            case "Council-owned Library":
                return "#03A9F4"; //blue
            default:
                return "#1E88E5"; //blue as default or just in case
        }
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