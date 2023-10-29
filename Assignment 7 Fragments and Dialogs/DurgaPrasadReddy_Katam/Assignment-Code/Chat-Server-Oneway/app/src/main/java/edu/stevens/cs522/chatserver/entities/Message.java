package edu.stevens.cs522.chatserver.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

import javax.annotation.processing.Generated;

import edu.stevens.cs522.base.DateUtils;

/**
 * Created by dduggan.
 */

// TODO annotate (including FK constraints)
// You must also declare indices on the FK columns, otherwise integrity checking
// may trigger a linear search of this table.

@Entity(foreignKeys = @ForeignKey(
        entity = Peer.class,
        parentColumns = "name",
        childColumns = "sender",
        onDelete = ForeignKey.CASCADE
), indices = @Index("sender"))
public class Message implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String chatroom;

    public String messageText;

    public Date timestamp;

    public Double latitude;

    public Double longitude;

    public String sender;

    public Message() {
    }

    public Message(Parcel in) {
        id = in.readLong();
        chatroom = in.readString();
        messageText = in.readString();
        timestamp = DateUtils.readDate(in);
        latitude = in.readDouble();
        longitude = in.readDouble();
        sender = in.readString();
    }

    @NonNull
    @Override
    public String toString() {
        return messageText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(chatroom);
        out.writeString(messageText);
        DateUtils.writeDate(out, timestamp);
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeString(sender);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }

    };

}

