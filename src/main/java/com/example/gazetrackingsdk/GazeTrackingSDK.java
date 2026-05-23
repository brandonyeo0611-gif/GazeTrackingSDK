package com.example.gazetrackingsdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.content.ContentValues;
import android.provider.MediaStore;

import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

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
    private GazeClass trueLabel;
    // suggested to change to GazeClass because else there is leak
    private final CaptureLayer captureLayer;
    private final ModelInference modelInference;

    private final PreprocessLayer preprocessLayer;

    private final PostprocessingLayer postprocessingLayer;
    private CalibrationLayer calibrationLayer;
    private PredictionListener predictionListener;

    private CsvLogger csvLogger;

    // specific to user
    private final String userID;
    private Pair<Float,float[]> calibrations = null;
    private File calibrationFile;
    private File inferenceFile;
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
        this.postprocessingLayer = new PostprocessingLayer();

        // user-specific
        this.userID = builder.userID;


    }




    public static class Builder {
        // needs a assetname!!
        private final Context context;
        private final LifecycleOwner lifecycleOwner;
        private String modelAssetName;
        private boolean enableCalibration = false;
        private boolean enableSmoothing = false;
        private boolean enableMajorityVote = false;
        private boolean enableCsvLogging = false;
        private boolean enableFrameSaving = false;
        private final String userID;

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
            if (modelAssetName == null) {
                throw new IllegalStateException(
                        "Model asset name required"
                );
            }

            return new GazeTrackingSDK(this);
        }
    }
    public void setPredictionListener(PredictionListener predictionListener) {
        this.predictionListener = predictionListener;
    }
    public void setTrueLabel(GazeClass gazeClass) {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        this.trueLabel = gazeClass;
    }

    // start capturing
    private void start() {
        this.captureLayer.start();
    }

    private void stop() {
        this.captureLayer.stop();
    }

    public float[] getRawLogitsInference(Bitmap bitmap) {
        ByteBuffer byteBuffer = this.preprocessLayer.convertBitmapToByteBuffer(bitmap);
        float[] res = this.modelInference.getInference(byteBuffer);
        return res;
    }
    public void onCapture(Bitmap bitmap) {
        if (enableFrameSaving) {
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Documents/" + userID);
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");// media type
                var time = System.currentTimeMillis();
                if (mode == Mode.CALIBRATION && trueLabel != null) {
                    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, trueLabel + "_" + time + ".jpeg");
                } else {
                    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, time + ".jpeg");
                }
                var contentResolver = context.getContentResolver();
                var uniformResourceIdentifier = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                // uri points to file
                // url point to website

                if (uniformResourceIdentifier != null) {
                    OutputStream stream = contentResolver.openOutputStream(uniformResourceIdentifier);
                    assert stream != null;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    stream.flush();
                    stream.close();
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }

        if (mode == null) {
            return;
        }
        switch (mode) {
            case CALIBRATION:
                if (trueLabel == null) {
                    throw new NullPointerException("Please set true label first");
                } else {
                    // calibrate base on raw logits
                    float[] logits = getRawLogitsInference(bitmap);
                    this.calibrationLayer.add(trueLabel.ordinal(), logits);
                    // should I feed the predicted into the predictive listener here too?
                    // would that mean that need go through the checks of enable smoothing ... like code below?

                    if (enableCsvLogging) {
                        csvLogger.onPrediction(logits, trueLabel);
                    }
                }
                break;

            case PREDICTION:
                float[] logitRaw; GazeClass rawClass; float confRaw; Optional<float[]> logitSmooth = Optional.empty(); Optional<GazeClass> smoothClass = Optional.empty(); Optional<Float> confSmooth = Optional.empty(); Optional<GazeClass> mVote = Optional.empty();
                logitRaw = getRawLogitsInference(bitmap);

                if (enableCalibration && calibrations != null) {
                    logitRaw = PostprocessingLayer.applyScaling(calibrations.second, calibrations.first, logitRaw);
                }

                Pair<Float, Integer> confnPredRaw = PostprocessingLayer.ConfnPred(logitRaw);
                rawClass = GazeClass.values()[confnPredRaw.second];
                confRaw = confnPredRaw.first;
                if (enableSmoothing) {
                    logitSmooth = Optional.of(postprocessingLayer.applySmoothing(logitRaw));
                    Pair<Float, Integer> confnPredSmooth = PostprocessingLayer.ConfnPred(logitSmooth.get());
                    smoothClass = Optional.of(GazeClass.values()[confnPredSmooth.second]);
                    confSmooth = Optional.of(confnPredSmooth.first);
                }


                // note that if smoothing is enabled, only apply mvote on smoothened one
                if (enableMajorityVote) {
                    if (enableSmoothing) {
                        int index = postprocessingLayer.applyRecencyWeightedVote(smoothClass.get().ordinal());
                        mVote = Optional.of(GazeClass.values()[index]);
                    } else {
                        int index = postprocessingLayer.applyRecencyWeightedVote(rawClass.ordinal());
                        mVote = Optional.of(GazeClass.values()[index]);
                    }
                }

                GazePrediction p = new GazePrediction(logitRaw, rawClass, confRaw,logitSmooth, smoothClass, confSmooth,  mVote);

                if (predictionListener != null) {
                    predictionListener.onPrediction(p);
                }

                if (enableCsvLogging) {
                    csvLogger.onPrediction(p);
                }
        }
    }

    public Pair<GazeClass, Float> predict(Bitmap bitmap) {
        float[] logitRaw = getRawLogitsInference(bitmap);

        if (enableCalibration && calibrations != null) {
            logitRaw = PostprocessingLayer.applyScaling(calibrations.second, calibrations.first, logitRaw);
        }

        Pair<Float, Integer> confnPredRaw = PostprocessingLayer.ConfnPred(logitRaw);
        GazeClass rawClass = GazeClass.values()[confnPredRaw.second];
        return new Pair<>(rawClass, confnPredRaw.first);
    }
    // public void setMode(Mode mode) {
    //    this.mode = mode;
    //} would something like this be better? so the user set the mdoe first then afterthat just run start()
    public void startInference() {
        this.mode = Mode.PREDICTION;
        if (enableCsvLogging) {
            this.csvLogger = new CsvLogger(context, userID, mode);
        }
        start();
    }

    public void stopInference() {
        stop();
        this.mode = null;
        if (enableCsvLogging) {
            csvLogger.close();
            this.inferenceFile = csvLogger.getFile();
        }
    }

    public void startCalibration() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        this.mode = Mode.CALIBRATION;
        if (enableCsvLogging) {
            this.csvLogger = new CsvLogger(context, userID, mode);
        }
        start();
    }

    public void stopCalibration() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        stop();
        this.mode = null;
        if (enableCsvLogging) {
            csvLogger.close();
            this.calibrationFile = csvLogger.getFile();
        }
    }

    public Pair<Float, float[]> getCalibrationResults() {
        if (!enableCalibration) {
            throw new IllegalStateException("Calibration not enabled");
        }
        if (calibrations == null) {
            float scalar = calibrationLayer.binarySearch(0.1F, 10); // note that low shouldn't be negative else it flips the sign for ALL logits, not 0 to prevent divide by zero
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

    public File getCalibrationFile() {
        return calibrationFile;
    }

    public File getInferenceFile() {
        return inferenceFile;
    }
}
