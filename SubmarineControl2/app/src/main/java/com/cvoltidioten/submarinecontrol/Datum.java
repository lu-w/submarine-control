package com.cvoltidioten.submarinecontrol;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;

/**
 * Represents one data point measured by the submarine. Adapt this if you want to add more data
 * functionality.
 */
class Datum implements Parcelable, Serializable {
    private double depthM = 0;
    private double temperatureC = 0;
    private double pressureBar = 0;
    private long timestamp = 0;

    Datum(double depthM, double temperatureC, double pressureBar, long timestamp) {
        this.depthM = depthM;
        this.temperatureC = temperatureC;
        this.pressureBar = pressureBar;
        this.timestamp = timestamp;
    }

    Datum(SubmarineProtos.Datum datum) {
        this.depthM = datum.getDepth();
        this.temperatureC = datum.getTemperature();
        this.pressureBar = datum.getTemperature();
        this.timestamp = datum.getTimestamp();
    }

    public double getDepthM() {
        return this.depthM;
    }

    public double getTemperatureC() {
        return this.temperatureC;
    }

    public double getPressureBar() {
        return this.pressureBar;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(this.depthM);
        out.writeDouble(this.temperatureC);
        out.writeDouble(this.pressureBar);
        out.writeLong(this.timestamp);
    }

    public final static Parcelable.Creator<Datum> CREATOR = new Parcelable.Creator<Datum>() {
        public Datum createFromParcel(Parcel in) {
            return new Datum(in);
        }

        public Datum[] newArray(int size) {
            return new Datum[size];
        }
    };

    private Datum(Parcel in) {
        this.depthM = in.readDouble();
        this.temperatureC = in.readDouble();
        this.pressureBar = in.readDouble();
        this.timestamp = in.readLong();
    }
}
