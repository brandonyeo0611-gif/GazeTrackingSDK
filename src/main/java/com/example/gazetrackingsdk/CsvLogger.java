package com.example.gazetrackingsdk;

import android.content.Context;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvLogger {
    private CSVWriter csvWriter;

    CsvLogger(Context context, String user) {
        File directory = new File(context.getFilesDir(), "GazeData");
        // save within local file, to add a method that can just retrieve the csv
        if (!directory.exists()) {
            directory.mkdirs(); // Create a dedicated folder
        }
        File file = new File(directory, user + "_logits.csv");
        // fiile name is userID
        try {
            this.csvWriter = new CSVWriter(new FileWriter(file, true));
            String[] header = {"time", "raw logits", "raw confidence", "raw prediction", "raw Mvote prediction", "smoothen logits", "smoothen confidence", "smoothen prediction", "Smoothen Mvote prediction"};
            // rn there is just the Mvote of the smoothen layer and not the raw one because it will need another instance
            if (file.length() == 0) {
                csvWriter.writeNext(header);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static String saveLogits(float[] logit) {
        StringBuilder logitsString = new StringBuilder();
        for (int i = 0; i < logit.length; i++) {
            logitsString.append(logit[i]);
            if (i < logit.length - 1) {
                logitsString.append(";"); // Separator inside the column
            }
        }
        return logitsString.toString();
    }

    void writeNext(String[] s) {
        csvWriter.writeNext(s);
    }

    // when the game is done then call this
    // it saves and close the csv
    void close() {
        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

