package com.example.sps.data_loader;

public class WifiReading {

    private String BSSID;
    private int RSS;

    public WifiReading(String BSSID, int RSS) {
        this.BSSID = BSSID;
        this.RSS = RSS;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public int getRSS() {
        return RSS;
    }

    public void setRSS(int RSS) {
        this.RSS = RSS;
    }

    public static WifiReading fromString(String toConvert){
        String BSSID = toConvert.split(", ")[0];
        int RSS = Integer.parseInt(toConvert.split(", ")[1]);

        return new WifiReading(BSSID, RSS);
    }
}
