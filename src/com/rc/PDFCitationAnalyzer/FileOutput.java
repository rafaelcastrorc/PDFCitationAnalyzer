package com.rc.PDFCitationAnalyzer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by rafaelcastro on 5/30/17.
 * Generates an Excel file with the output of the program
 */
public class FileOutput {

    private XSSFSheet mySheet;
    private XSSFWorkbook myWorkBook;

    /**
     * Writes the data into a new excel workbook called Report.xlsx
     * @param dataGathered TreeMap with the data gathered
     * @throws IOException if there is a problem creating the file
     */
    void writeToFile(TreeMap<Integer, ArrayList<Object>> dataGathered) throws IOException {
        //blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        //blank sheet
        XSSFSheet spreadSheet = workbook.createSheet("Report.xslx");
        //Create row object
        XSSFRow row;

        //Iterate over data and write
        int rowid = 0;

        for (int id : dataGathered.keySet()) {
            row = spreadSheet.createRow(rowid++);
            List list = dataGathered.get(id);
            int cellId = 0;
            for (Object obj : list) {
                Cell cell = row.createCell(cellId++);
                if (obj instanceof String) {
                    cell.setCellValue((String) obj);
                } else if (obj instanceof Double) {
                    cell.setCellValue((Double) obj);
                } else if (obj instanceof Integer) {
                    cell.setCellValue((Integer) obj);
                }
            }
        }

        //Write the workbook info in the file system
        FileOutputStream out = new FileOutputStream(new File("Report.xlsx"));
        workbook.write(out);
        out.close();
        System.out.println("Done");


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
        myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        mySheet = myWorkBook.getSheetAt(0);
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


}
