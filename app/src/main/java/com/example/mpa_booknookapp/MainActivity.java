package com.example.mpa_booknookapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import android.content.SharedPreferences;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupNotifications();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        //tell nav bar what to do when a button is clicked
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (id == R.id.nav_wishlist) {
                selectedFragment = new WishlistFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();

                if (getIntent().getBooleanExtra("OPEN_ACHIEVEMENTS", false)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("SHOW_ACHIEVEMENTS", true);
                    selectedFragment.setArguments(bundle); //stuffs the note into the fragment
                    getIntent().removeExtra("OPEN_ACHIEVEMENTS"); //deletes original note
                }
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        //check which intent was used to get here
        if (getIntent().getBooleanExtra("GO_TO_PROFILE", false)) {
            //user came from achievement dialogueAchievement Dialog. nav bar selectts profile
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if (savedInstanceState == null) {
            //opened app normaly so force the navbar to select map
            bottomNav.setSelectedItemId(R.id.nav_map);
        }

        //wait for the ui to be ready before showing tutorial overlay
        findViewById(android.R.id.content).post(() -> {
            checkTutorialStatus();
        });
    }

    //runs if main activity is already open in the background and gets a new intent. use with achievement dialog
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent); //updates the activities memory with new intent.

        if (intent.getBooleanExtra("GO_TO_PROFILE", false)) {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_profile);
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
    //for notificatons
    private void setupNotifications() {
        //create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "firebase",
                    "Wishlist Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        //to send message to all users. might be useful in furute for app-wide announcments
        FirebaseMessaging.getInstance().subscribeToTopic("all");
        //to send a notification to just one specific user. e.g a wishlist item found
        getFirebaseCloudMessagingToken();
    }

    private void getFirebaseCloudMessagingToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        android.util.Log.d("FCM Token", "Token: " + token);
                    } else {
                        android.util.Log.e("FCM Token", "Failed to get token");
                    }
                });
    }


    //onboarding tutorial methods
    private void setupOnboarding() {
        //find all the bits
        ViewPager2 viewPager = findViewById(R.id.onboardingViewPager);
        Button btnSkip = findViewById(R.id.btnSkipTutorial);
        Button btnNext = findViewById(R.id.btnNextTutorial);

        //creates tutorial slides
        List<OnboardingItem> slides = new ArrayList<>();
        slides.add(new OnboardingItem(R.drawable.ic_map, "Find Local Nooks", "Discover community bookcases and reading spots in your local area!"));
        slides.add(new OnboardingItem(R.drawable.ic_book, "Donate and Find Books", "Scan their barcodes to donate a book to a Nook or mark them as found."));
        slides.add(new OnboardingItem(R.drawable.ic_wishlist, "Build your Wishlist", "Scan barcodes or search for titles to add books to your wishlist."));
        slides.add(new OnboardingItem(R.drawable.ic_star, "Earn Achievements", "Help the community grow and unlock special reader titles."));

        //give the slides to the adapter
        OnboardingAdapter adapter = new OnboardingAdapter(slides);
        viewPager.setAdapter(adapter);

        //logic for skip button. closes tutorial
        btnSkip.setOnClickListener(v -> finishTutorial());

        //logic for next button. goes to next slide
        btnNext.setOnClickListener(v -> {
            //if more slides go to the next one
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                //if last slide close tutorial
                finishTutorial();
            }
        });

        //change the nexrt button to get started if its the last slide
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });
    }

    private void checkTutorialStatus() {
        SharedPreferences prefs = getSharedPreferences("BookNookPrefs", MODE_PRIVATE);
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean hasSeenTutorial = prefs.getBoolean("hasSeenTutorial_" + userId, false);

        //check if Settings sent a replay command
        boolean replayTutorial = getIntent().getBooleanExtra("REPLAY_TUTORIAL", false);

        View tutorialOverlay = findViewById(R.id.tutorialOverlay);
        if (tutorialOverlay == null) return;

        //if user has not seen it before or  if settings told us to replay it
        if (!hasSeenTutorial || replayTutorial) {

            //delete the replay command so it doesn't get stuck in a loop if they rotate the screen
            getIntent().removeExtra("REPLAY_TUTORIAL");

            tutorialOverlay.setVisibility(View.VISIBLE);
            tutorialOverlay.bringToFront();
            setupOnboarding();
        } else {
            tutorialOverlay.setVisibility(View.GONE);
            triggerAppPermissions();
        }
    }

    private void finishTutorial() {
        findViewById(R.id.tutorialOverlay).setVisibility(View.GONE);

        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences("BookNookPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasSeenTutorial_" + userId, true).apply();

        triggerAppPermissions();
    }


    private void requestLocationPermission() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof MapFragment) {
            ((MapFragment) currentFragment).getUserLocation();
        }
    }

    //ask for notification permissions frst
    private void triggerAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            //notification permission popup. waits for user to respond
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        } else {
            //if done go straight to asking for location
            triggerLocationPermission();
        }
    }

    //take the answer to the permission request when user clicks allow/deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            //now ask for location permission
            triggerLocationPermission();
        }
    }

    //after this we go to the map fragment to get the user location
    private void triggerLocationPermission() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof MapFragment) {
            ((MapFragment) currentFragment).getUserLocation();
        }
    }


}