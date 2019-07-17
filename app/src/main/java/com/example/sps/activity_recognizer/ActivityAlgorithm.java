package com.example.sps.activity_recognizer;


public enum ActivityAlgorithm {

    NORMAL_STD(new StdDevActivityRecognizer()),
    STEP_DETECTOR(new StepDetectorActivityRecognizer()),
    NORMAL_AUTOCORR(new AutocorrActivityRecognizer()),
    CROSS_CORR(new CrossCorrActivityRecognizer()),
    FOURIER_TRANSFORM(new FourierTransformActivityRecognizer());



    private ActivityRecognizer method;

    ActivityAlgorithm(ActivityRecognizer method) {
        this.method = method;
    } //enum constructors are automatically called

    public ActivityRecognizer getMethod() {
        return method;
    }
}
