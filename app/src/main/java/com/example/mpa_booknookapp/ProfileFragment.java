package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //inflate the layout
        View view = inflater.inflate(R.layout.fragment_profile, container, false);



        ImageView settingsIcon = view.findViewById(R.id.ivSettings);
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });


        //find the components for tab layout and the list veiw
        TabLayout tabLayout = view.findViewById(R.id.profileTabLayout);
        ListView listView = view.findViewById(R.id.lvProfileContent);

        //set up stat cards
        setupStats(view);

        //create data for lists. fake for now
        ArrayList<BookModel> journeyList = new ArrayList<>();
        journeyList.add(new BookModel("Donated 'The Hobbit'", "Derby Library • 14 Oct"));
        journeyList.add(new BookModel("Found '1984'", "Castle St Nook • 10 Oct"));

        ArrayList<BookModel> achievementList = new ArrayList<>();
        achievementList.add(new BookModel("Nook Scout", "Visit 3 unique Nooks"));
        achievementList.add(new BookModel("The Book Fairy", "Donate 5 books"));

        //default view will be journey list
        listView.setAdapter(new BookAdapter(requireContext(), journeyList));

        //when a tab is clicked on
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    listView.setAdapter(new BookAdapter(requireContext(), journeyList));
                } else {
                    listView.setAdapter(new BookAdapter(requireContext(), achievementList));
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void setupStats(View view) {
        View nookStat = view.findViewById(R.id.statNooks);
        ((TextView) nookStat.findViewById(R.id.tvStatNumber)).setText("12");
        ((TextView) nookStat.findViewById(R.id.tvStatLabel)).setText("Nooks");

        View donatedStat = view.findViewById(R.id.statDonated);
        ((TextView) donatedStat.findViewById(R.id.tvStatNumber)).setText("5");
        ((TextView) donatedStat.findViewById(R.id.tvStatLabel)).setText("Donated");

        View foundStat = view.findViewById(R.id.statFound);
        ((TextView) foundStat.findViewById(R.id.tvStatNumber)).setText("8");
        ((TextView) foundStat.findViewById(R.id.tvStatLabel)).setText("Found");
    }
}