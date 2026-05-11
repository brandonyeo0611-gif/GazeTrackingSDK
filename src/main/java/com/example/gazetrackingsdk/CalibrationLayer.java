package com.example.gazetrackingsdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// note that the whole point of this layer is to find bias and temperature scaling
// the formula imma use is to find the bias first then the temperature scaling so
// ( logit + bias ) / scalar
public class CalibrationLayer {
    private LinkedList<Integer> y_true_class;
    private LinkedList<float[]> y_pred_logits;


    CalibrationLayer() {
        this.y_true_class = new LinkedList<>();
        this.y_pred_logits = new LinkedList<>();
    }

    void add(int y_true, float[] y_pred) {
        this.y_true_class.add(y_true);
        this.y_pred_logits.add(y_pred);
    }

    float f1score(LinkedList<Integer> y_pred_class_after_scaling) {
        float f1result = 0;
        for (int i = 0; i < 9; i++) {
            int tp = 0, fp = 0, fn = 0;
            for (int j = 0; j < y_true_class.size(); j++) {
                if (y_pred_class_after_scaling.get(j) == i && y_true_class.get(j) == i) {
                    tp += 1;
                } else if (y_pred_class_after_scaling.get(j) == i && y_true_class.get(j) != i) {
                    fp += 1;
                } else if (y_pred_class_after_scaling.get(j) != i && y_true_class.get(j) == i) {
                    fn += 1;
                }
            }
            float precision = (tp + fp == 0) ? 0 : (float) tp / (tp + fp); // guard against 0 division error
            float recall = (tp + fn == 0) ? 0 : (float) tp / (tp + fn);
            float f1 = (precision + recall == 0) ? 0 : 2 * (precision * recall) / (precision + recall);
            f1result += f1;
        }
        return f1result / 9;
    }

    LinkedList<Integer> convert_to_class(LinkedList<float[]> logits) {
        LinkedList<Integer> result = new LinkedList<>();
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

    float[] coordinate_descent(float[] bias) {
        for (int i = 0; i < 100; i++) {
            // how many iterations u want
            for (int cat = 0; cat < 9; cat++) {
                float current_class_bias = bias[cat];

                LinkedList<float[]> logits = Postprocessing.applyScaling(bias, 1, this.y_pred_logits);
                float f1_same = f1score(convert_to_class(logits));

                bias[cat] = (float) (current_class_bias + 0.1);
                logits = Postprocessing.applyScaling(bias, 1, this.y_pred_logits);
                float f1_incr = f1score(convert_to_class(logits));

                bias[cat] = (float) (current_class_bias - 0.1);
                logits = Postprocessing.applyScaling(bias, 1, this.y_pred_logits);
                float f1_decr = f1score(convert_to_class(logits));

                List<Float> lst = List.of(f1_same,f1_incr,f1_decr);
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
        return bias;
    }


}
