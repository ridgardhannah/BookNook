package com.example.mpa_booknookapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ConfirmBookActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor;
    private String currentCoverUrl = "";
    private String scannedISBN = "";
    private String scanMode = "";
    private String currentNookId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_book);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //link text to variables
        tvTitle = findViewById(R.id.tvBookTitle);
        tvAuthor = findViewById(R.id.tvBookAuthor);

        //catch the isbn and mode sent in from the barcode scanner
        scannedISBN = getIntent().getStringExtra("ISBN");
        scanMode = getIntent().getStringExtra("MODE"); //DONATE or FIND or WISHLIST
        currentNookId = getIntent().getStringExtra("NOOK_ID");

        //temp text to show loading
        tvTitle.setText("Looking up book...");
        tvAuthor.setText("ISBN: " + scannedISBN);

        //make api request to get book details
        if (scannedISBN != null && !scannedISBN.isEmpty()) { //check if isbn is valid
            fetchBookDetails(scannedISBN);
        } else {
            tvTitle.setText("Error: No ISBN found"); //handle it
        }


        //confirm button->execute database transaction
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            v.setEnabled(false); //cant double click
            ((android.widget.Button) v).setText("Logging transaction...");
            processBookTransaction();
        });

        //scan again button->go back to scanner/manual isbn screen
        findViewById(R.id.btnScanAgain).setOnClickListener(v -> finish());
    }


    //magical google books api stuff
    //give method the isbn to use
    private void fetchBookDetails(String isbn) {

        String cleanIsbn = isbn.trim().replaceAll("[^0-9]", "");

        if (cleanIsbn.equals("1234567890") || cleanIsbn.equals("9780451524935")) {
            tvTitle.setText("1984 (Test Mode)");
            tvAuthor.setText("George Orwell");

            //img for testing
            currentCoverUrl = "https://via.placeholder.com/300x400/0000FF/FFFFFF?text=1984+Cover";


            android.widget.ImageView ivBookCover = findViewById(R.id.ivBookCover);
            com.bumptech.glide.Glide.with(ConfirmBookActivity.this)
                    .load(currentCoverUrl)
                    .placeholder(R.drawable.ic_book)
                    .error(R.drawable.ic_book)
                    .into(ivBookCover);

            return; //stops network request
        }

        //build url using scanned isbn
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + cleanIsbn + "&key=AIzaSyDXzQmwEbQDKwSRUeTysrB2ozMOkPyji0I";

        //create network request. needs to be a GET
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        //check if google found book with the isbn
                        if (response.getInt("totalItems") > 0) {
                            JSONArray items = response.getJSONArray("items");
                            JSONObject volumeInfo = items.getJSONObject(0).getJSONObject("volumeInfo");

                            //get the title and author or first author if theres >1
                            String title = volumeInfo.optString("title", "Unknown Title");
                            String author = "Unknown Author";
                            if (volumeInfo.has("authors")) {
                                author = volumeInfo.getJSONArray("authors").getString(0);
                            }

                            //put real data into ui, instead of the loading text
                            tvTitle.setText(title);
                            tvAuthor.setText(author);

                            if (volumeInfo.has("imageLinks")) {
                                //JSONObject is the class. imageLinks is nickname
                                JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");

                                //use global variable
                                currentCoverUrl = imageLinks.optString("thumbnail", "");

                                //security stuff to get https
                                currentCoverUrl = currentCoverUrl.replace("http://", "https://");

                                //use glide to load img into imageview
                                android.widget.ImageView ivBookCover = findViewById(R.id.ivBookCover);
                                com.bumptech.glide.Glide.with(ConfirmBookActivity.this)
                                        .load(currentCoverUrl)
                                        .placeholder(R.drawable.ic_book)
                                        .error(R.drawable.ic_book)
                                        .into(ivBookCover);
                            }

                        } else { //fallback if details not found
                            tvTitle.setText("This book was not found in our database. Maybe it's a rare one!");
                            tvAuthor.setText("Please enter the book details manually.");

                        }
                    } catch (JSONException e) {
                        tvTitle.setText("Error reading book data");
                        Log.e("API_ERROR", "JSON error", e);
                    }
                },
                error -> {
                    tvTitle.setText("Network error!");
                    Toast.makeText(this, "We were unable to connect to the Books database", Toast.LENGTH_SHORT).show();

                    //bloody bugs
                    //find error code. use logcat to check
                    if (error.networkResponse != null) {
                        Log.e("VOLLEY_ERROR", "HTTP Status Code: " + error.networkResponse.statusCode);
                    } else {
                        Log.e("VOLLEY_ERROR", "Network request failed: " + error.toString());
                    }
                });

        //add request to the Volley queue to be executed
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void processBookTransaction() {
        FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(userId);

        String title = tvTitle.getText().toString();
        String author = tvAuthor.getText().toString();
        String todayDate = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        java.util.Map<String, Object> bookData = new java.util.HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", scannedISBN);
        bookData.put("coverUrl", currentCoverUrl);
        bookData.put("addedAt", com.google.firebase.Timestamp.now());

        //check for wishlist first before donate/find
        if (scanMode.equals("WISHLIST")) {

            //check db for specific isbn to avoid dupes
            userRef.collection("wishlist")
                    .whereEqualTo("isbn", scannedISBN)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {

                        if (!querySnapshot.isEmpty()) {
                            //match was found so dont save it. tell user already in wishlist
                            android.widget.Toast.makeText(this, "This book is already in your Wishlist!", android.widget.Toast.LENGTH_LONG).show();
                            finish(); //back to wishliost screen

                        } else {
                            //no match found. can add to wishlist
                            userRef.collection("wishlist").add(bookData)
                                    .addOnSuccessListener(docRef -> {
                                        finishWithSuccess("'" + title + "' added to your Wishlist!");
                                    })
                                    .addOnFailureListener(e -> android.widget.Toast.makeText(this, "Failed to add to Wishlist: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
                        }
                    })
                    .addOnFailureListener(e -> android.widget.Toast.makeText(this, "Error checking Wishlist: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());

        } else {

            //only look up nook if donating or finding
            com.google.firebase.firestore.DocumentReference nookRef = db.collection("nooks").document(currentNookId);
            com.google.firebase.firestore.CollectionReference inventoryRef = nookRef.collection("inventory");

            if (scanMode.equals("DONATE")) {
                inventoryRef.add(bookData).addOnSuccessListener(docRef -> {
                    nookRef.update("activityLog", com.google.firebase.firestore.FieldValue.arrayUnion("A copy of '" + title + "' was donated!" + " • " + todayDate));

                    userRef.update("donatedCount", com.google.firebase.firestore.FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> checkForAchievements(userRef, "Thanks for donating!"));

                }).addOnFailureListener(e -> Toast.makeText(this, "Failed to donate: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else if (scanMode.equals("FIND")) {
                inventoryRef.whereEqualTo("isbn", scannedISBN).limit(1).get()
                        .addOnSuccessListener(inventoryQuery -> {
                            if (!inventoryQuery.isEmpty()) {
                                String bookId = inventoryQuery.getDocuments().get(0).getId();
                                inventoryRef.document(bookId).delete();
                                nookRef.update("activityLog", com.google.firebase.firestore.FieldValue.arrayUnion("'" + title + "' was taken to a new home!"));

                                userRef.update("foundCount", com.google.firebase.firestore.FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid -> checkForAchievements(userRef, "Thanks for finding a book!"));

                            } else {
                                nookRef.update("activityLog", com.google.firebase.firestore.FieldValue.arrayUnion("A hidden gem ('" + title + "') was discovered and taken!"));

                                userRef.update("foundCount", com.google.firebase.firestore.FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid -> checkForAchievements(userRef, "Thanks for finding a book!"));
                            }
                        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to find book: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void finishWithSuccess(String message) {
        //show success mesage
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        //go back to main map. clear the scanner history
        //close the confirmation screen. user will go back to nook details page
        finish();
    }


    private void checkForAchievements(com.google.firebase.firestore.DocumentReference userRef, String successMessage) {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                long donated = documentSnapshot.getLong("donatedCount") != null ? documentSnapshot.getLong("donatedCount") : 0;
                long found = documentSnapshot.getLong("foundCount") != null ? documentSnapshot.getLong("foundCount") : 0;

                boolean achievementUnlocked = false;

                if (scanMode.equals("DONATE")) {
                    if (donated == 1) {
                        showAchievementDialog("A Novel Beginning!", "You've donated your very first book to a Nook!");
                        achievementUnlocked = true;
                    } else if (donated == 5) {
                        showAchievementDialog("Famous Five!", "You have donated 5 books!");
                        achievementUnlocked = true;
                    }
                } else if (scanMode.equals("FIND")) {
                    if (found == 1) {
                        showAchievementDialog("An Unexpected Journey!", "You found your very first book in a Nook! Enjoy it!");
                        achievementUnlocked = true;
                    } else if (found == 10) {
                        showAchievementDialog("Bookworm!", "10 books found! You are building a great collection!");
                        achievementUnlocked = true;
                    }
                }

                //if here then they didnt get an achievement. close screen normally
                if (!achievementUnlocked) {
                    finishWithSuccess(successMessage);
                }
            }
        });
    }


    private void showAchievementDialog(String title, String description) {
        //builder for the popup
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

        //set the text
        builder.setTitle("🏆 Achievement Unlocked!");
        builder.setMessage(title + "\n\n" + description);

        builder.setPositiveButton("View in Profile", (dialog, which) -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("GO_TO_PROFILE", true);
            intent.putExtra("OPEN_ACHIEVEMENTS", true); //pass the intent extras
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        //button to close/stay here
        builder.setNegativeButton("Okay", (dialog, which) -> {
            dialog.dismiss();
            finish(); //back to nook details
        });

        //show it
        builder.show();
    }
}