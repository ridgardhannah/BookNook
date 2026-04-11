package com.example.mpa_booknookapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class NookDetailsActivity extends AppCompatActivity {

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailAbout = findViewById(R.id.tvAboutDescription);
        tvDetailType = findViewById(R.id.tvDetailTypeDistance);
        cgAccessibility = findViewById(R.id.chipGroupDetailsAccessibility);
        tvRecentActivity = findViewById(R.id.tvRecentActivity);

        String nookName = getIntent().getStringExtra("nookName");
        String nookId = getIntent().getStringExtra("NOOK_ID");
        double lat = getIntent().getDoubleExtra("nookLat", 0.0);
        double lng = getIntent().getDoubleExtra("nookLng", 0.0);


        rvInventory = findViewById(R.id.rvInventory);
        tvEmptyInventory = findViewById(R.id.tvEmptyInventory);
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(bookList);
        rvInventory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvInventory.setAdapter(bookAdapter);

        if (nookId != null) {
            fetchInventory(nookId);
        }

        if (nookName != null) {
            tvDetailName.setText(nookName);
            fetchNookData(nookName);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        Button btnDonate = findViewById(R.id.btnDonate);
        btnDonate.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            intent.putExtra("MODE", "DONATE");
            intent.putExtra("NOOK_ID", nookId);
            startActivity(intent);
        });

        Button btnFind = findViewById(R.id.btnFind);
        btnFind.setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ScannerActivity.class);
            intent.putExtra("MODE", "FIND");
            intent.putExtra("NOOK_ID", nookId);
            startActivity(intent);
        });

        Button btnDirections = findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(v -> {
            //if coords are 0,  intent failed to pass data
            if (lat != 0.0 && lng != 0.0) {
                //build URI for the intent
                //q=lat,lng(Label) puts a pin on the map with the name
                Uri intentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, intentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                //try to launch google maps
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    //use browser if google maps not installed
                    String url = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this, "Location data not available for this Nook", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnReport).setOnClickListener(v -> {
            Intent intent = new Intent(NookDetailsActivity.this, ReportIssueActivity.class);
            intent.putExtra("NOOK_ID", nookId);
            startActivity(intent);
        });
    }

    private void fetchNookData(String name) {
        db.collection("nooks").whereEqualTo("name", name)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        var doc = queryDocumentSnapshots.getDocuments().get(0);

                        tvDetailAbout.setText(doc.getString("description"));
                        tvDetailType.setText(doc.getString("nookType"));

                        ImageView ivMainImage = findViewById(R.id.ivNookImage);
                        LinearLayout llNookGallery = findViewById(R.id.llNookGallery);
                        llNookGallery.removeAllViews();

                        List<String> localPhotoPaths = (List<String>) doc.get("imageUrls");

                        if (localPhotoPaths != null && !localPhotoPaths.isEmpty()) {

                            //main image failsafe
                            try {
                                ivMainImage.setImageURI(Uri.parse(localPhotoPaths.get(0)));
                            } catch (SecurityException e) {
                                android.util.Log.e("NookDetails", "Permission denied for main photo: " + e.getMessage());
                                ivMainImage.setImageResource(R.drawable.img_map_placeholder);
                            }

                            if (localPhotoPaths.size() > 1) {
                                llNookGallery.setVisibility(View.VISIBLE);
                                for (String path : localPhotoPaths) {
                                    ImageView thumb = new ImageView(NookDetailsActivity.this);
                                    int sizeInPx = (int) (80 * getResources().getDisplayMetrics().density);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
                                    params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                                    thumb.setLayoutParams(params);
                                    thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                    // failsafe thumbnail
                                    try {
                                        thumb.setImageURI(Uri.parse(path));
                                    } catch (SecurityException e) {
                                        thumb.setImageResource(R.drawable.img_map_placeholder);
                                    }

                                    // failsafe clicking thumnail
                                    thumb.setOnClickListener(v -> {
                                        try {
                                            ivMainImage.setImageURI(Uri.parse(path));
                                        } catch (SecurityException e) {
                                            ivMainImage.setImageResource(R.drawable.img_map_placeholder);
                                        }
                                    });

                                    llNookGallery.addView(thumb);
                                }
                            } else {
                                llNookGallery.setVisibility(View.GONE);
                            }
                        } else {
                            ivMainImage.setImageResource(R.drawable.img_map_placeholder);
                            llNookGallery.setVisibility(View.GONE);
                        }

                        List<String> activityList = (List<String>) doc.get("activityLog");
                        if (activityList != null && !activityList.isEmpty()) {
                            String latestActivity = activityList.get(activityList.size() - 1);
                            if (latestActivity.contains(" • ")) {
                                String[] parts = latestActivity.split(" • ");
                                String styledText = parts[0] + " <small><i>• " + parts[1] + "</i></small>";
                                tvRecentActivity.setText(HtmlCompat.fromHtml(styledText, HtmlCompat.FROM_HTML_MODE_LEGACY));
                            } else {
                                tvRecentActivity.setText(latestActivity);
                            }
                        }

                        List<String> accessList = (List<String>) doc.get("accessibility");
                        Boolean isOpen247 = doc.getBoolean("isOpen247");

                        updateAccessibilityChips(accessList, isOpen247 != null && isOpen247);
                    }
                });
    }

    private void updateAccessibilityChips(List<String> features, boolean open247) {
        cgAccessibility.removeAllViews();

        if (open247) {
            addChip("Open 24/7", R.drawable.ic_clock);
        }

        if (features != null) {
            for (String feature : features) {
                int iconRes = R.drawable.ic_info;

                if (feature.contains("Wheelchair")) iconRes = R.drawable.ic_wheelchair;
                else if (feature.contains("Well Lit")) iconRes = R.drawable.ic_lightbulb;
                else if (feature.contains("Seating")) iconRes = R.drawable.ic_seating;
                else if (feature.contains("Parking")) iconRes = R.drawable.ic_parking;
                else if (feature.contains("Transit")) iconRes = R.drawable.ic_bus;
                else if (feature.contains("Kid-Friendly")) iconRes = R.drawable.ic_child;
                else if (feature.contains("Dog Friendly")) iconRes = R.drawable.ic_dog;
                else if (feature.contains("Quiet Zone")) iconRes = R.drawable.ic_quiet;

                addChip(feature, iconRes);
            }
        }

        TextView tvLabel = findViewById(R.id.tvFeaturesLabel);
        if (tvLabel != null) {
            tvLabel.setVisibility((open247 || (features != null && !features.isEmpty())) ? View.VISIBLE : View.GONE);
        }
    }

    private void addChip(String text, int iconRes) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(text);
        chip.setChipIconResource(iconRes);
        chip.setChipIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        cgAccessibility.addView(chip);
    }

    private void fetchInventory(String nookId) {
        db.collection("nooks").document(nookId).collection("inventory")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    bookList.clear();

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
                    bookAdapter.notifyDataSetChanged();
                });
    }
}