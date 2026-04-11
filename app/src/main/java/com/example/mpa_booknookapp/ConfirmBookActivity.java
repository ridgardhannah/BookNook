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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


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
        DocumentReference userRef = db.collection("users").document(userId);

        String title = tvTitle.getText().toString();
        String author = tvAuthor.getText().toString();
        String todayDate = new SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault()).format(new Date());
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("isbn", scannedISBN);
        bookData.put("coverUrl", currentCoverUrl);
        bookData.put("addedAt", Timestamp.now());


        //check for wishlist first before donate/find
        if (scanMode.equals("WISHLIST")) {

            //hunt for the isbn for this book and user. to check for dupes
            userRef.collection("wishlist").whereEqualTo("isbn", scannedISBN).get().addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    //found repeat. dont add to wishlist
                    Toast.makeText(this, "This book is already in your Wishlist!", Toast.LENGTH_LONG).show();
                    finish(); //kill screen
                } else {
                    //add to wishlist as new book document
                    userRef.collection("wishlist").add(bookData).addOnSuccessListener(docRef -> {

                        //wait for the increment to finish then check for achievements
                        userRef.update("wishlistAddedCount", com.google.firebase.firestore.FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> {
                                    checkForAchievements(userRef, "'" + title + "' added to your Wishlist!");
                                });
                    });
                }
            });
            return; //end of wishlist mode leave method now

        } else {
            //only look up the  nook if donating or finding
            //go into nooks and find the one with the matching nookid
            DocumentReference nookRef = db.collection("nooks").document(currentNookId);
            //within that nook, get the inventory of books in there
            CollectionReference inventoryRef = nookRef.collection("inventory");

            //get user data now
            userRef.get().addOnSuccessListener(userDoc -> {

                //is it a new nook?
                List<String> visitedNooks = (List<String>) userDoc.get("visitedNookIds");
                if (visitedNooks == null) visitedNooks = new ArrayList<>();
                boolean isNewNook = !visitedNooks.contains(currentNookId);

                //put all user updates into one big update box to deal with at once
                Map<String, Object> userUpdates = new HashMap<>();
                List<String> newLogs = new ArrayList<>(); //hold logs

                //shove updates in box ready to roll
                if (scanMode.equals("DONATE")) {
                    userUpdates.put("donatedCount", com.google.firebase.firestore.FieldValue.increment(1));
                    newLogs.add("You donated '" + title + "'! • " + todayDate);
                } else if (scanMode.equals("FIND")) {
                    userUpdates.put("foundCount", com.google.firebase.firestore.FieldValue.increment(1));
                    newLogs.add("You found '" + title + "'! • " + todayDate);
                }

                if (isNewNook) {
                    userUpdates.put("visitedNookIds", com.google.firebase.firestore.FieldValue.arrayUnion(currentNookId));
                    userUpdates.put("nooksCount", com.google.firebase.firestore.FieldValue.increment(1));
                    newLogs.add("You explored a new Nook! • " + todayDate);
                }

                //put all the logs into the box at the same time
                if (!newLogs.isEmpty()) {
                    userUpdates.put("recentActivity", com.google.firebase.firestore.FieldValue.arrayUnion(newLogs.toArray()));
                }


                //now actually make the updates happen, depending on if its donate or fied
                if (scanMode.equals("DONATE")) {
                    inventoryRef.add(bookData).addOnSuccessListener(docRef -> {
                        nookRef.update("activityLog", FieldValue.arrayUnion("A copy of '" + title + "' was donated! • " + todayDate));

                        //send update box to db
                        userRef.update(userUpdates).addOnSuccessListener(aVoid -> {
                            checkForAchievements(userRef, "Thanks for donating!");
                        });
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to donate", Toast.LENGTH_SHORT).show());

                } else if (scanMode.equals("FIND")) {

                    //look at wishlist
                    //before the find action is done, check if it was on the wishlist
                    userRef.collection("wishlist").whereEqualTo("isbn", scannedISBN).get().addOnSuccessListener(wishlistQuery -> {
                        if (!wishlistQuery.isEmpty()) {
                            //found a match
                            String wishlistDocId = wishlistQuery.getDocuments().get(0).getId();

                            //delete it from wishlist
                            userRef.collection("wishlist").document(wishlistDocId).delete();

                            //add the lifetime wishlist counter to the update box
                            userUpdates.put("wishlistFoundCount", FieldValue.increment(1));

                            //add a log to the activity feed
                            userUpdates.put("recentActivity", FieldValue.arrayUnion("Wish granted! You found '" + title + "'! • " + todayDate));
                        }

                        //continue find logic
                        inventoryRef.whereEqualTo("isbn", scannedISBN).limit(1).get().addOnSuccessListener(inventoryQuery -> {
                            if (!inventoryQuery.isEmpty()) {
                                String bookId = inventoryQuery.getDocuments().get(0).getId();
                                inventoryRef.document(bookId).delete();
                                nookRef.update("activityLog", FieldValue.arrayUnion("'" + title + "' was taken to a new home! • " + todayDate));
                            } else {
                                nookRef.update("activityLog", FieldValue.arrayUnion("A hidden gem ('" + title + "') was discovered! • " + todayDate));
                            }

                            //send update box to db (now carrying wishlist updates if there were any!)
                            userRef.update(userUpdates).addOnSuccessListener(aVoid -> {
                                checkForAchievements(userRef, "Thanks for finding a book!");
                            });
                        });
                    });
                }
            });
        }
    }
        private void finishWithSuccess (String message){
            //show success mesage
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            //go back to main map. clear the scanner history
            //close the confirmation screen. user will go back to nook details page
            finish();
        }




    private void checkForAchievements(com.google.firebase.firestore.DocumentReference userRef, String successMessage) {
        //asynchronous data retrievel bit
        //ask firestore for the latest user data after the increment is successful
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            //check user document exists first
            if (documentSnapshot.exists()) {


                //pull data from the document
                //use ternary operators as a safety net. default to 0 if it doesnt exist yet
                long donated = documentSnapshot.getLong("donatedCount") != null ? documentSnapshot.getLong("donatedCount") : 0;
                long found = documentSnapshot.getLong("foundCount") != null ? documentSnapshot.getLong("foundCount") : 0;
                long nooks = documentSnapshot.getLong("nooksCount") != null ? documentSnapshot.getLong("nooksCount") : 0;
                long wishlistAdded = documentSnapshot.getLong("wishlistAddedCount") != null ? documentSnapshot.getLong("wishlistAddedCount") : 0;
                long wishlistFound = documentSnapshot.getLong("wishlistFoundCount") != null ? documentSnapshot.getLong("wishlistFoundCount") : 0;

                //check finding achievement thresholds
                if (scanMode.equals("FIND")) {
                    if (found == 5) { showAchievementDialog("Reader", "You found 5 books!"); return; }
                    if (found == 10) { showAchievementDialog("Bookworm", "You found 10 books!"); return; }
                    if (found == 30) { showAchievementDialog("Archivist", "You found 30 books!"); return; }
                    if (found == 60) { showAchievementDialog("Book Keeper", "You found 60 books!"); return; }

                    if (wishlistFound == 1) { showAchievementDialog("Serendipity", "You found your very first Wishlist book!"); return; }
                    if (wishlistFound == 5) { showAchievementDialog("Prophecy", "You found 5 Wishlist books!"); return; }
                }

                    //check donating achievement thresholds
                  if (scanMode.equals("DONATE")) {
                    if (donated == 5) { showAchievementDialog("Giver", "You donated 5 books!"); return; }
                    if (donated == 10) { showAchievementDialog("Benefactor", "You donated 10 books!"); return; }
                    if (donated == 20) { showAchievementDialog("Patron", "You donated 20 books!"); return; }
                }

                //check wishlist adding achievement thresholds
                if (scanMode.equals("WISHLIST")) {
                    if (wishlistAdded == 1) { showAchievementDialog("Dreamer", "You added your first book to your Wishlist!"); return; }
                    if (wishlistAdded == 10) { showAchievementDialog("Magpie", "You added 10 books to your Wishlist!"); return; }
                    if (wishlistAdded == 20) { showAchievementDialog("Curator", "You added 20 books to your Wishlist!"); return; }
                }

                //check exploring new nooks threshold - donating or finding
                if (scanMode.equals("DONATE") || scanMode.equals("FIND")) {
                    if (nooks == 5) { showAchievementDialog("Scout", "You explored 5 unique Nooks!"); return; }
                    if (nooks == 10) { showAchievementDialog("Explorer", "You explored 10 unique Nooks!"); return; }
                    if (nooks == 20) { showAchievementDialog("Pathfinder", "You explored 20 unique Nooks!"); return; }
                }
                //if we get to here its just standard completion. no new thresholds crossed
                //dismiss loading screen. show success
                finishWithSuccess(successMessage);
            }
        }).addOnFailureListener(e -> {
            //problem escape
            //if user has internet issue or firebase freaks out this catches the error
            //stops being stuck on the logging transaction bit
            finishWithSuccess(successMessage);
        });
    }


        private void showAchievementDialog (String title, String description){
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
