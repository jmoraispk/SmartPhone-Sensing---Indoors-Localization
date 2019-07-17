package com.example.sps.localization_method;

public enum LocalizationAlgorithm {


    KNN_FINGERPRINT(new FingerprintKnnLocalizationMethod()),
    KNN_RSSI(new RSSIFingerprintKnnLocalizationMethod()),
    PARALLEL_BAYESIAN(new ParallelBayesianLocalizationMethod()),
    SERIAL_BAYESIAN(new SerialBayesianLocalizationMethod()),
    PARTICLE_FILTER_SIMPLE(new ParticleFilterLocalization());
    private LocalizationMethod method;

    LocalizationAlgorithm(LocalizationMethod method) {
        this.method = method;
    } //enum constructors are automatically called

    public LocalizationMethod getMethod() {
        return method;
    }
}
