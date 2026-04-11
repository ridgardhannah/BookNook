package com.example.mpa_booknookapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    //variables for activity feed
    private int activityDisplayLimit = 5;
    private List<String> fullActivityList = new ArrayList<>();

    //global variable for camera
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //settings icon logic
        ImageView settingsIcon = view.findViewById(R.id.ivSettings);
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        //find stuff for ui
        TabLayout tabLayout = view.findViewById(R.id.profileTabLayout);
        ScrollView achievementsGrid = view.findViewById(R.id.achievementsContainer);
        LinearLayout activityContainer = view.findViewById(R.id.myActivityContainer);
        RecyclerView listView = view.findViewById(R.id.lvProfileContent);
        Button btnViewMore = view.findViewById(R.id.btnViewMoreActivity);

        //set up camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        String uriString = result.getData().getStringExtra("imageUri");
                        if (uriString != null) {
                            Uri imageUri = Uri.parse(uriString);

                            //update ui now
                            ShapeableImageView ivAvatar = view.findViewById(R.id.ivAvatar);
                            ivAvatar.setImageURI(imageUri);

                            //save it to firebase so it stays there forever
                            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                            FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .update("profilePicUrl", uriString)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
        );

        //make avatar clickable
        ShapeableImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NookCameraActivity.class);
            //we want front camera to take selfie
            intent.putExtra("USE_FRONT_CAMERA", true);

            cameraLauncher.launch(intent);
        });

        //make name clickable
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        tvUserName.setOnClickListener(v -> {
            EditText input = new EditText(getContext());
            input.setText(tvUserName.getText()); //start as current name

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Edit Name")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            //update ui now
                            tvUserName.setText(newName);

                            //update firebase
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .update("name", newName);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        //view more clicky
        btnViewMore.setOnClickListener(v -> {
            activityDisplayLimit += 5; //load 5 more to limit
            refreshActivityFeed(view); //show list with those extra 5 added as limit is bigger now
        });

        //setup for recyclerview layout
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        listView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        //use firebase to set up stats and pull activity feed
        setupStats(view);

        //tab listener for user clicks
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    //hide achievements, show activity container with button
                    activityContainer.setVisibility(View.VISIBLE);
                    achievementsGrid.setVisibility(View.GONE);
                } else {
                    //hide activity container, show achievements tab
                    activityContainer.setVisibility(View.GONE);
                    achievementsGrid.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        //check iintent/bundle to see if we should open straight to achievements or not
        boolean openFromBundle = getArguments() != null && getArguments().getBoolean("SHOW_ACHIEVEMENTS", false);
        boolean openFromIntent = false;
        if (getActivity() != null && getActivity().getIntent() != null) {
            openFromIntent = getActivity().getIntent().getBooleanExtra("OPEN_ACHIEVEMENTS", false);
        }

        //if either found, switch tabs
        if (openFromBundle || openFromIntent) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                tabLayout.selectTab(tab);
            }
            activityContainer.setVisibility(View.GONE);
            achievementsGrid.setVisibility(View.VISIBLE);

            if (getArguments() != null) getArguments().remove("SHOW_ACHIEVEMENTS");
            if (getActivity() != null && getActivity().getIntent() != null) {
                getActivity().getIntent().removeExtra("OPEN_ACHIEVEMENTS");
            }
        }

        return view;
    }

    private void setupStats(View view) {
        TextView tvNooks = view.findViewById(R.id.statNooks).findViewById(R.id.tvStatNumber);
        TextView tvDonated = view.findViewById(R.id.statDonated).findViewById(R.id.tvStatNumber);
        TextView tvFound = view.findViewById(R.id.statFound).findViewById(R.id.tvStatNumber);
        TextView tvUserBadge = view.findViewById(R.id.tvUserBadge);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {

                        //user info and badge
                        String realName = documentSnapshot.getString("name");
                        TextView tvUserName = view.findViewById(R.id.tvUserName);
                        if (realName != null && tvUserName != null) {
                            tvUserName.setText(realName);
                        }

                        String currentTitle = documentSnapshot.getString("currentTitle");
                        if (currentTitle != null && !currentTitle.isEmpty()) {
                            tvUserBadge.setText(currentTitle);
                        } else {
                            tvUserBadge.setText("Bookworm");
                        }

                        //load profile piccie
                        String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            com.google.android.material.imageview.ShapeableImageView avatar = getView().findViewById(R.id.ivAvatar);
                            if (avatar != null) {
                                try {
                                    avatar.setImageURI(android.net.Uri.parse(profilePicUrl));
                                } catch (SecurityException e) {
                                    avatar.setImageResource(R.drawable.ic_account);
                                }
                            }
                        }

                        //fetch stats
                        //keep getlong.....seems to have fixes crash problem. do not touch it now!
                        Long nooksLong = documentSnapshot.getLong("nooksCount");
                        int nooksInt = (nooksLong != null) ? nooksLong.intValue() : 0;

                        Long donatedLong = documentSnapshot.getLong("donatedCount");
                        int donatedInt = (donatedLong != null) ? donatedLong.intValue() : 0;

                        Long foundLong = documentSnapshot.getLong("foundCount");
                        int foundInt = (foundLong != null) ? foundLong.intValue() : 0;

                        Long addedLong = documentSnapshot.getLong("wishlistAddedCount");
                        int wishlistAddedInt = (addedLong != null) ? addedLong.intValue() : 0;

                        Long wishlistFoundLong = documentSnapshot.getLong("wishlistFoundCount");
                        int wishlistFoundInt = (wishlistFoundLong != null) ? wishlistFoundLong.intValue() : 0;

                        //ui for stat cards at top
                        ((TextView) view.findViewById(R.id.statNooks).findViewById(R.id.tvStatLabel)).setText("Nooks Visited");
                        ((TextView) view.findViewById(R.id.statDonated).findViewById(R.id.tvStatLabel)).setText("Donated");
                        ((TextView) view.findViewById(R.id.statFound).findViewById(R.id.tvStatLabel)).setText("Found");

                        tvNooks.setText(String.valueOf(nooksInt));
                        tvDonated.setText(String.valueOf(donatedInt));
                        tvFound.setText(String.valueOf(foundInt));

                        //big list of all achievements
                        //find books
                        setupMedalLogic(view, R.id.medal_1, R.id.medal_1_hex, R.id.medal_1_icon, R.id.medal_1_label, "Reader", "Find 5 books.", foundInt >= 5, R.drawable.ic_book);
                        setupMedalLogic(view, R.id.medal_2, R.id.medal_2_hex, R.id.medal_2_icon, R.id.medal_2_label, "Bookworm", "Find 10 books.", foundInt >= 10, R.drawable.ic_book2);
                        setupMedalLogic(view, R.id.medal_3, R.id.medal_3_hex, R.id.medal_3_icon, R.id.medal_3_label, "Archivist", "Find 30 books.", foundInt >= 30, R.drawable.ic_book3);
                        setupMedalLogic(view, R.id.medal_4, R.id.medal_4_hex, R.id.medal_4_icon, R.id.medal_4_label, "Book Keeper", "Find 60 books.", foundInt >= 60, R.drawable.ic_book4);

                        //explore nooks
                        setupMedalLogic(view, R.id.medal_5, R.id.medal_5_hex, R.id.medal_5_icon, R.id.medal_5_label, "Scout", "Explore 5 unique Nooks.", nooksInt >= 5, R.drawable.ic_explore);
                        setupMedalLogic(view, R.id.medal_6, R.id.medal_6_hex, R.id.medal_6_icon, R.id.medal_6_label, "Explorer", "Explore 10 unique Nooks.", nooksInt >= 10, R.drawable.ic_map);
                        setupMedalLogic(view, R.id.medal_7, R.id.medal_7_hex, R.id.medal_7_icon, R.id.medal_7_label, "Pathfinder", "Explore 20 unique Nooks.", nooksInt >= 20, R.drawable.ic_world);

                        //donating
                        setupMedalLogic(view, R.id.medal_8, R.id.medal_8_hex, R.id.medal_8_icon, R.id.medal_8_label, "Giver", "Donate 5 books.", donatedInt >= 5, R.drawable.ic_heart);
                        setupMedalLogic(view, R.id.medal_9, R.id.medal_9_hex, R.id.medal_9_icon, R.id.medal_9_label, "Patron", "Donate 10 books.", donatedInt >= 10, R.drawable.ic_gift);
                        setupMedalLogic(view, R.id.medal_10, R.id.medal_10_hex, R.id.medal_10_icon, R.id.medal_10_label, "Book Fairy", "Donate 20 books.", donatedInt >= 20, R.drawable.ic_wand);

                        //wishlist
                        setupMedalLogic(view, R.id.medal_11, R.id.medal_11_hex, R.id.medal_11_icon, R.id.medal_11_label, "Dreamer", "Add 1 book to Wishlist.", wishlistAddedInt >= 1, R.drawable.ic_add);
                        setupMedalLogic(view, R.id.medal_12, R.id.medal_12_hex, R.id.medal_12_icon, R.id.medal_12_label, "Magpie", "Add 10 books to Wishlist.", wishlistAddedInt >= 10, R.drawable.ic_wishlist2);
                        setupMedalLogic(view, R.id.medal_13, R.id.medal_13_hex, R.id.medal_13_icon, R.id.medal_13_label, "Curator", "Add 20 books to Wishlist.", wishlistAddedInt >= 20, R.drawable.ic_bookmark);

                        //other
                        setupMedalLogic(view, R.id.medal_14, R.id.medal_14_hex, R.id.medal_14_icon, R.id.medal_14_label, "Gotcha", "Find 1 Wishlist book.", wishlistFoundInt >= 1, R.drawable.ic_star2);
                        setupMedalLogic(view, R.id.medal_15, R.id.medal_15_hex, R.id.medal_15_icon, R.id.medal_15_label, "Detective", "Find 5 Wishlist books.", wishlistFoundInt >= 5, R.drawable.ic_star);

                        //activity feed
                        List<String> activities = (List<String>) documentSnapshot.get("recentActivity");
                        fullActivityList.clear();

                        if (activities != null && !activities.isEmpty()) {
                            //newest first. backwards loop
                            for (int i = activities.size() - 1; i >= 0; i--) {
                                fullActivityList.add(activities.get(i));
                            }
                        }

                        //call method to draw in items
                        refreshActivityFeed(view);
                    }
                });
    }

    //helper method to draw right number of items in activity feed
    private void refreshActivityFeed(View view) {
        if (view == null) return;

        //slice list at limit. starts as 5
        List<String> displayList = new ArrayList<>();
        for (int i = 0; i < Math.min(fullActivityList.size(), activityDisplayLimit); i++) {
            displayList.add(fullActivityList.get(i));
        }

        //only send this sliced bit to screen
        RecyclerView rvActivity = view.findViewById(R.id.lvProfileContent);
        if (rvActivity != null) {
            rvActivity.setAdapter(new TextOnlyAdapter(displayList));
        }

        //if all items are there dont show the view more button
        Button btnViewMore = view.findViewById(R.id.btnViewMoreActivity);
        if (btnViewMore != null) {
            if (activityDisplayLimit >= fullActivityList.size() || fullActivityList.isEmpty()) {
                btnViewMore.setVisibility(View.GONE);
            } else {
                btnViewMore.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupMedalLogic(View view, int containerId, int hexId, int iconId, int labelId, String title, String description, boolean isUnlocked, int iconResId) {

        //find views
        View medalView = view.findViewById(containerId);
        if (medalView == null) return;

        ImageView ivHex = medalView.findViewById(hexId);
        ImageView ivIcon = medalView.findViewById(iconId);
        TextView tvTitle = medalView.findViewById(labelId);

        //set text and icon
        if (tvTitle != null) tvTitle.setText(title);
        if (ivIcon != null) ivIcon.setImageResource(iconResId);

        //grey->gold logic
        if (ivHex != null) {
            if (isUnlocked) {
                //when unlocked gold
                ivHex.clearColorFilter();
                ivHex.setAlpha(1.0f);
                if (tvTitle != null) tvTitle.setTextColor(android.graphics.Color.BLACK);
                if (ivIcon != null) ivIcon.setColorFilter(android.graphics.Color.BLACK);
            } else {
                //when locked grey
                android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
                matrix.setSaturation(0);
                ivHex.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                ivHex.setAlpha(0.4f);

                if (tvTitle != null)
                    tvTitle.setTextColor(android.graphics.Color.parseColor("#888888"));
                if (ivIcon != null)
                    ivIcon.setColorFilter(android.graphics.Color.parseColor("#888888"));
            }
        }

        //clicky for medal
        medalView.setOnClickListener(v -> {
            //show medal popup
            showAchievementPopup(title, description, isUnlocked, iconResId);
        });
    }


    private void showAchievementPopup(String title, String description, boolean isUnlocked, int iconResId) {
        //inflate popup layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_achievement, null);

        //find all dialogs bits
        ImageView dialogHexBg = dialogView.findViewById(R.id.dialogHexBg);
        ImageView dialogHexIcon = dialogView.findViewById(R.id.dialogHexIcon);
        TextView dialogHexLabel = dialogView.findViewById(R.id.dialogHexLabel);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogDesc = dialogView.findViewById(R.id.dialogDesc);
        TextView dialogDate = dialogView.findViewById(R.id.dialogDate);
        Button btnEquip = dialogView.findViewById(R.id.dialogBtnEquip);
        Button btnOkay = dialogView.findViewById(R.id.dialogBtnOkay);

        //set text and icon
        dialogTitle.setText(title);
        dialogDesc.setText(description);
        dialogHexLabel.setText(title);
        dialogHexIcon.setImageResource(iconResId);

        //apply gold or grey colour
        if (isUnlocked) {
            dialogHexBg.setImageResource(R.drawable.shape_hexagon_gold);
            dialogDate.setText("Unlocked!");
            dialogDate.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
            btnEquip.setVisibility(View.VISIBLE); //show equip button if unlocked
        } else {
            dialogHexBg.setImageResource(R.drawable.shape_hexagon_grey);
            dialogDate.setText("Locked");
            dialogDate.setTextColor(android.graphics.Color.parseColor("#888888"));
            btnEquip.setVisibility(View.GONE); //locked. hide equip button
        }

        //build and show dialog
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        //button clicks
        btnOkay.setOnClickListener(v -> dialog.dismiss());
        btnEquip.setOnClickListener(v -> {
            equipUserTitle(title);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void equipUserTitle(String newTitle) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("currentTitle", newTitle)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Title equipped: " + newTitle, Toast.LENGTH_SHORT).show();
                });
    }




}