package com.example.gazetrackingsdk;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import android.content.res.AssetManager;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelInference {
    private Context context;
    private Interpreter interpreter;
    // intepreter is the model

    ModelInference(Context context) {
        this.context = context;
        initialiseInterpreter();
        // call the below method to initialise the interpreter
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // 1. Open the model file as an AssetFileDescriptor
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);

        // 2. Create an input stream and get its channel
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        // 3. Get metadata about where the file starts and its length in assets
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        // 4. Map the file into memory and return it
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        // boilerplate code
    }

    private void initialiseInterpreter() {
        AssetManager assetManager = context.getAssets();
        try {
            MappedByteBuffer model = loadModelFile(assetManager, "finaloutputmatrix.tflite"); // type that is fed into tensorlite
            Interpreter interpreter = new Interpreter(model);
            // the interpreter is the model
            // interpret whats the input

            //int[] inputShape = interpreter.getInputTensor(0).shape();
            // should be [1,224,224,3] first one is the batch size i.e. no. of image input in each tensorlite model
            // DataType outputDataType = interpreter.getOutputTensor(0).dataType();

            // int inputImageWidth = inputShape[1];
            // int inputImageHeight = inputShape[2];
            // int inputImageDepth = inputShape[3];
            // currently not using but possible to use for debug in the future

// Finish interpreter initialization
            this.interpreter = interpreter;
        } catch (Exception e) {
            // errrrr no choice ig need assume it works??
        }
    }

    public float[] getInference(ByteBuffer byteBuffer) {
        try {
            float[][] logits = new float[1][9];
            interpreter.run(byteBuffer, logits);
            return logits[0];
        } catch (Exception e) {
            return new float[9];
        }
    }
}
