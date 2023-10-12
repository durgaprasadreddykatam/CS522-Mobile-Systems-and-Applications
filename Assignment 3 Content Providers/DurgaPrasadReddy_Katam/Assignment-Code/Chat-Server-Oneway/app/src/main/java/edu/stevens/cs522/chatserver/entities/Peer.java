package edu.stevens.cs522.chatserver.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.util.Date;

import edu.stevens.cs522.base.DateUtils;
import edu.stevens.cs522.chatserver.contracts.PeerContract;

/**
 * Created by dduggan.
 */

public class Peer implements Parcelable, Persistable {

    // Will be database key
    public long id;

    public String name;

    // Last time we heard from this peer.
    public Date timestamp;

    // Where we heard from them
    public Double latitude;

    public Double longitude;

    public Peer() {
    }

    public Peer(Cursor in) {
        this.id = PeerContract.getId(in);
        this.name = PeerContract.getName(in);
        this.timestamp = PeerContract.getTimestamp(in);
        this.latitude = PeerContract.getLatitude(in);
        this.longitude = PeerContract.getLongitude(in);

    }

    @Override
    public void writeToProvider(ContentValues out) {
        PeerContract.putId(out, this.id);
        PeerContract.putName(out, this.name);
        PeerContract.putTimestamp(out, this.timestamp);
        PeerContract.putLatitude(out, this.latitude);
        PeerContract.putLongitude(out, this.longitude);

    }


    @Override
    public int describeContents() {
        return 0;
    }

    public Peer(Parcel in) {
        id = in.readLong();
        name = in.readString();
        timestamp = (Date) in.readSerializable();
        latitude = in.readDouble();
        longitude = in.readDouble();

    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeSerializable(timestamp);
        out.writeDouble(latitude);
        out.writeDouble(longitude);

    }

    public static final Creator<Peer> CREATOR = new Creator<Peer>() {

        @Override
        public Peer createFromParcel(Parcel source) {
            return new Peer(source);

        }

        @Override
        public Peer[] newArray(int size) {
            return new Peer[size];
        }

    };
}
