package com.example.mpa_booknookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

//class to manage data and views for the onboarding screen
//extends recyclerview adapter to recycle the ui bits as user swipes
//uses viewpager2 slides to run smoothly
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    //list to hold data for each slide
    private List<OnboardingItem> onboardingItems;

    //constructor. initialises adapter with list of slides
    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    //called when the recyclerview needs a new viewholder to represent an item
    //inflates item_onboarding.xml into an object
    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false)
        );
    }

    //called by the recyclerview to display the data in the right places
    //updates the viewholder with the correct data for the slide at the given position
    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    //tells adapter how many slides exist so it knows when the last one is to stop more swiping
    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    //implements viewholder pattern
    //catches ui bits
    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textDescription;
        private ImageView imageIcon;

        //constructor finds the ui bits from the xml and stores them in the object
        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            textDescription = itemView.findViewById(R.id.tvOnboardingDescription);
            imageIcon = itemView.findViewById(R.id.ivOnboardingGraphic);
        }

        //method maps data from object to the ui bits
        void bind(OnboardingItem onboardingItem) {
            textTitle.setText(onboardingItem.getTitle());
            textDescription.setText(onboardingItem.getDescription());
            imageIcon.setImageResource(onboardingItem.getImageResId());
        }
    }
}