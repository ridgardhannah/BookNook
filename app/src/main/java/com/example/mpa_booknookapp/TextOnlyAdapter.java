package com.example.mpa_booknookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

//class to display text lists in recyclerview in activity feed
public class TextOnlyAdapter extends RecyclerView.Adapter<TextOnlyAdapter.ViewHolder> {

    //data source. list of strings. each an activity log
    private List<String> textItems;

    //constructor to receive the list of strings
    //activity strings from firebase
    public TextOnlyAdapter(List<String> textItems) {
        this.textItems = textItems;
    }

    //when a new one is needed to display a new item
    //inflates xml layout into an object
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the text layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_activity, parent, false);
        return new ViewHolder(view);
    }

    //to bind the data to a recyclerview
    //takes string at current scroll position and puts it into the textview
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //maps the string into the textview
        holder.textView.setText(textItems.get(position));
    }

    //returns total num of items in the dataset
    //used by recyclerview to calculate size of scrollbar
    @Override
    public int getItemCount() {
        return textItems.size();
    }

    //holds the view so it doesnt have to find it every time you scroll
    //makes it smoothy smooth
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        //constructor
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvActivityText);
        }
    }
}