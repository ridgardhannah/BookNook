package com.example.mpa_booknookapp;


//just author and title for now
public class BookModel {
    private String title;
    private String author;

    public BookModel(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
}