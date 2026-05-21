package com.example.gazetrackingsdk;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// note that the whole point of this layer is to find bias and temperature scaling
// the formula imma use is to find the bias first then the temperature scaling so
// ( logit + bias ) / scalar
public class CalibrationLayer {
    private ArrayList<Integer> yTrueClass;
    // use array list if there is heavy indexing
    private ArrayList<float[]> yPredLogits;
    private float[] bias = {0,0,0,0,0,0,0,0,0};


    CalibrationLayer() {
        this.yTrueClass = new ArrayList<>();
        this.yPredLogits = new ArrayList<>();
    }

    // addes the true label and the logits of the predicted into private list
    void add(int y_true, float[] y_pred) {
        this.yTrueClass.add(y_true);
        this.yPredLogits.add(y_pred);
    }

    // calculate the f1 score
    double f1Score(ArrayList<Integer> y_pred_class_after_scaling) {
        double f1result = 0;
        for (int i = 0; i < 9; i++) {
            int tp = 0, fp = 0, fn = 0;
            for (int j = 0; j < yTrueClass.size(); j++) {
                if (y_pred_class_after_scaling.get(j) == i && yTrueClass.get(j) == i) {
                    tp += 1;
                } else if (y_pred_class_after_scaling.get(j) == i && yTrueClass.get(j) != i) {
                    fp += 1;
                } else if (y_pred_class_after_scaling.get(j) != i && yTrueClass.get(j) == i) {
                    fn += 1;
                }
            }
            double precision = (tp + fp == 0) ? 0 : (double) tp / (tp + fp); // guard against 0 division error
            double recall = (tp + fn == 0) ? 0 : (double) tp / (tp + fn);
            double f1 = (precision + recall == 0) ? 0 : 2 * (precision * recall) / (precision + recall);
            f1result += f1;
        }
        return f1result / 9;
    }

    ArrayList<Integer> convertToClass(ArrayList<float[]> logits) {
        ArrayList<Integer> result = new ArrayList<>();
        for (float[] f : logits) {
            int max = 0;
            for (int i = 1; i < f.length; i++) {
                if (f[i] > f[max]) {
                    max = i;
                }
            }
            result.add(max);
        }
        return result;
    }

    float[] coordinateDescent(float scalar) {
        for (int i = 0; i < 100; i++) {
            // how many iterations u want
            for (int cat = 0; cat < 9; cat++) {
                float current_class_bias = bias[cat];

                // same bias
                ArrayList<float[]> logits = PostprocessingLayer.applyScaling(bias, scalar, this.yPredLogits);
                double f1_same = f1Score(convertToClass(logits));

                // bias with increment
                bias[cat] = (float) (current_class_bias + 0.1);
                logits = PostprocessingLayer.applyScaling(bias, scalar, this.yPredLogits);
                double f1_incr = f1Score(convertToClass(logits));

                // bias with decrement
                bias[cat] = (float) (current_class_bias - 0.1);
                logits = PostprocessingLayer.applyScaling(bias, scalar, this.yPredLogits);
                double f1_decr = f1Score(convertToClass(logits));

                List<Double> lst = List.of(f1_same,f1_incr,f1_decr);
                int index = lst.indexOf(Collections.max(lst));

                if (index == 0) {
                    bias[cat] = current_class_bias;
                } else if (index == 1) {
                    bias[cat] = (float) (current_class_bias + 0.1);
                } else {
                    bias[cat] = (float) (current_class_bias - 0.1);
                }
            }
        }
        this.bias = bias;
        return bias;
    }

    double softmaxLoss(ArrayList<float[]> logits) {
        double loss = 0.0;
        for (int i = 0; i < yTrueClass.size(); i++ ) {
            int index = yTrueClass.get(i);
            float[] softmax = PostprocessingLayer.applySoftmax(logits.get(i));
            loss += -Math.log(Math.max(softmax[index], 1e-12));
            // guard against 0 error
        }
        return loss / yTrueClass.size();
    }

    float binarySearch(float low, float high) {
        for (int i=0 ; i < 200; i++) {
            float mid = (low + high) / 2;
            double loss_left = softmaxLoss(PostprocessingLayer.applyScaling(bias, (float) (mid - 0.05), yPredLogits));
            double loss_right = softmaxLoss(PostprocessingLayer.applyScaling(bias, (float) (mid + 0.05), yPredLogits));

            if (loss_left < loss_right) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return (low + high) / 2;
    }

    boolean sameSize() {
        return yTrueClass.size() == yPredLogits.size();
    }
    // check if same size first before continuing.
}
