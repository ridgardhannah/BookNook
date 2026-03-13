package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddNookActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private double nookLat;
    private double nookLng;

    private com.google.android.material.textfield.TextInputEditText etNookName;
    private com.google.android.material.textfield.TextInputEditText etDescription;
    private android.widget.AutoCompleteTextView typeDropdown;
    private com.google.android.material.chip.Chip chipWheelchair;
    private com.google.android.material.chip.Chip chipWellLit;
    private com.google.android.material.chip.Chip chipOpenAlways;
    private com.google.android.material.button.MaterialButton btnSubmitNook;


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

        etNookName = findViewById(R.id.etNookName);
        etDescription = findViewById(R.id.etDescription);
        typeDropdown = findViewById(R.id.nookType);

        chipWheelchair = findViewById(R.id.chipWheelchair);
        chipWellLit = findViewById(R.id.chipWellLit);
        chipOpenAlways = findViewById(R.id.chipOpenAlways);

        btnSubmitNook = findViewById(R.id.btnSubmitNook);


        //toolbar back button
        findViewById(R.id.toolbarBackBtn).setOnClickListener(v -> finish());

        //MINI MAP
        //get lat and lng from the intent first
        nookLat = getIntent().getDoubleExtra("lat", 0.0); //default 0s for fallbacks. better thn nothing
        nookLng = getIntent().getDoubleExtra("lng", 0.0);

        //set up minimap
        SupportMapFragment miniMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.miniMap);
        //finds map fragment
        if (miniMapFragment != null) {
            miniMapFragment.getMapAsync(googleMap -> {
                LatLng nookLocation = new LatLng(nookLat, nookLng);
                //zoom i
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nookLocation, 16f));
                //dont let map move about. markr is in the middle over the correct spot
                googleMap.getUiSettings().setAllGesturesEnabled(false);
            });
        }

        //types of nooks
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

        AutoCompleteTextView nookTypeDropdown = findViewById(R.id.nookType);

        //link the list of types to the dropdown options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nookTypes
        );

//stick adapter to dropdown
        nookTypeDropdown.setAdapter(adapter);

        //submit nook button
        Button btnSubmitNook = findViewById(R.id.btnSubmitNook);

        //submit button clicky bit
        btnSubmitNook.setOnClickListener(v -> {
            // Show the Snackbar on the current screen
            Snackbar.make(findViewById(android.R.id.content),
                    "Nook submitted! Waiting for review.",
                    Snackbar.LENGTH_LONG).show();

            //dont just finish, give usr couple seconds to read mesage first
            new Handler().postDelayed(() -> {
                finish();
            }, 2000);
        });

        //submit button ->submit method
        btnSubmitNook.setOnClickListener(v -> submitNookToFirebase());

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

    }//end of oncreate

    private void submitNookToFirebase() {
        //get data
        String name = etNookName.getText().toString().trim();
        String type = typeDropdown.getText().toString();
        String description = etDescription.getText().toString().trim();

        //bare minimum the user has to name it
        if (name.isEmpty()) {
            etNookName.setError("Give your Nook a name!");
            return;
        }

        //get all the accessibility chips and put in array
        List<String> accessibilityFeatures = new ArrayList<>();
        if (chipWheelchair.isChecked()) accessibilityFeatures.add("Wheelchair Accessible");
        if (chipWellLit.isChecked()) accessibilityFeatures.add("Well Lit Area");
        if (chipOpenAlways.isChecked()) accessibilityFeatures.add("Open 24/7");

        //set pin colour
        String pinColourHex = getColourForType(type);

        //creates document in firebase
        Map<String, Object> nookData = new HashMap<>();
        List<String> activityTimeline = new ArrayList<>();
        activityTimeline.add("Nothing has happened at this nook yet. Be the first!");

        nookData.put("name", name);
        nookData.put("nookType", type);
        nookData.put("description", description);
        nookData.put("accessibility", accessibilityFeatures);
        nookData.put("lat", nookLat); //take from intent
        nookData.put("lng", nookLng);
        nookData.put("colour", pinColourHex);
        nookData.put("activityLog", activityTimeline);
        nookData.put("createdAt", com.google.firebase.Timestamp.now());

        //send data to firestore
        db.collection("nooks").add(nookData)
                .addOnSuccessListener(documentReference -> {
                    //confirmation dialog
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(AddNookActivity.this)
                            .setTitle("Nook Added!")
                            .setMessage("Your new Nook is now live on the map for the community to find.")
                            .setCancelable(false) //click the button to leave
                            .setPositiveButton("Back to Map", (dialog, which) -> {
                                finish();
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    //uh oh toast
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }//end of submit to firebase

    //assign colours for nook types
    private String getColourForType(String type) {
        if (type.equals("Converted Phonebox")) return "#E53935"; //red
        if (type.equals("Community Bookcase (Cafe)")) return "#FB8C00"; //orange
        if (type.equals("Community Bookcase (Supermarket)")) return "#FDD835"; //yellow
        if (type.equals("Outdoor Reading Spot")) return "#43A047"; //green
        if (type.equals("Indoor Reading Spot")) return "#00ACC1"; //bright ish blue
        if (type.equals("Council-owned Library")) return "#1E88E5"; //blue
        if (type.equals("Converted Bus Shelter")) return "#D81B60"; //pinkish
        if (type.equals("Other")) return "#5E35B1"; //purple

        return "#1E88E5"; //blue
    }

}