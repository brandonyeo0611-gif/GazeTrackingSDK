package com.example.gazetrackingsdk;

import android.util.Pair;

import java.util.LinkedList;

public class Postprocessing {
    private final LinkedList<float[]> logits;
    private static final int size = 5;
    private final LinkedList<Integer> votes;
    private static final float[] weights = {0.05f, 0.1f, 0.15f, 0.3f, 0.4f};
    Postprocessing() {
        this.logits = new LinkedList<>();
        this.votes = new LinkedList<>();
    }

    float[] applySmoothing(float[] rawLogits) {
        logits.addLast(rawLogits);
        if (logits.size() > size) {
            logits.removeFirst();
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
                    sol[i] += logits.get(j)[i] + weights[j];
                }
                // use a weighted sum such that the earlier results are weighted more
            }
            for (int i = 0; i < rawLogits.length; i++) {
                sol[i] /= logits.size();
            }
        }
        return sol;

    }

    static LinkedList<float[]> applyScaling(float[] bias, float scalar, LinkedList<float[]> logits) {
        LinkedList<float[]> result = new LinkedList<>();
        for (float[] f : logits) {
            result.add(applyScaling(bias,scalar, f));
        }
        return result;
    }

    static float[] applyScaling(float[] bias, float scalar, float[] logits) {
        float[] result = new float[9];

        for (int i = 0; i < 9; i++) {
            result[i] = ( logits[i] + bias[i] ) / scalar;
        }
        return result;
    }




    static float[] applySoftmax(float[] logits) {
        // pass in the logits and return the vector with the softmax function applied then can select the argmax to get predicted class
        float sumOfExp = 0.0f;
        float[] sol = new float[9];

        for (int i = 0 ; i < logits.length; i++) {
            sol[i] = (float) Math.exp(logits[i]);
            sumOfExp += sol[i];
            // make it more efficient; because math.exp is computationally expensive
        }
        for (int i = 0 ; i < logits.length; i++) {
            sol[i] /= sumOfExp;
        }

        return sol;
    }


    static Pair<Float,Integer> ConfnPred(float[] rawLogits) {
        float[] result = Postprocessing.applySoftmax(rawLogits);
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

    int majorityVote(int prediction) {
        votes.addLast(prediction);
        if (votes.size() > size) {
            votes.removeFirst();
        }
        float[] sol =  new float[9];

        for (int i = 0; i < votes.size();i++ ) {
            int pastPredictions = votes.get(i);
            sol[pastPredictions] += 1 * weights[i];
            // score them based on past predictions and store it in sol variable
            // sol variable is just a temp array from 1-9 to store the scores
            // qn : this was the first thing that come to mind for me when I was doing majority vote, are there other better ones?

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
