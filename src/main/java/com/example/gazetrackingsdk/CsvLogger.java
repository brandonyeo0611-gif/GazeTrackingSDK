package com.example.gazetrackingsdk;

import android.content.Context;
import android.util.Pair;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CsvLogger {
    private CSVWriter csvWriter;
    private File file;

    CsvLogger(Context context, String userID, Mode mode) {

        File directory = new File(context.getFilesDir(), "GazeData");
        // save within local file, to add   method that can just retrieve the csv
        if (!directory.exists()) {
            directory.mkdirs(); // Create a dedicated folder
        }
        switch (mode) {
            case CALIBRATION : {
                this.file = new File(directory, userID + "_calibration_logits.csv");
                break;
            }
            case PREDICTION : {
                this.file = new File(directory, userID + "_inference_logits.csv");
                break;
            }
        };


        String[] defaultString = new String[] {"Time",
                "Raw_logits",
                "Raw_predication",
                "Raw_confidence",
                "Smoothen_logits",
                "Smoothen_prediction",
                "Smoothen_confidence",
                "Majorite_vote",
                "True_label"
        };

        // all CSV initialising the same way, if the content is unavailable ( AKA null ), add a "" to substitute

        try {
            this.csvWriter = new CSVWriter(new FileWriter(file, true));
            csvWriter.writeNext(defaultString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPrediction(float[] rawLogit, GazeClass trueLabel) {
        // for calibration -- assuming calibration only uses raw logits, so no smooth ones taken note
        String logitToStr = saveLogits(rawLogit);
        Pair<Float, Integer> pred = PostprocessingLayer.ConfnPred(rawLogit);
        GazeClass gazeClass = GazeClass.values()[pred.second];
        long time = System.currentTimeMillis();
        List<String> base = new ArrayList<String>(List.<String>of(String.valueOf(time),
                logitToStr,
                gazeClass.toString(),
                pred.first.toString(),
                "",
                "",
                "",
                ""
        ));
        base.add(trueLabel.name());
        csvWriter.writeNext(base.toArray(new String[0]));
    }
    public void onPrediction(GazePrediction p) {
        long time = System.currentTimeMillis();
        List<String> base = new ArrayList<String>(List.<String>of(String.valueOf(time)));
        ArrayList<Object> content = p.getAll();
        for (Object o : content) {
            // since prediction now has optional fields
            Object value = (o instanceof Optional<?> opt) ? opt.orElse(null) : o;

            if (value == null) {
                base.add("");
            } else {
                if (value instanceof float[] floats) {
                    String logitToStr = saveLogits(floats);
                    base.add(logitToStr);
                } else if (value instanceof Float f) {
                    String confidence = String.valueOf(f);
                    base.add(confidence);
                } else if (value instanceof GazeClass gazeClass) {
                    base.add(gazeClass.name());
                }
            }
        }
        base.add(""); // to substitute for the calibration column
        csvWriter.writeNext(base.toArray(new String[0]));
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

    public File getFile() {
        return file;
    }
}

