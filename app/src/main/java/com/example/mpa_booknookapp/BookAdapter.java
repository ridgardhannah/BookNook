package com.example.mpa_booknookapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class BookAdapter extends ArrayAdapter<BookModel> {
    public BookAdapter(Context context, ArrayList<BookModel> books) {
        super(context, 0, books);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_book_wishlist, parent, false);
        }

        BookModel currentBook = getItem(position);

        TextView title = convertView.findViewById(R.id.tvBookTitle);
        TextView author = convertView.findViewById(R.id.tvBookAuthor);

        title.setText(currentBook.getTitle());
        author.setText(currentBook.getAuthor());

        return convertView;
    }
}