package com.example.gazetrackingsdk;
import android.graphics.Bitmap;
import android.content.Context;

import java.nio.ByteBuffer;

public class Orchestrator implements FrameListener {
    private final ModelInference modelInference;
    private final PreprocessLayer preprocessLayer;
    public float[] logits;

    Orchestrator(Context context) {
        this.modelInference = new ModelInference(context.getApplicationContext());
        this.preprocessLayer = new PreprocessLayer();
    }

    public void onCapture(Bitmap bitmap) {
        ByteBuffer byteBuffer = preprocessLayer.convertBitmapToByteBuffer(bitmap);
        float[] logits = modelInference.getInference(byteBuffer);
        this.logits = logits;
        // do some post-processing
        // send to api to application such that it gives the logits? WRONG
        // note that web dev API and this APi call is completely different
    }

    public logits smoothen() {

    }
}
