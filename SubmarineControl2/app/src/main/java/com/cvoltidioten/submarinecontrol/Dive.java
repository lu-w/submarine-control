package com.cvoltidioten.submarinecontrol;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Represents one dive done by the submarine, including the starting time, offset, depth and the
 * later obtained data points.
 */
class Dive implements Parcelable, Serializable {
    private List<Datum> data = new ArrayList<>();
    private int depthM;
    private int offsetS;
    private int amountOfData;
    private Date startingTime;

    public Dive(int depthM, int offsetS) {
        this.depthM = depthM;
        this.offsetS = offsetS;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, offsetS);
        this.startingTime = cal.getTime();
    }

    public Dive(int depthM, int offsetS, List<SubmarineProtos.Datum> data) {
        this.depthM = depthM;
        this.offsetS = offsetS;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, offsetS);
        this.startingTime = cal.getTime();
        this.data.clear();
        for(SubmarineProtos.Datum datum : data) {
            this.data.add(new Datum(datum.getDepth(), datum.getTemperature(), datum.getPressure(), datum.getTimestamp()));
        }
        this.amountOfData = data.size();
    }

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<SubmarineProtos.Datum> data) {
        if(data != null && this.data != null) {
            this.data.clear();
            for(SubmarineProtos.Datum datum : data) {
                this.data.add(new Datum(datum.getDepth(), datum.getTemperature(), datum.getPressure(), datum.getTimestamp()));
            }
            this.amountOfData = data.size();
        } else {
            this.data = new ArrayList<>();
            this.amountOfData = 0;
        }
    }

    public int getDepthM() {
        return depthM;
    }

    public int getOffsetS() {
        return offsetS;
    }

    public Date getStartingTime() {
        return startingTime;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.depthM);
        out.writeInt(this.offsetS);
        out.writeInt(this.amountOfData);
        out.writeSerializable(this.startingTime);
        for(int i = 0; i < this.data.size(); i++) {
            out.writeParcelable(this.data.get(i), flags);
        }
    }

    public static final Parcelable.Creator<Dive> CREATOR
            = new Parcelable.Creator<Dive>() {
        public Dive createFromParcel(Parcel in) {
            return new Dive(in);
        }

        public Dive[] newArray(int size) {
            return new Dive[size];
        }
    };

    private Dive(Parcel in) {
        this.depthM = in.readInt();
        this.offsetS = in.readInt();
        this.amountOfData = in.readInt();
        this.startingTime = (Date)in.readSerializable();
        this.data = new ArrayList<>();
        for(int i = 0; i < this.amountOfData; i++) {
            this.data.add((Datum) in.readParcelable(Datum.class.getClassLoader()));
        }
    }
}
