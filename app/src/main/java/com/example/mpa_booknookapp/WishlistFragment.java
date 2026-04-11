package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class WishlistFragment extends Fragment {

    private RecyclerView listView;
    private BookAdapter adapter;
    private ArrayList<BookModel> bookList;

    //for when no books are in wishlist
    private View emptyStateLayout;


    public WishlistFragment() {
        //empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishlist, container, false);

        //set up empty list and reclyer view
        bookList = new ArrayList<>();
        listView = view.findViewById(R.id.lvWishlist);
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        emptyStateLayout = view.findViewById(R.id.layoutEmptyState);

        adapter = new BookAdapter(bookList);
        listView.setAdapter(adapter);

        //logic for swipey deletes
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; //to avoid drag-and-drop up and down. do not delete this line again!
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                //find which row user swiped
                int position = viewHolder.getAdapterPosition();
                BookModel swipedBook = bookList.get(position);

                //delete the document id from firebase with that book
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("wishlist").document(swipedBook.getDocumentId())
                        .delete()
                        .addOnSuccessListener(aVoid -> { //it worked yay
                            Toast.makeText(getContext(), swipedBook.getTitle() + " removed!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> { //it didnt work
                            Toast.makeText(getContext(), "Failed to delete.", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(position); //bounce item back
                        });
            }
        };

        //attach swipe logic to list
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(listView);


        //get data from firebase
        loadWishlistFromFirebase();

        //open settigns when user taps gear icon
        ImageView btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        //open scanner for wishlist
        view.findViewById(R.id.btnScanWishlist).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            //tell scanner its now wishlist mode
            intent.putExtra("MODE", "WISHLIST");
            startActivity(intent);
        });

        //find search bar stuff
        EditText etSearch = view.findViewById(R.id.etWishlistSearch);
        ImageView btnSearch = view.findViewById(R.id.btnTextSearchWishlist);

        //listen for search icon click
        btnSearch.setOnClickListener(v -> performTextSearch(etSearch.getText().toString()));

        //listen for keyboard search
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performTextSearch(etSearch.getText().toString());
                return true; //android knows we handled click
            }
            return false;
        });

        return view;
    }

    private void loadWishlistFromFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //listen towishlist in real time
        db.collection("users").document(userId).collection("wishlist")
                //newest to oldest
                .orderBy("addedAt", Query.Direction.DESCENDING)
                //to pull refreshed list when needed
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Failed to load wishlist", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        bookList.clear(); //empty old list

                        //loop thrugh all the books in the firebase
                        for (QueryDocumentSnapshot doc : value) {
                            String title = doc.getString("title");
                            String author = doc.getString("author");
                            String coverUrl = doc.getString("coverUrl");

                            //add each to the list in steps
                            if (title != null && author != null) {

                                //create the booknusing the model
                                BookModel book = new BookModel(title, author, coverUrl);

                                //attach the id
                                book.setDocumentId(doc.getId());

                                //bob it in the list
                                bookList.add(book);
                            }
                        }

                        //tell adapter data has changed so it refreshes the screen
                        adapter.notifyDataSetChanged();
                        if (bookList.isEmpty()) {
                            listView.setVisibility(View.GONE);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                        } else {
                            listView.setVisibility(View.VISIBLE);
                            emptyStateLayout.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void performTextSearch(String query) {
        if (query.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please enter a book or author", Toast.LENGTH_SHORT).show();
            return;
        }

        //send query to search results screen
        Intent intent = new Intent(getActivity(), BookSearchActivity.class);
        intent.putExtra("QUERY", query.trim());
        startActivity(intent);

        //clear the search bar so its empty when user comes back to this screen
        android.widget.EditText etSearch = getView().findViewById(R.id.etWishlistSearch);
        if (etSearch != null) etSearch.setText("");
    }


}