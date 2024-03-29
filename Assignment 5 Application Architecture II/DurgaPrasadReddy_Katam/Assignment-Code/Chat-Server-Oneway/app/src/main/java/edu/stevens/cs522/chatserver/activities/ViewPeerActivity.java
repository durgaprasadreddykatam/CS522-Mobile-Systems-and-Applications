package edu.stevens.cs522.chatserver.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.entities.Message;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.TextAdapter;
import edu.stevens.cs522.chatserver.viewmodels.PeerViewModel;

/**
 * Created by dduggan.
 */

public class ViewPeerActivity extends FragmentActivity {

    public static final String TAG = ViewPeerActivity.class.getCanonicalName();

    public static final String PEER_KEY = "peer";

    private TextAdapter<Message> messagesAdapter;

    private RecyclerView messageList;

    PeerViewModel peerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_peer);

        Peer peer = getIntent().getParcelableExtra(PEER_KEY);
        if (peer == null) {
            throw new IllegalArgumentException("Expected peer id as intent extra");
        }

        TextView username=findViewById(R.id.view_user_name);
        TextView location=findViewById(R.id.view_location);
        TextView lastseen=findViewById(R.id.view_timestamp);
        messageList=findViewById(R.id.message_list);

        username.setText(getString(R.string.view_user_name, peer.name));
        lastseen.setText(getString(R.string.view_timestamp, formatTimestamp(peer.timestamp)));
        location.setText(getString(R.string.view_location, peer.latitude, peer.longitude));
        Log.i(TAG, "(Re)starting View Peer Activity");

        // Initialize the recyclerview and adapter for messages

        messageList.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new TextAdapter<>(messageList);
        messageList.setAdapter(messagesAdapter);

        // open the view model

        peerViewModel=new ViewModelProvider(this).get(PeerViewModel.class);


        // query the database asynchronously, and use messagesAdapter to display the result
                peerViewModel.loadMessages(peer).observe(this, new Observer<List<Message>>() {
                    @Override
                    public void onChanged(List<Message> messages) {
                        messagesAdapter.setDataset(messages);
                        messageList.setAdapter(messagesAdapter);

                    }
                });

    }

    private static String formatTimestamp(Date timestamp) {
        LocalDateTime dateTime = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "Leaving View Peer Activity");
    }

}
