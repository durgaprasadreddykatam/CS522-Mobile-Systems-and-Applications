package edu.stevens.cs522.chat.services;

import android.os.ResultReceiver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

/**
 * Created by dduggan.
 */

public interface IChatService {

    void send(String destAddress, String chatRoom, String message,
                     Date timestamp, double latitude, double longitude,
                     ResultReceiver receiver);

}
