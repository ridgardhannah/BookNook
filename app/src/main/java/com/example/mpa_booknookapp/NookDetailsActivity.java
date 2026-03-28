package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class NookDetailsActivity extends AppCompatActivity {

    //declare the db and the info taken from the bottom dialog to bring to this activity
    private FirebaseFirestore db;
    private TextView tvDetailName;
    private TextView tvDetailAbout;
    private TextView tvDetailType;
    private ChipGroup cgAccessibility;
    private RecyclerView rvInventory;
    private BookAdapter bookAdapter;
    private ArrayList<BookModel> bookList;
    private TextView tvEmptyInventory;
    private TextView tvRecentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nook_details);

        //sort the padding for the top/bottom bars and edge-to-edge
        //waits for system to change size. listens for it
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //initialise the  db and ui elements using the global variables
        db = FirebaseFirestore.getInstance();
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailAbout = findViewById(R.id.tvAboutDescription);
        tvDetailType = findViewById(R.id.tvDetailTypeDistance);
        cgAccessibility = findViewById(R.id.cgAccessibility);

        //get name and id from intent
        String nookName = getIntent().getStringExtra("nookName");
        String nookId = getIntent().getStringExtra("NOOK_ID");

        //initialise the RecyclerView
        rvInventory = findViewById(R.id.rvInventory);
        tvEmptyInventory = findViewById(R.id.tvEmptyInventory);
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(bookList);
        rvInventory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvInventory.setAdapter(bookAdapter);

        //fetch live inventory data from firebase
        if (nookId != null) {
            fetchInventory(nookId);
        }

        if (nookName != null) {
            tvDetailName.setText(nookName); //show it
            fetchNookData(nookName);
        }

        tvRecentActivity = findViewById(R.id.tvRecentActivity);

        //tool bar and buttons on it shenanigans
        //find the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        //logic to handle toolbar back button
        //tells the little arrow at the top to just finish theis screen and go back to the map when tapped
        toolbar.setNavigationOnClickListener(v -> finish());


        //find/donate buttosn
        //donate button opens the scanner
        Button btnDonate = findViewById(R.id.btnDonate);
        btnDonate.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            intent.putExtra("MODE", "DONATE");
            intent.putExtra("NOOK_ID", nookId);
            startActivity(intent);
        });

        //find button opens the scanner
        Button btnFind = findViewById(R.id.btnFind);
        btnFind.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            intent.putExtra("MODE", "FIND");
            intent.putExtra("NOOK_ID", nookId);
            startActivity(intent);
        });

        //directions button currently just placeholder
        Button btnDirections = findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(v -> {
            //later put google map direction stuff here. not priority yet.
        });

        //report button opens report screen
        findViewById(R.id.btnReport).setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ReportIssueActivity.class);
            startActivity(intent);
        });
    }//end of oncreate method


    private void fetchNookData(String name) {
        //look in nooks collection in firebase for nook with the matching name
        db.collection("nooks").whereEqualTo("name", name).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var doc = queryDocumentSnapshots.getDocuments().get(0);

                        tvDetailAbout.setText(doc.getString("description"));
                        tvDetailType.setText(doc.getString("nookType"));

                        List<String> activityList = (List<String>) doc.get("activityLog");
                        if (activityList != null && !activityList.isEmpty()) {
                            //grab the last item in the list. newest activity
                            String latestActivity = activityList.get(activityList.size() - 1);
                            if (latestActivity.contains(" • ")) {
                                String[] parts = latestActivity.split(" • ");
                                String styledText = parts[0] + " <small><i>&bull; " + parts[1] + "</i></small>";
                                tvRecentActivity.setText(androidx.core.text.HtmlCompat.fromHtml(styledText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
                            } else {
                                tvRecentActivity.setText(latestActivity);
                            }                        }

                        //handle Accessibility Chips
                        List<String> accessList = (List<String>) doc.get("accessibility");
                        Boolean isOpen247 = doc.getBoolean("isOpen247");

                        updateAccessibilityChips(accessList, isOpen247 != null && isOpen247);
                    }
                });
    }

    private void updateAccessibilityChips(List<String> features, boolean open247) {
        cgAccessibility.removeAllViews();

        //if true, ad 247 chip
        if (open247) {
            addChip("Open 24/7", R.drawable.ic_clock);

        }

        if (features != null) {
            for (String feature : features) {
                int iconRes = R.drawable.ic_info; // default just in case dont have icon ready in future. note. make chip for dof friendly. will be v cute
                if (feature.contains("Wheelchair")) iconRes = R.drawable.ic_wheelchair;
                if (feature.contains("Well Lit")) iconRes = R.drawable.ic_lightbulb;

                addChip(feature, iconRes);
            }
        }
    }

    private void addChip(String text, int iconRes) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(text);
        chip.setChipIconResource(iconRes);
        chip.setChipIconVisible(true);
        chip.setCheckable(false);  //cant edit them
        chip.setClickable(false);
        cgAccessibility.addView(chip);
    }

    private void fetchInventory(String nookId) {
        db.collection("nooks").document(nookId).collection("inventory")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return; //ignore errors for now
                    bookList.clear(); //clear the old list

                    if (value != null && !value.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            String title = doc.getString("title");
                            String author = doc.getString("author");
                            String coverUrl = doc.getString("coverUrl");
                            bookList.add(new BookModel(title, author, coverUrl));
                        }
                        rvInventory.setVisibility(View.VISIBLE);
                        tvEmptyInventory.setVisibility(View.GONE);
                    } else {
                        rvInventory.setVisibility(View.GONE);
                        tvEmptyInventory.setText("This Nook is currently empty. Be the first to donate!");
                        tvEmptyInventory.setVisibility(View.VISIBLE);
                    }
                    bookAdapter.notifyDataSetChanged(); //tell the screen to refresh
                });
    }

}