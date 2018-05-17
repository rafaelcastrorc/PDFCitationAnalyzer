package com.rc.PDFCitationAnalyzer;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by Rafael Castro on 5/15/17.
 * Formats the metadata of any pdf document.
 * Used to retrieve the information from a given document, as well as to correctly format it.
 * Only used for Single Article mode or Twin Article mode because they require manual input.
 */
class FileFormatter {
    static File file;
    private static COSDocument cosDoc;
    private static PDDocument pdDoc;


    /**
     * Sets the file that will be formatted
     *
     * @param file - file to be formatted
     */
    static void setFile(File file) throws IOException {
        FileFormatter.file = file;
        formatFile();
    }

    /**
     * Scans the document with pdfBox to allow modifications.
     */
    private static void formatFile() throws IOException {
        PDFParser parser;
        try {
            parser = new PDFParser(new RandomAccessBufferedFileInputStream(file));
            parser.parse();

            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

        } catch (IOException e) {
            throw new IOException("Could not parse file");
        }
    }

    /**
     * Adds author to a given pdf document
     *
     * @param s - the name of the authors.
     */
    static void addAuthors(String s) {
        PDDocumentInformation currInfo = pdDoc.getDocumentInformation();
        currInfo.setAuthor(s);
        pdDoc.setDocumentInformation(currInfo);
        try {
            pdDoc.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Adds a title to a given pdf document
     *
     * @param s - the title of the document
     */
    static void addTitle(String s) {

        PDDocumentInformation currInfo = pdDoc.getDocumentInformation();
        currInfo.setTitle(s);
        pdDoc.setDocumentInformation(currInfo);
        try {
            pdDoc.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the title and the author of the current document, based on the metadata
     *
     * @return a string with the information
     */
    static String getCurrentInfo() {
        return "The title of the paper: " + pdDoc.getDocumentInformation().getTitle() + "\n" +
                "The authors: " + pdDoc.getDocumentInformation().getAuthor() + "\n" +
                "Published: " + pdDoc.getDocumentInformation().getCreationDate().get(Calendar.YEAR);
    }

    /**
     * Gets the authors of the current document, based on the metadata
     *
     * @return a string with the authors
     */
    static String getAuthors() {
        return pdDoc.getDocumentInformation().getAuthor();
    }

    /**
     * Gets the title of the current document, based on the metadata
     *
     * @return a string with the title
     */
    static String getTitle() {
        return pdDoc.getDocumentInformation().getTitle();
    }

    /**
     * Closes the current file.
     */
    static void closeFile() throws IOException {
        try {
            pdDoc.close();
            cosDoc.close();
        } catch (IOException e) {
            throw new IOException("There was a problem closing the file");
        }

    }


    /**
     * Adds the year to the metadata of the PDF file
     * @param year String that represents the year
     */
    static void addYear(String year) {
        PDDocumentInformation currInfo = pdDoc.getDocumentInformation();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, Integer.valueOf(year));
        currInfo.setCreationDate(calendar);
        pdDoc.setDocumentInformation(currInfo);
        try {
            pdDoc.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the year from the PDF metadata.
     * @return int with the year, if any.
     */
    static int getYear() {
        Calendar cal = pdDoc.getDocumentInformation().getCreationDate();
        return cal.get(Calendar.YEAR);
    }
}
