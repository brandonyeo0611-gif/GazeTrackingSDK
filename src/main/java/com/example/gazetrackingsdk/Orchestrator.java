package com.example.gazetrackingsdk;
import android.graphics.Bitmap;
import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.nio.ByteBuffer;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Orchestrator implements FrameListener {
    private final String user;
    private final ModelInference modelInference;
    // modelInference is a class that i made
    private final PreprocessLayer preprocessLayer;
    private final Postprocessing postprocessingLayer;
    private CSVWriter csvWriter;

    public float[] logits;

    Orchestrator(Context context, String user) {
        this.modelInference = new ModelInference(context);
        this.preprocessLayer = new PreprocessLayer();
        this.postprocessingLayer = new Postprocessing();
        //
        // using the same postprocessing so there is kinda like a memory
        this.user = user;
        File directory = new File(context.getFilesDir(), "GazeData");
        // save within local file, to add a method that can just retrieve the csv
        if (!directory.exists()) {
            directory.mkdirs(); // Create a dedicated folder
        }
        File file = new File(directory, user + "_logits.csv");

        try {
            this.csvWriter = new CSVWriter(new FileWriter(file, true));
            String[] header = {"time", "raw logits", "raw confidence", "raw prediction", "smoothen logits", "smoothen confidence", "smoothen prediction", "Smoothen Mvote prediction"};
            // rn there is just the Mvote of the smoothen layer and not the raw one because it will need another instance

            csvWriter.writeNext(header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public void onCapture(Bitmap bitmap) {
        ByteBuffer byteBuffer = preprocessLayer.convertBitmapToByteBuffer(bitmap);
        float[] logits = modelInference.getInference(byteBuffer);
        this.logits = logits;
        String rawLogitsStr = saveLogits(logits);
        Pair<Float, Integer> ConfnPredRaw = Postprocessing.ConfnPred(logits);
        float[] smoothenLogits = postprocessingLayer.applySmoothing(logits);
        String smoothenLogitsStr = saveLogits(smoothenLogits);
        Pair<Float, Integer> ConfnPredSmooth = Postprocessing.ConfnPred(smoothenLogits);
        int Mvote = postprocessingLayer.majorityVote(ConfnPredSmooth.second);
        String[] info = {
                String.valueOf(System.currentTimeMillis()),
                rawLogitsStr,
                ConfnPredRaw.first.toString(),
                ConfnPredRaw.second.toString(),
                smoothenLogitsStr,
                ConfnPredSmooth.first.toString(),
                ConfnPredSmooth.second.toString(),
                String.valueOf(Mvote)
        };
        csvWriter.writeNext(info);



        // do some post-processing
        // send to api to application such that it gives the logits? WRONG
        // is it ok to do everything within this method for example smoothen, Mvote all here
    }

    private static String saveLogits(float[] logit) {
        StringBuilder logitsString = new StringBuilder();
        for (int i = 0; i < logit.length; i++) {
            logitsString.append(logit[i]);
            if (i < logit.length - 1) {
                logitsString.append(";"); // Separator inside the column
            }
        }
        return logitsString.toString();
    }

    public void stopRecording() {
        try {
            if (this.csvWriter != null) {
                this.csvWriter.flush();
                this.csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // need to flush else the last few frames may be lost
}
