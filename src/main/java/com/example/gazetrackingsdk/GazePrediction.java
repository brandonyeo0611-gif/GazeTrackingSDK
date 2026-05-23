package com.example.gazetrackingsdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GazePrediction {
    private float[] logitRaw;
    private GazeClass rawClass;
    private final float confidenceRaw;
    private final Optional<float[]> logitSmooth;
    private final Optional<GazeClass> smoothClass;
    private final Optional<Float> confidenceSmooth;
    private final Optional<GazeClass> MVote;

    public GazePrediction(float[] logitRaw, GazeClass rawClass, float confidenceRaw, Optional<float[]> logitSmooth, Optional<GazeClass> smoothClass, Optional<Float> confidenceSmooth, Optional<GazeClass> mVote) {
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

    public Optional<float[]> getLogitSmooth() {
        return logitSmooth;
    }

    public Optional<GazeClass> getSmoothClass() {
        return smoothClass;
    }

    public Optional<Float> getConfidenceSmooth() {
        return confidenceSmooth;
    }

    public Optional<GazeClass> getMVote() {
        return MVote;
    }

    protected ArrayList<Object> getAll() {
        return new ArrayList<Object>(List.<Object>of(logitRaw, rawClass, confidenceRaw, logitSmooth, smoothClass, confidenceSmooth, MVote));
    }
}
