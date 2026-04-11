package com.example.mpa_booknookapp;


//pojo class to hold data for each slide of the onboarding screen
public class OnboardingItem {
    private int imageResId;  //stores resource id of the image
    private String title; //main heading text
    private String description;  //body text

    public OnboardingItem(int imageResId, String title, String description) {
        this.imageResId = imageResId;
        this.title = title;
        this.description = description;
    }

    //getters so the adapter can read the data
    //onboardingadapter calls them to map data to ui elemets
    public int getImageResId() { return imageResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}