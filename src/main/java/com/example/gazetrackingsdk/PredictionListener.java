package com.example.gazetrackingsdk;

public interface PredictionListener {
    public void onPrediction(GazePrediction p);

    public void onError();
}
