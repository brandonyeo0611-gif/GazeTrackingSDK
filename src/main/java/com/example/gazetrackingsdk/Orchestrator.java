package com.example.gazetrackingsdk;
import android.graphics.Bitmap;
import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

abstract class Orchestrator implements FrameListener {
    private Context context;
    private String userID;

    private Pair<Double,Double> scalar;
    // to do!
    private final ModelInference modelInference;
    // modelInference is a class that i made
    private final PreprocessLayer preprocessLayer;
    private final Postprocessing postprocessingLayerRaw;
    private final Postprocessing postprocessingLayerSmooth;
    private final CalibrationLayer calibrationLayer;
    private CsvLogger csvLogger;
    private AtomicInteger count;
    Orchestrator(Context context, String userID) {
        this.context = context;
        this.userID = userID;
        this.count = new AtomicInteger();
        this.modelInference = new ModelInference(context);
        this.preprocessLayer = new PreprocessLayer();
        this.postprocessingLayerSmooth = new Postprocessing();
        // using the same postprocessing so there is kinda like a memory
        this.postprocessingLayerRaw = new Postprocessing();
        this.calibrationLayer = new CalibrationLayer();
        this.csvLogger = new CsvLogger(context, userID);
    }
    // there probably should be a switch like there is game 1 game 2 and calibration
    // ultimately the API will call differently method signature to tell the thing what task it is?
    //


    abstract public void onCapture(Bitmap bitmap);
    // essentially each game extends from the orchestrator layer
    // which make sense to me because each game are the ones that kinda decide
    // what they want to do with the raw image captured
    // so orchestrator layer should just include methods to use

    public void saveCapture(Bitmap bitmap) {
        try {
            int frame = count.getAndIncrement();
            // should just pass the bitmap to the preprocess layer --> model --> post --> api?
            // maybe just return a bytebuffer then the orchestrator layer / API layer will do it
            File folder = new File(context.getExternalFilesDir(null), "user" + userID);
            // save to the external storage for checking
            // need to ask how it works cause I can't really visualise it
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, "frame_" + frame + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            // sends it to orchestrator and orchestrator will save the initial logits

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            // turns it into images
            // quality attribute 100 is maximum quality
            // these lines of code saves the file in the cached repository WITHIN THE DEVICE for access later
            outputStream.close();
        } catch (java.io.IOException ignored) {
        }
    }
    public ByteBuffer getByteBuffer(Bitmap bitmap) {
        return preprocessLayer.convertBitmapToByteBuffer(bitmap);
    }
    public float[] getInference(ByteBuffer byteBuffer) {
        return modelInference.getInference(byteBuffer);
    }
    void smoothenLogits(float[] logit) {
        float[] smoothenLogits = this.postprocessingLayerSmooth.applySmoothing(logit);
        String smoothenLogitsStr = CsvLogger.saveLogits(smoothenLogits);
        Pair<Float, Integer> ConfnPredSmooth = Postprocessing.ConfnPred(smoothenLogits);
        int Mvote = postprocessingLayerSmooth.majorityVote(ConfnPredSmooth.second);
        String[] resultStr = new String[]{
                String.valueOf(System.currentTimeMillis()),
                "",
                "",
                "",
                "",
                smoothenLogitsStr,
                ConfnPredSmooth.first.toString(),
                ConfnPredSmooth.second.toString(),
                String.valueOf(Mvote)
        };
        csvLogger.writeNext(resultStr);
    }

    void rawLogits(float[] logit) {
        String smoothenLogitsStr = CsvLogger.saveLogits(logit);
        Pair<Float, Integer> ConfnPredRaw = Postprocessing.ConfnPred(logit);
        int Mvote = postprocessingLayerRaw.majorityVote(ConfnPredRaw.second);
        String[] resultStr = new String[]{
                String.valueOf(System.currentTimeMillis()),
                smoothenLogitsStr,
                ConfnPredRaw.first.toString(),
                ConfnPredRaw.second.toString(),
                String.valueOf(Mvote),
                "",
                "",
                "",
                "",
        };
        csvLogger.writeNext(resultStr);
    }

    // for testing probably I suggest we should just use this one
    void smoothAndRawLogits(float[] logit) {
        String rawLogitsStr = CsvLogger.saveLogits(logit);
        Pair<Float, Integer> ConfnPredRaw = Postprocessing.ConfnPred(logit);
        int MvoteRaw = postprocessingLayerRaw.majorityVote(ConfnPredRaw.second);
        float[] smoothenLogits = postprocessingLayerSmooth.applySmoothing(logit);
        String smoothenLogitsStr = CsvLogger.saveLogits(smoothenLogits);
        Pair<Float, Integer> ConfnPredSmooth = Postprocessing.ConfnPred(smoothenLogits);
        int MvoteSmooth = postprocessingLayerSmooth.majorityVote(ConfnPredSmooth.second);
        String[] resultStr = new String[]{
                String.valueOf(System.currentTimeMillis()),
                rawLogitsStr,
                ConfnPredRaw.first.toString(),
                ConfnPredRaw.second.toString(),
                String.valueOf(MvoteRaw),
                smoothenLogitsStr,
                ConfnPredSmooth.first.toString(),
                ConfnPredSmooth.second.toString(),
                String.valueOf(MvoteSmooth) // return Mvote on the smoothened one
        };
        csvLogger.writeNext(resultStr);
    }

    void stop() {
        csvLogger.close();
    }




    // need to flush else the last few frames may be lost
}
