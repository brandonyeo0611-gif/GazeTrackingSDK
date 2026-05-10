package com.example.gazetrackingsdk;
import android.graphics.Bitmap;
import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.nio.ByteBuffer;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

abstract class Orchestrator implements FrameListener {

    private Pair<Double,Double> scalar;
    // to do!
    private final ModelInference modelInference;
    // modelInference is a class that i made
    private final PreprocessLayer preprocessLayer;
    private final Postprocessing postprocessingLayerRaw;
    private final Postprocessing postprocessingLayerSmooth;
    private CsvLogger csvLogger;
    Orchestrator(Context context, String user) {
        this.modelInference = new ModelInference(context);
        this.preprocessLayer = new PreprocessLayer();
        this.postprocessingLayerSmooth = new Postprocessing();
        // using the same postprocessing so there is kinda like a memory
        this.postprocessingLayerRaw = new Postprocessing();

        this.csvLogger = new CsvLogger(context, user);
    }
    // there probably should be a switch like there is game 1 game 2 and calibration
    // ultimately the API will call differently method signature to tell the thing what task it is?
    //

    abstract public void onCapture(Bitmap bitmap);
    // essentially each game extends from the orchestrator layer
    // which make sense to me because each game are the ones that kinda decide
    // what they want to do with the raw image captured
    // so orchestrator layer should just include methods to use

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
