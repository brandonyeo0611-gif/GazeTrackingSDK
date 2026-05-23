package com.example.gazetrackingsdk;

import java.util.ArrayList;
import java.util.List;

public class GazePrediction {
    protected float[] logitRaw;
    protected GazeClass rawClass;
    protected float confidenceRaw;
    protected float[] logitSmooth;
    protected GazeClass smoothClass;
    protected float confidenceSmooth;
    protected GazeClass MVote;

    public GazePrediction(float[] logitRaw, GazeClass rawClass, float confidenceRaw, float[] logitSmooth, GazeClass smoothClass, float confidenceSmooth, GazeClass mVote) {
        this.logitRaw = logitRaw;
        this.rawClass = rawClass;
        this.confidenceRaw = confidenceRaw;
        this.logitSmooth = logitSmooth;
        this.smoothClass = smoothClass;
        this.confidenceSmooth = confidenceSmooth;
        this.MVote = mVote;
    }
    public float[] getLogitRaw() {
        return logitRaw;
    }
    public GazeClass getRawClass() {
        return rawClass;
    }

    public float getConfidenceRaw() {
        return confidenceRaw;
    }

    public float[] getLogitSmooth() {
        return logitSmooth;
    }

    public GazeClass getSmoothClass() {
        return smoothClass;
    }

    public float getConfidenceSmooth() {
        return confidenceSmooth;
    }

    public GazeClass getMVote() {
        return MVote;
    }

    protected List<Object> getAll() {
        ArrayList<Object> values = new ArrayList<>();
        values.add(logitRaw);
        values.add(rawClass);
        values.add(confidenceRaw);
        values.add(logitSmooth);
        values.add(smoothClass);
        values.add(confidenceSmooth);
        values.add(MVote);
        return values;
    }
}
