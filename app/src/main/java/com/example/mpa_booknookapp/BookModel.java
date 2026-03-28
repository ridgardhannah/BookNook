package com.example.mpa_booknookapp;

public class BookModel {
    private String title;
    private String subtitle; //author
    private String coverUrl;
    private String isbn; //for wishlist and notifications


    //og constructor. use for setup and dummy tests
    public BookModel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        this.coverUrl = null; //allowed to be null
        this.isbn = null;
    }

    //constructor for dislaying in nook inventory and the wishlists.
    //data from firebase -> app draws recyclerview.
    //grabs text and cover image. doesnt need isbn

    public BookModel(String title, String subtitle, String coverUrl) {
        this.title = title;
        this.subtitle = subtitle;
        this.coverUrl = coverUrl;
        this.isbn = null;
    }


    //for bookSearchActivity. google books api tet search.
    //all details grabbed from firebase. when user saves to wishlist, isbn can get saved too like when a book is scanned
    public BookModel(String title, String subtitle, String coverUrl, String isbn) {
        this.title = title;
        this.subtitle = subtitle;
        this.coverUrl = coverUrl;
        this.isbn = isbn;
    }


    //getters
    //for app to read the data
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getCoverUrl() { return coverUrl; }
    public String getIsbn() {return isbn; }

    //logic for deletions
    private String documentId; //uuid

    public String getDocumentId() { return documentId; }

    public void setDocumentId(String documentId) { this.documentId = documentId; }

}