package com.rc.PDFCitationAnalyzer;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by rafaelcastro on 5/30/17.
 */
class FileAnalyzerTest {
    @Test
    void containsCitation() throws IOException {
        FileAnalyzer fileAnalyzer = new FileAnalyzer();
        String authors = "Kerr, Searle";
        String regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        String ans = fileAnalyzer.containsCitation("(Kerr and Searle, 1972a and b; John and Steven 2010)", regex,
                authors, "", false, 1);
        assertTrue(!ans.isEmpty());

        authors = "Kerr, Searle";
        regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        ans = fileAnalyzer.containsCitation("(Kerr and Searle, 1972a and b)", regex, authors, "", false, 1);

        assertTrue(!ans.isEmpty());

        regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        ans = fileAnalyzer.containsCitation("(Fig. 8; Farbman, 1968;\n" +
                "Kerr, 1971, 1972a and b).", regex, authors, "", false, 1);
        assertTrue(!ans.isEmpty());


        authors = "Mitsutomo Abe, Yasushi Kobayashi, Sumiko Yamamoto";
        regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        ans = fileAnalyzer.containsCitation("Abe et al. 2001, Huang et al. 2005, Wigge et al. 2005", regex, authors,
                "", false, 1);
        assertTrue(!ans.isEmpty());

        //When text is in CAPS
        authors = "Jayhong A. Chong, José Tapia-Ramirez, Sandra Kim";
        regex = fileAnalyzer.generateReferenceRegex(authors, false, false);
        String regexInCaps = fileAnalyzer.generateReferenceRegex(authors.toUpperCase(), false, false);
        String finalRegex = "("+regex+")|("+regexInCaps+")";
        ans = fileAnalyzer.containsCitation("CHONG, J.A., J. TAPIA-RAMIREZ, S. KIM, et al.1995.", finalRegex,
                authors, "", true, 1);
        assertTrue(!ans.isEmpty());



    }

    @Test
    void generateReferenceRegex() {
    }

    @Test
    void containsYear() {

        FileAnalyzer fileAnalyzer = new FileAnalyzer();

        String authors = "Kerr, Searle";

        String regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        String ans = fileAnalyzer.containsCitation("(Kerr and Searle, 1972a and b)", regex, authors, "", false, 1);

        assertTrue(fileAnalyzer.containsYear(ans, "1972a" ));
        assertTrue(fileAnalyzer.containsYear(ans, "1972b" ));

        regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        ans = fileAnalyzer.containsCitation("(Fig. 8; Farbman, 1968;\n" +
                "Kerr, 1971, 1972a and b).", regex, authors, "", false, 1);

        assertTrue(fileAnalyzer.containsYear(ans, "1972a" ));
        assertTrue(fileAnalyzer.containsYear(ans, "1972b" ));
        assertTrue(fileAnalyzer.containsYear(ans, "1971" ));


        authors = "Mitsutomo Abe, Yasushi Kobayashi, Sumiko Yamamoto";
        regex = fileAnalyzer.generateReferenceRegex(authors, false, true);
        ans = fileAnalyzer.containsCitation("Abe et al. 2001, Huang et al. 2005, Wigge et al. 2005", regex, authors,
                "", false, 1);
        assertFalse(fileAnalyzer.containsYear(ans, "2005" ));


    }





}