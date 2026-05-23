package com.example.gazetrackingsdk;

import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;

public class PostprocessingLayer {
    private final ArrayList<float[]> logits;
    private static final int size = 5;
    private final ArrayList<Integer> votes;
    private static final float[] weights = {0.05f, 0.1f, 0.15f, 0.3f, 0.4f};
    PostprocessingLayer() {
        this.logits = new ArrayList<>();
        this.votes = new ArrayList<>();
    }

    float[] applySmoothing(float[] rawLogits) {
        logits.add(rawLogits);
        // append latest logit to the list then afterwards do the smoothing on it
        if (logits.size() > size) {
            logits.remove(0);
        }
        float[] sol = new float[9];
        if (logits.size() < size) {
            for (float[] f : logits) {
                for (int i = 0; i < rawLogits.length; i++) {
                    sol[i] += f[i];
                }
            }
            for (int i = 0; i < rawLogits.length; i++) {
                sol[i] /= logits.size();
            }
            // simple moving average when the number of logits stored is less than size;
        } else {
            for (int j = 0; j < logits.size(); j++) {
                for (int i = 0; i < rawLogits.length; i++) {
                    sol[i] += logits.get(j)[i] * weights[j];
                }
                // use a weighted sum such that the more recent results are weighted more
            }
        }
        return sol;
    }

    static ArrayList<float[]> applyScaling(float[] bias, float scalar, ArrayList<float[]> logits) {
        ArrayList<float[]> result = new ArrayList<>();
        for (float[] f : logits) {
            result.add(applyScaling(bias,scalar, f));
        }
        return result;
    }

    static float[] applyScaling(float[] bias, float scalar, float[] logits) {
        float[] result = new float[9];

        for (int i = 0; i < 9; i++) {
            // changed to scale first then add bias
            result[i] = ( logits[i] / scalar + bias[i] ) ;
        }
        return result;
    }




    static float[] applySoftmax(float[] logits) {
        // pass in the logits and return the vector with the softmax function applied then can select the argmax to get predicted class
        float sumOfExp = 0.0f;
        float[] sol = new float[9];
        float findMax = logits[0];
        for (int i = 1 ; i < logits.length; i++) {
            if (logits[i] > findMax) {
                findMax = logits[i];
            }
        }

        for (int i = 0 ; i < logits.length; i++) {
            sol[i] = (float) Math.exp(logits[i] - findMax);
            sumOfExp += sol[i];
            // usually subtract the max first for stability
            // make it more efficient; because math.exp is computationally expensive
        }
        for (int i = 0 ; i < logits.length; i++) {
            sol[i] /= sumOfExp;
        }

        return sol;
    }


    static Pair<Float,Integer> ConfnPred(float[] rawLogits) {
        float[] result = PostprocessingLayer.applySoftmax(rawLogits);
        float max = result[0];
        int n = 0;
        for (int i = 1; i < result.length; i ++) {
            if (result[i] > max) {
                n = i;
                max = result[i];
            }
        }
        // max contains the confidence and the n contains the predicted class label
        return new Pair<Float,Integer>(max,n);

    }

    int applyRecencyWeightedVote(int prediction) {
        votes.add(prediction);
        if (votes.size() > size) {
            votes.remove(0);
        }
        float[] sol =  new float[9];
        // initialise a [0,0,0,0,0,0,0,0,0] matrix

        // if the total votes < 5, need to normalise afterwards
        float norm = 0f;

        for (int i = 0; i < votes.size();i++ ) {
            norm += weights[i];
            int pastPredictions = votes.get(i);
            sol[pastPredictions] += 1 * weights[i];
            // score them based on past predictions and store it in sol variable
            // sol variable is just a temp array from 1-9 to store the scores
            // qn : this was the first thing that come to mind for me when I was doing majority vote, are there other better ones?
        }
        // I think it's easier to process with numbers instead of like BOTTOM_LEFT from the gaze class but ultimately when we return we would use the gazeclass one

        // normalising --> do this because if less than 5
        for (int i = 0; i < sol.length; i++ ) {
            sol[i] /= norm;
        }
        int winner = 0;
        float maxScore = 0.0f;
        for (int i = 0; i < sol.length; i++) {
            if (sol[i] > maxScore) {
                maxScore = sol[i];
                winner = i;
            }
        }
        return winner;
    }
}
