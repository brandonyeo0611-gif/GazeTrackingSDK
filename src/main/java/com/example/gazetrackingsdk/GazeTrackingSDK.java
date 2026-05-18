package com.example.gazetrackingsdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;

import androidx.lifecycle.LifecycleOwner;

import org.checkerframework.checker.units.qual.A;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

enum Mode {
    CALIBRATION,
    PREDICTION

}
public class GazeTrackingSDK implements FrameListener {
    // android dependencies
    private final Context context;
    private final LifecycleOwner lifecycleOwner;

    // configuration
    private final String modelAssetName;
    private final boolean enableCalibration;
    private final boolean enableSmoothing;
    private final boolean enableMajorityVote;
    private final boolean enableCsvLogging;
    private final boolean enableFrameSaving;

    // pipeline
    private Mode mode;
    private Integer trueLabel;
    private CaptureLayer captureLayer;
    private ModelInference modelInference;

    private PreprocessLayer preprocessLayer;
    private Postprocessing postprocessingLayerRaw;
    private Postprocessing postprocessingLayerSmooth;
    private CalibrationLayer calibrationLayer;
    private PredictionListener predictionListener;

    private CsvLogger csvLogger;

    // specific to user
    private AtomicInteger count;
    private String userID;
    private Pair<Float,float[]> calibrations;
    private GazeTrackingSDK(Builder builder) {
        // android dependency
        this.context = builder.context;
        this.lifecycleOwner = builder.lifecycleOwner;

        // configuration
        this.modelAssetName = builder.modelAssetName;
        this.enableCalibration = builder.enableCalibration;
        this.enableSmoothing = builder.enableSmoothing;
        this.enableMajorityVote = builder.enableMajorityVote;
        this.enableFrameSaving = builder.enableFrameSaving;
        this.enableCsvLogging = builder.enableCsvLogging;

        // pipeline
        this.captureLayer = new CaptureLayer(context, lifecycleOwner, this, builder.userID);
        this.preprocessLayer = new PreprocessLayer();
        this.modelInference = new ModelInference(context, modelAssetName);
        this.calibrationLayer = new CalibrationLayer();
        this.postprocessingLayerRaw = new Postprocessing();
        this.postprocessingLayerSmooth = new Postprocessing();
        this.csvLogger = new CsvLogger(context, builder.userID);

        // user-specific
        this.userID = builder.userID;
        this.count = new AtomicInteger();

    }




    public static class Builder {
        private Context context;
        private LifecycleOwner lifecycleOwner;
        private String modelAssetName;
        private boolean enableCalibration = false;
        private boolean enableSmoothing = false;
        private boolean enableMajorityVote = false;
        private boolean enableCsvLogging = false;
        private boolean enableFrameSaving = false;
        private String userID;

        // nested class is not implicitly public
        public Builder(Context context, LifecycleOwner lifecycleOwner, String userID) {
            this.context = context;
            this.lifecycleOwner = lifecycleOwner;
            this.userID = userID;
        }

        public Builder setModelAssetName(String modelAssetName) {
            this.modelAssetName = modelAssetName;
            return this;
        }

        public Builder setEnableCalibration(boolean enabled) {
            this.enableCalibration = enabled;
            return this;
        }

        public Builder setEnableSmoothing(boolean enabled) {
            this.enableSmoothing = enabled;
            return this;
        }

        public Builder setEnableMajorityVote(boolean enabled) {
            this.enableMajorityVote = enabled;
            return this;
        }

        public Builder setEnableCsvLogging(boolean enabled) {
            this.enableCsvLogging = enabled;
            return this;
        }

        public Builder setEnableFrameSaving(boolean enabled) {
            this.enableFrameSaving = enabled;
            return this;
        }

        public GazeTrackingSDK build() {
            return new GazeTrackingSDK(this);
        }
    }
    public void setPredictionListener(PredictionListener predictionListener) {
        this.predictionListener = predictionListener;
    }
    public void setTrueLabel(GazeClass gazeClass) {
        this.trueLabel = gazeClass.ordinal();
    }

    // start capturing
    public void start() {
        this.captureLayer.captureImage();
    }

    public void stop() {
        this.captureLayer.stop();
    }

    public float[] getRawLogitsInference(Bitmap bitmap) {
        ByteBuffer byteBuffer = this.preprocessLayer.convertBitmapToByteBuffer(bitmap);
        float[] res = this.modelInference.getInference(byteBuffer);
        return res;
    }
    protected void onCapture(Bitmap bitmap) {
        if (mode == null) {
            throw new IllegalStateException("Please choose a mode");
        }
        switch (mode) {
            case CALIBRATION:
                if (trueLabel == null) {
                    throw new NullPointerException("Please set true label first");
                } else {
                    float[] pred = getRawLogitsInference(bitmap);
                    this.calibrationLayer.add(trueLabel, pred);
                }
                break;

            case PREDICTION:
                // to-do
                break;
        }
    }

    public void startCalibration() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        this.mode = Mode.CALIBRATION;
        start();
    }

    public void stopCalibration() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        this.mode = null;
        stop();
    }

    public Pair<Float, float[]> getCalibrationResults() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }

        if (calibrations == null) {
            float scalar = calibrationLayer.binarySearch(-10, 10);
            float[] bias = calibrationLayer.coordinateDescent(scalar);
            this.calibrations = new Pair<>(scalar, bias);
            return calibrations;
        } else {
            return calibrations;
        }
    }

    public void clearCalibration() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        this.calibrations = null;
        this.calibrationLayer = new CalibrationLayer();
    }
}
