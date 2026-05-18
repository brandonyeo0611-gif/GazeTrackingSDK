package com.example.gazetrackingsdk;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public interface FrameListener {
    protected void onCapture(Bitmap bitmap);
}
