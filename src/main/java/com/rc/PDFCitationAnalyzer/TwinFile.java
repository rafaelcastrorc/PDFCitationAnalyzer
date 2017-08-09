package com.rc.PDFCitationAnalyzer;

/**
 * Created by rafaelcastro on 8/4/17.
 * TwinFile Object. Holds all the attributes that a twin file needs to have.
 */
public class TwinFile {
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

    public int getTwinID() {
        return twinID;
    }

    public String getPaperName() {
        return paperName;
    }

    public int getYearPublished() {
        return yearPublished;
    }

    public String getAuthors() {
        return authors;
    }
}
