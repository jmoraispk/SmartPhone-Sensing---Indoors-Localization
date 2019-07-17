package com.example.sps;

import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;

import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final int NUM_BUCKETS_TO_USE = 100;


    //calculates the means of the RSSI measurement for each BSSID.
    public static Map<String, Float> calculateMeans(List<WifiScan> scansOfCell) {
        Map<String, Float> toReturn = new HashMap<>();

        //BSSID -> histogram of RSS values
        Map<String, int[]> count = new HashMap<>();

        //Get all scans done in a certain cell
        for(WifiScan scan : scansOfCell) {
            //get the results of each scan
            for(WifiReading reading : scan.getReadings()) {
                //if that bssid hasn't been found  yet, add it
                if(!count.containsKey(reading.getBSSID())) {
                    count.put(reading.getBSSID(), new int[NUM_BUCKETS_TO_USE]);
                }
                //increment the count for that BSSID
                count.get(reading.getBSSID())[Math.abs(reading.getRSS())]++;

            }
        }

        for(String bssid : count.keySet()){
            int[] hist = count.get(bssid);
            float mean = 0;
            int total = 0;

            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                total += hist[i];
            }
            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                mean += hist[i] * i;
            }
            toReturn.put(bssid, - mean/total);
        }
        return toReturn;
    }

    //Calculates the Standard Deviations of the RSSIs for each BSSI every time it appeared on a scan of that cell
    public static Map<String, Float> calculateStdDevs(List<WifiScan> scansOfCell, Map<String, Float> means) {
        Map<String, Float> toReturn = new HashMap<>();

        //BSSID -> histogram of RSS values
        Map<String, int[]> count = new HashMap<>();

        for(WifiScan scan : scansOfCell) {

            for(WifiReading reading : scan.getReadings()) {

                if(!count.containsKey(reading.getBSSID())) {
                    count.put(reading.getBSSID(), new int[NUM_BUCKETS_TO_USE]);
                }

                count.get(reading.getBSSID())[Math.abs(reading.getRSS())]++;

            }
        }

        for(String bssid : count.keySet()){
            int[] hist = count.get(bssid);
            float stddev = 0;
            float mean = means.get(bssid);

            //Standard Deviation computation
            // TODO: we should use the other function... right?
            // I think we don't even need to send the -i in there IF the mean is calculated with a -,
            //meaning: we can do stdDev(hist, - mean) instead of inverting all of them
            int c = 0;
            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                for(int j = 0; j < hist[i]; j++) {
                    stddev += Math.pow(-i - mean, 2);
                }
                c += hist[i];
            }
            stddev /= c;


            toReturn.put(bssid, (float) Math.sqrt(stddev));
        }

        return toReturn;
    }

    //Computes the mean of a List of Floats
    public static float mean(List<Float> list) {

        float mean = 0;
        for (int i = 0; i < list.size(); i++) {
            mean += list.get(i);
        }
        mean /= list.size();

        return mean;
    }

    //Computes the Standard Deviation of a List of Floats
    public static float stdDeviation(List<Float> list, Float mean) {

        float stdDev = 0;
        for (int i = 0; i < list.size(); i++) {
            stdDev += (float) Math.pow(list.get(i) - mean,2);
        }
        stdDev /= list.size();
        stdDev = (float) Math.sqrt(stdDev);

        return stdDev;
    }

    //Computes the correlation for 2 lists
    public static List<Float> correlation(List<Float> data1, List<Float> data2, int minDelay, int maxDelay) {


        List<Float> array1;
        List<Float> array2;

        Float mean1, mean2;
        Float stdDev1, stdDev2;


        List<Float> correlationForEachDelay = new ArrayList<>();

        for (int delay = minDelay; delay < maxDelay; delay++) {
            float sum = 0;

            array1 = data1.subList(0, delay);
            array2 = data2.subList(delay, 2 * delay);
            mean1 = Utils.mean(array1);
            mean2 = Utils.mean(array2);
            stdDev1 = Utils.stdDeviation(array1, mean1);
            stdDev2 = Utils.stdDeviation(array2, mean2);

            for (int k = 0; k < delay-1; k++) {
                sum += (array1.get(k) - mean1) * (array2.get(k) - mean2);
            }
            sum /= (delay * stdDev1 * stdDev2);
            correlationForEachDelay.add(sum);
        }

        return correlationForEachDelay;
    }

    //Returns the index of the largest element in the List
    public static int argMax(List<Float> elements) {
        if (elements == null || elements.size() == 0)
            return -1;

        float maxVal = Float.MIN_VALUE;
        int maxIndex = 0;

        for(int i = 0; i < elements.size(); i++) {
            if(elements.get(i) > maxVal) {
                maxIndex = i;
                maxVal = elements.get(i);
            }
        }
        return  maxIndex;
    }

    //TODO: there is an argMax function... (Bottom line: how to convert float[] to a list in the same line?
    // argMax(new Arraylist<>(array)) doesn't.)
    public static int getIndexOfLargestOnArray(float[] array) {
        if (array == null || array.length == 0)
            return -1;

        int largest = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest;
    }



    public static List<Float> fourierTransform(List<Float> sig) {

        int MAX_POWER_OF_TWO = 16;

        //array out of size constraints
        if (sig.size() > Math.pow(2, MAX_POWER_OF_TWO) && sig.size() < 4)
            return null;

        //Get the List to Doubles, needed for the Transform function.
        List<Double> signalTimeDomain = new ArrayList<>();
        for (Float sample : sig)
            signalTimeDomain.add((double) sample);


        boolean isPowerOfTwo = false;
        for (int i = 2; i < MAX_POWER_OF_TWO; i++) {
            if (sig.size() / Math.pow(2, i) == 1)
                isPowerOfTwo = true;
        }


        //pad the data to become of a length that is a power of 2.
        //The fft will be different but will preserve the same information, therefore won't change the signal :
        // https://dsp.stackexchange.com/questions/8792/fft-of-size-not-a-power-of-2
        // (because our data can have means different from zero, we will pad it instead with the mean: may increase the contributions on the DC component
        //  but if it is done only for signals with the same length, the contribution will be the same and they can still be compared on that frequency as well)
        if (!isPowerOfTwo) {
            double sigMean = mean(sig);
            for (int i = 2; i < MAX_POWER_OF_TWO; i++) {
                if (signalTimeDomain.size() < Math.pow(2, i)) {
                    // pad until the size reaches the next power of 2
                    while (signalTimeDomain.size() != Math.pow(2, i))
                        signalTimeDomain.add(sigMean);

                    break;
                }
            }
        }


        //Computes the Fourier Transform of a signal.
        //The transform will be done in place, so the input must be double[][]
        //UNITARY normalization - normalizes que output to the number of samples of the input
        //FORWARD transformation type - to the frequency domain (backwards would be the inverse transform)

        double[][] sigInput = new double[2][signalTimeDomain.size()];
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < signalTimeDomain.size(); k++) {
                if (i == 1)
                    sigInput[i][k] = 0; //imaginary part of the signal
                else
                    sigInput[i][k] = signalTimeDomain.get(k); //real part of the signal
            }
        }

        FastFourierTransformer.transformInPlace(sigInput, DftNormalization.STANDARD,TransformType.FORWARD);

        ArrayList<Float> magnitudesOutputSignal = new ArrayList<>();

        //magnitude from real and imaginary part of the transform.
        for (int k = 0; k < signalTimeDomain.size(); k++)
            magnitudesOutputSignal.add((float) Math.sqrt(Math.pow(sigInput[0][k],2) + Math.pow(sigInput[1][k],2)));


        return magnitudesOutputSignal;
    }

}
