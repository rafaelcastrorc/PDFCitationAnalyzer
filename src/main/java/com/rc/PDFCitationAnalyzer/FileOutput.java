package com.rc.PDFCitationAnalyzer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by rafaelcastro on 5/30/17.
 * Generates an Excel file with the output of the program.
 */
class FileOutput {

    FileOutput() {
    }

    /**
     * Writes the data into a new excel workbook called Report.xlsx
     * @param dataGathered TreeMap with the data gathered
     * @throws IOException if there is a problem creating the file
     */
    void writeOutputToFile(TreeMap<Integer, ArrayList<Object>> dataGathered, String excelName) throws IOException {
        //blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        //blank sheet
        XSSFSheet spreadSheet = workbook.createSheet("Output");
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

        FileOutputStream out = new FileOutputStream(new File("./"+excelName));
        workbook.write(out);
        out.close();
        workbook.close();


    }

    /**
     * Writes the titles that were extracted from PDF files to a new excel workbook called Titles.xlsx
     * @param  titles List Of titles
     * @param excelName Name of the Excel file.
     */
    void writeTitlesToFile(ArrayList<String> titles, String excelName) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook();
        //blank sheet
        XSSFSheet spreadSheet = workbook.createSheet(excelName);
        //Create row object
        XSSFRow row;

        //Iterate over data and write
        int rowId = 0;

        for (String currTitle: titles) {
            row = spreadSheet.createRow(rowId++);
            Cell cell = row.createCell(0);
            cell.setCellValue(currTitle);
        }

        //Write the workbook info in the file system

        FileOutputStream out = new FileOutputStream(new File("./"+excelName));
        workbook.write(out);
        out.close();
    }





}
