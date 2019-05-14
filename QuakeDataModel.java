package com.kalom.unipismartalert;

public class QuakeDataModel {

    private long currentTime;
    private String latitude;
    private String longtitude;

    public QuakeDataModel(String latitude, String longtitude) {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public QuakeDataModel(long currentTime, String latitude, String longtitude) {
        this.currentTime = currentTime;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(String longtitude) {
        this.longtitude = longtitude;
    }
}
