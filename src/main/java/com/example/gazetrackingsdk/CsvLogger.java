package com.example.gazetrackingsdk;

import android.content.Context;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvLogger extends PredictionListener {
    private CSVWriter csvWriter;

    CsvLogger(Context context, String userID) {
        File directory = new File(context.getFilesDir(), "GazeData");
        // save within local file, to add a method that can just retrieve the csv
        if (!directory.exists()) {
            directory.mkdirs(); // Create a dedicated folder
        }
        File file = new File(directory, userID + "_logits.csv");
        try {
            this.csvWriter = new CSVWriter(new FileWriter(file, true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialise(String[] header) {
        csvWriter.writeNext(header);
    }

    public void onPrediction(GazePrediction p) {
    }

    public void onError() {
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

