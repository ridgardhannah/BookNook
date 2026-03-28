package com.example.mpa_booknookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private ArrayList<BookModel> books;

    public BookAdapter(ArrayList<BookModel> books) {
        this.books = books;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_wishlist, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookModel currentBook = books.get(position);

        holder.tvTitle.setText(currentBook.getTitle());
        holder.tvSubtitle.setText(currentBook.getSubtitle());
        String coverUrl = currentBook.getCoverUrl();

        //load book cover using glide

        if (coverUrl != null && !coverUrl.isEmpty()) {
            //force glide to download fresh ones and forget anything that was cached. seems to have helped
            Glide.with(holder.itemView.getContext())
                    .load(currentBook.getCoverUrl())
                    .placeholder(R.drawable.ic_book) //placeholder image while loading
                    .error(R.drawable.ic_book) //error image if loading fails
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .fitCenter()
                    .placeholder(R.drawable.ic_book)
                    .error(R.drawable.ic_book)
                    .into(holder.ivBookCover);
        } else {
            //placeholder
            holder.ivBookCover.setImageResource(R.drawable.ic_book);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        ImageView ivBookCover;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvSubtitle = itemView.findViewById(R.id.tvBookAuthor);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
        }
    }
}