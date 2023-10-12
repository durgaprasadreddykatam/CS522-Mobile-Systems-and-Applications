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
        // TODO

    }

    @Override
    public void writeToProvider(ContentValues out) {
        // TODO

    }


    @Override
    public int describeContents() {
        return 0;
    }

    public Peer(Parcel in) {
        // TODO

    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // TODO

    }

    public static final Creator<Peer> CREATOR = new Creator<Peer>() {

        @Override
        public Peer createFromParcel(Parcel source) {
            // TODO
            // return null;
            return null;
        }

        @Override
        public Peer[] newArray(int size) {
            // TODO
            // return null;
            return null;
        }

    };
}
