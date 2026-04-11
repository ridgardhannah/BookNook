package com.example.mpa_booknookapp;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookSearchActivity extends AppCompatActivity {

    private RecyclerView rvSearchResults;
    private ProgressBar progressSearch;
    private ArrayList<BookModel> searchResults;
    private BookSearchAdapter adapter;
    private String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_search);

        //get search term passed from wishlist fragment
        searchQuery = getIntent().getStringExtra("QUERY");

        //set up the ui
        TextView tvQuery = findViewById(R.id.tvSearchQuery);
        tvQuery.setText("Searching for '" + searchQuery + "'...");
        findViewById(R.id.btnBackBookSearch).setOnClickListener(v -> finish());

        progressSearch = findViewById(R.id.progressSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));

        searchResults = new ArrayList<>();

        //setup adapter with clicker to save to firebase
        adapter = new BookSearchAdapter(searchResults, book -> saveToWishlist(book));
        rvSearchResults.setAdapter(adapter);

        //do the search
        searchGoogleBooksAPI(searchQuery);
    }

    private void searchGoogleBooksAPI(String query) {
        // internet search has to be a backgroud thread. android rule
        new Thread(() -> {
            try {
                //format url for googlebooks
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://www.googleapis.com/books/v1/volumes?q=" + encodedQuery + "&maxResults=15");

                //open connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                //read the datta
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                //parse JSON response
                JSONObject jsonObject = new JSONObject(response.toString());
                if (jsonObject.has("items")) {
                    JSONArray items = jsonObject.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject volumeInfo = items.getJSONObject(i).getJSONObject("volumeInfo");

                        String title = volumeInfo.optString("title", "Unknown Title");

                        String author = "Unknown Author";
                        if (volumeInfo.has("authors")) {
                            author = volumeInfo.getJSONArray("authors").getString(0);
                        }

                        String coverUrl = "";
                        if (volumeInfo.has("imageLinks")) {
                            coverUrl = volumeInfo.getJSONObject("imageLinks").optString("thumbnail", "");
                            coverUrl = coverUrl.replace("http://", "https://");
                        }

                        //get isbn too for wishlist to match scanned books in db
                        String isbn = "";
                        if (volumeInfo.has("industryIdentifiers")) {
                            JSONArray identifiers = volumeInfo.getJSONArray("industryIdentifiers");
                            for (int j = 0; j < identifiers.length(); j++) {
                                JSONObject idObj = identifiers.getJSONObject(j);
                                String type = idObj.optString("type");

                                //isbn13 preferable. isbn10 fine if needed
                                if (type.equals("ISBN_13")) {
                                    isbn = idObj.optString("identifier");
                                    break; //found 13. dont need to look for 10
                                } else if (type.equals("ISBN_10")) {
                                    isbn = idObj.optString("identifier");
                                }
                            }
                        }

                        //add to list of search resultsn using big constructor
                        searchResults.add(new BookModel(title, author, coverUrl, isbn));
                    }
                }

                // update ui back on main thread
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();

                    if (searchResults.isEmpty()) {
                        Toast.makeText(this, "No books found!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    Toast.makeText(this, "Error connecting to server.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveToWishlist(BookModel book) {
        Toast.makeText(this, "Trying to save: " + book.getTitle(), Toast.LENGTH_SHORT).show();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Error: You are not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //package book data
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("title", book.getTitle());
        bookData.put("author", book.getSubtitle());
        bookData.put("coverUrl", book.getCoverUrl());
        bookData.put("isbn", book.getIsbn());
        bookData.put("addedAt", com.google.firebase.Timestamp.now());

        //check for dupes by title first to prevent spamming
        db.collection("users").document(userId).collection("wishlist")
                .whereEqualTo("title", book.getTitle())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Already in your Wishlist!", Toast.LENGTH_SHORT).show();
                    } else {
                        //save it
                        db.collection("users").document(userId).collection("wishlist")
                                .add(bookData)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(this, "Added to Wishlist!", Toast.LENGTH_SHORT).show();
                                    finish(); //close search screen. back to wishlist
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show());
                    }
                });

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
}