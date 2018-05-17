package com.rc.PDFCitationAnalyzer;

/**
 * Created by rafaelcastro on 8/4/17.
 * TwinFile Object. Holds all the attributes that a twin file needs to have.
 */
class TwinFile {
    private int twinID;
    private String paperName;
    private int yearPublished;
    private String authors;

    TwinFile(int twinID, String paperName, int yearPublished, String authors) {
        this.twinID = twinID;
        this.paperName = paperName;
        this.yearPublished = yearPublished;
        this.authors = authors;
    }

    int getTwinID() {
        return twinID;
    }

    String getPaperName() {
        return paperName;
    }

    int getYearPublished() {
        return yearPublished;
    }

    String getAuthors() {
        return authors;
    }
}
