package com.rc.PDFCitationAnalyzer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by rafaelcastro on 7/20/17.
 *
 */
public class MultipleFilesSetup {
    HashMap<ArrayList<String>, ArrayList<String>> twinPapersToCitingArticles = new HashMap<>();
    Hashtable<String, String> paperToAuthor = new Hashtable<>();


    Hashtable<String, String> paperToYear = new Hashtable<>();
    private Controller controller;


    MultipleFilesSetup(Controller controller) {
        this.controller = controller;
        controller.getOutputPanel().getChildren().clear();

    }

    //First column is title1, second column is title2, third column

    void setupTitleList(File file) {
        try {
            Scanner scanner = new Scanner(new FileInputStream(file));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] array = line.split(",");
                ArrayList<String> twinPapers = new ArrayList<>();
                twinPapers.add(array[0]);
                twinPapers.add(array[1]);
                if (!twinPapersToCitingArticles.containsKey(twinPapers)) {
                    ArrayList<String> citingPapers = new ArrayList<>();
                    citingPapers.add(array[2]);
                    twinPapersToCitingArticles.put(twinPapers, citingPapers);
                }
                else {
                    ArrayList<String> citingPapers = twinPapersToCitingArticles.get(twinPapers);
                    citingPapers.add(array[2]);
                    twinPapersToCitingArticles.put(twinPapers, citingPapers);
                }
                addAuthors(array[0], array[3]);
                addAuthors(array[1], array[4]);
                addYear(array[0], array[99]);
                addYear(array[1], array[99]);


            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void addAuthors(String paper, String author) {
        if (!paperToAuthor.contains(paper)) {
            paperToAuthor.put(paper, author);
        }
    }

    private void addYear(String paper, String year) {
        if (!paperToAuthor.contains(paper)) {
            paperToAuthor.put(paper, year);
        }
    }


    public HashMap<ArrayList<String>, ArrayList<String>> getTwinPapersToCitingArticles() {
        return twinPapersToCitingArticles;
    }

    public Hashtable<String, String> getPaperToAuthor() {
        return paperToAuthor;
    }

    public Hashtable<String, String> getPaperToYear() {
        return paperToYear;
    }


    /**
     * Reads the information inside of report.xlsx
     *
     * @throws IOException if it is unable to access the file
     */
    void readFile() throws IOException {

        File myFile = new File("Report.xlsx");
        FileInputStream fis = new FileInputStream(myFile);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();
            System.out.println();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                if (cell.getCellTypeEnum() == CellType.STRING) {
                    System.out.print(cell.getStringCellValue() + "\t");

                } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                    System.out.print(cell.getNumericCellValue() + "\t");

                } else if (cell.getCellTypeEnum() == CellType.BOOLEAN) {
                    System.out.print(cell.getBooleanCellValue() + "\t");

                }

            }

        }
        System.out.println();

    }

    private void finish() {
        controller.getOutputPanel().getChildren().clear();
        controller.updateStatus("Files have been set.");
        controller.getSetFolderButton().setDisable(false);
        }


   void setReportAndFolder(File report, File folder) {
       try {
           Scanner scanner = new Scanner(report);
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       }

   }
}
