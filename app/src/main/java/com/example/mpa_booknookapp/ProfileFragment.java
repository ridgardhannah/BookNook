package com.example.mpa_booknookapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

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
        RecyclerView listView = view.findViewById(R.id.lvProfileContent);
        ScrollView achievementsGrid = view.findViewById(R.id.achievementsContainer);

        //use firebase to set up stats
        setupStats(view);


        ArrayList<BookModel> journeyList = new ArrayList<>();
        //dummy data activity list. put back in if needed
        //journeyList.add(new BookModel("Donated 'The Hobbit'", "Derby Library • 14 Oct"));
        //journeyList.add(new BookModel("Found '1984'", "Castle St Nook • 10 Oct"));

        //setup for recyclerview
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        listView.setAdapter(new BookAdapter(journeyList));

        //tab listener for manual clicks
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    listView.setVisibility(View.VISIBLE);
                    achievementsGrid.setVisibility(View.GONE);
                } else {
                    listView.setVisibility(View.GONE);
                    achievementsGrid.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //check bundle
        boolean openFromBundle = getArguments() != null && getArguments().getBoolean("SHOW_ACHIEVEMENTS", false);

        //check intent
        boolean openFromIntent = false;
        if (getActivity() != null && getActivity().getIntent() != null) {
            openFromIntent = getActivity().getIntent().getBooleanExtra("OPEN_ACHIEVEMENTS", false);
        }

        //if either found, switch
        if (openFromBundle || openFromIntent) {
            //select second tab
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                tabLayout.selectTab(tab);
            }

            //flip visibility so it shows immediately
            listView.setVisibility(View.GONE);
            achievementsGrid.setVisibility(View.VISIBLE);

            //clear messages so they dnt fire again
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
                        String nooks = String.valueOf(documentSnapshot.get("nooksCount"));
                        String donated = String.valueOf(documentSnapshot.get("donatedCount"));
                        String found = String.valueOf(documentSnapshot.get("foundCount"));

                        //get current title from firebase
                        String currentTitle = documentSnapshot.getString("currentTitle");

                        //if user already has one equpped show it. Or use default
                        if (currentTitle != null && !currentTitle.isEmpty()) {
                            tvUserBadge.setText(currentTitle);
                        } else {
                            tvUserBadge.setText("Wandering Reader"); //default for new accounts
                        }

                        ((TextView) view.findViewById(R.id.statNooks).findViewById(R.id.tvStatLabel)).setText("Nooks Visited");
                        ((TextView) view.findViewById(R.id.statDonated).findViewById(R.id.tvStatLabel)).setText("Donated");
                        ((TextView) view.findViewById(R.id.statFound).findViewById(R.id.tvStatLabel)).setText("Found");

                        tvNooks.setText(!nooks.equals("null") ? nooks : "0");
                        tvDonated.setText(!donated.equals("null") ? donated : "0");
                        tvFound.setText(!found.equals("null") ? found : "0");

                        //convert firebase string into an int for calcs
                        int donatedInt = !donated.equals("null") ? Integer.parseInt(donated) : 0;
                        int foundInt = !found.equals("null") ? Integer.parseInt(found) : 0;


                        //medal 2 bookworm. main medal anchor point
                        //donated 10+ books?
                        boolean isBookwormUnlocked = donatedInt >= 10;
                        //call helper to build the medal
                        setupMedalLogic(
                                view, R.id.medal_2, //medal id
                                "Bookworm", //title
                                "Donate 10 books to Nooks to unlock this badge.", //desc
                                isBookwormUnlocked, //bool
                                R.drawable.ic_book  //icon
                        );

                    }
                });
    }

    //to set up how medal looks
    private void setupMedalLogic(View view, int medalId, String title, String desc, boolean isUnlocked, int iconResId) {
        android.widget.FrameLayout medalFrame = view.findViewById(medalId);
        if (medalFrame == null) return;

        //go into the frameLayout to grab the pieces
        ImageView bgImage = (ImageView) medalFrame.getChildAt(0);
        LinearLayout textStack = (android.widget.LinearLayout) medalFrame.getChildAt(1);
        ImageView icon= (ImageView) textStack.getChildAt(0);
        TextView labelText = (TextView) textStack.getChildAt(1);

        //apply the specific icon and title
        icon.setImageResource(iconResId);
        labelText.setText(title);

        //gold or gery
        if (isUnlocked) {
            bgImage.setImageResource(R.drawable.shape_hexagon_gold);
            icon.setColorFilter(Color.BLACK);
            labelText.setTextColor(Color.BLACK);
        } else {
            bgImage.setImageResource(R.drawable.shape_hexagon_grey);
            icon.setColorFilter(Color.parseColor("#888888"));
            labelText.setTextColor(Color.parseColor("#888888"));
        }

        //set up the clicky for the pop-up
        medalFrame.setOnClickListener(v -> showMedalDialog(title, desc, isUnlocked, iconResId));
    }

    //to build and shows the pop-up
    private void showMedalDialog(String title, String desc, boolean isUnlocked, int iconResId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_achievement, null);

        ImageView hexBg = dialogView.findViewById(R.id.dialogHexBg);
        ImageView hexIcon = dialogView.findViewById(R.id.dialogHexIcon);
        TextView hexLabel = dialogView.findViewById(R.id.dialogHexLabel);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView tvDate = dialogView.findViewById(R.id.dialogDate);
        TextView tvDesc = dialogView.findViewById(R.id.dialogDesc);

        android.widget.Button btnOkay = dialogView.findViewById(R.id.dialogBtnOkay);
        android.widget.Button btnEquip = dialogView.findViewById(R.id.dialogBtnEquip);

        tvTitle.setText(title);
        hexLabel.setText(title);
        tvDesc.setText(desc);
        hexIcon.setImageResource(iconResId);

        if (isUnlocked) {
            hexBg.setImageResource(R.drawable.shape_hexagon_gold);
            hexIcon.setColorFilter(Color.BLACK);
            hexLabel.setTextColor(Color.BLACK);
            tvDate.setVisibility(View.VISIBLE); //show unlocked text if unlocked

            //show equip button if unlocked. give a clicky
            btnEquip.setVisibility(View.VISIBLE);
            btnEquip.setOnClickListener(v -> {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("currentTitle", title) // creates/updates firebase field
                        .addOnSuccessListener(aVoid -> {
                            android.widget.Toast.makeText(requireContext(), title + " Equipped!", android.widget.Toast.LENGTH_SHORT).show();
                        });
            });

        } else {
            hexBg.setImageResource(R.drawable.shape_hexagon_grey);
            hexIcon.setColorFilter(Color.parseColor("#888888"));
            hexLabel.setTextColor(Color.parseColor("#888888"));
            tvDate.setVisibility(View.GONE); //hide the date if locked
            btnEquip.setVisibility(View.GONE); //stays hidden if locked
        }

        //make and show the dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        //background transparent to round corners off
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOkay.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }




}