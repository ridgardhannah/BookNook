package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WishlistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WishlistFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public WishlistFragment() {
        //need empty public constructor here
    }

    public static WishlistFragment newInstance(String param1, String param2) {
        WishlistFragment fragment = new WishlistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishlist, container, false);

        ListView listView = view.findViewById(R.id.lvWishlist);

        //fake data for now
        ArrayList<BookModel> bookList = new ArrayList<>();
        bookList.add(new BookModel("The Hobbit", "J.R.R. Tolkien"));
        bookList.add(new BookModel("1984", "George Orwell"));
        bookList.add(new BookModel("The Great Gatsby", "F. Scott Fitzgerald"));
        bookList.add(new BookModel("Harry Potter", "J.K. Rowling"));

        //set the adapter
        BookAdapter adapter = new BookAdapter(getContext(), bookList);
        listView.setAdapter(adapter);

        //logic to open scanner when barcode icon pressed
        view.findViewById(R.id.btnScanWishlist).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            startActivity(intent);
        });
        return view;
    }
}