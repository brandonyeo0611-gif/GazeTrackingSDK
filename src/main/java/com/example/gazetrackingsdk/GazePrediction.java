package com.example.gazetrackingsdk;

public class GazePrediction {
    private float[] logitRaw;
    private GazeClass rawClass;
    private float confidenceRaw;
    private float[] logitSmooth;
    private GazeClass smoothClass;
    private float confidenceSmooth;
    private GazeClass MVote;

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
}
