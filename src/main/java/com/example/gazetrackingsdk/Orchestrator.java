package com.example.gazetrackingsdk;
import android.graphics.Bitmap;
import android.content.Context;

import java.nio.ByteBuffer;

public class Orchestrator implements FrameListener {
    private final ModelInference modelInference;
    // modelInference is a class that i made
    private final PreprocessLayer preprocessLayer;
    private final Postprocessing postprocessing;
    public float[] logits;

    Orchestrator(Context context) {
        this.modelInference = new ModelInference(context);
        this.preprocessLayer = new PreprocessLayer();
        this.postprocessing = new Postprocessing();
        // using the same postprocessing so there is kinda like a memory
    }

    public void onCapture(Bitmap bitmap) {
        ByteBuffer byteBuffer = preprocessLayer.convertBitmapToByteBuffer(bitmap);
        float[] logits = modelInference.getInference(byteBuffer);
        this.logits = logits;
        // do some post-processing
        // send to api to application such that it gives the logits? WRONG
        // note that web dev API and this APi call is completely different
    }

    public float[] smoothen() {
        return postprocessing.applySmoothing(logits);
    }
}
