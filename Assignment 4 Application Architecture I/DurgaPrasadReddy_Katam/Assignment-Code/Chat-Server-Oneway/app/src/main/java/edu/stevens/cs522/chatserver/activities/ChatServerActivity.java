/*********************************************************************

    Chat server: accept chat messages from clients.
    
    Sender name and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2017 Stevens Institute of Technology

**********************************************************************/
package edu.stevens.cs522.chatserver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;

import java.io.StringReader;
import java.util.Date;
import java.util.List;

import edu.stevens.cs522.base.Datagram;
import edu.stevens.cs522.base.DatagramConnectionFactory;
import edu.stevens.cs522.base.IDatagramConnection;
import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.databases.ChatDatabase;
import edu.stevens.cs522.chatserver.databases.MessageDao;
import edu.stevens.cs522.chatserver.databases.PeerDao;
import edu.stevens.cs522.chatserver.entities.Message;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.MessagesAdapter;

public class ChatServerActivity extends FragmentActivity implements OnClickListener {

	final static public String TAG = ChatServerActivity.class.getCanonicalName();

    public final static String SENDER_NAME = "name";

    public final static String CHATROOM = "room";

    public final static String MESSAGE_TEXT = "text";

    public final static String TIMESTAMP = "timestamp";

    public final static String LATITUDE = "latitude";

    public final static String LONGITUDE = "longitude";
		
	/*
	 * Socket used both for sending and receiving
	 */
    private IDatagramConnection serverConnection;


    /*
	 * True as long as we don't get socket errors
	 */
	private boolean socketOK = true;

    /*
     * Data access objects.
     */
    private ChatDatabase chatDatabase;

    private MessageDao messageDao;

    private PeerDao peerDao;

    public ListView messageList;
    public Button next;


    /*
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.view_messages);

        /**
         * Let's be clear, this is a HACK to allow you to do network communication on the messages thread.
         * This WILL cause an ANR, and is only provided to simplify the pedagogy.  We will see how to do
         * this right in a future assignment (using a Service managing background threads).
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            /*
             * Get port information from the resources.
             */
            int port = getResources().getInteger(R.integer.app_port);

            DatagramConnectionFactory factory = new DatagramConnectionFactory();
            serverConnection = factory.getUdpConnection(port);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot open socket", e);
        }

        Log.d(TAG, "Initializing the UI with no messages....");

        MessagesAdapter messagesAdapter = new MessagesAdapter(this);
        /*
         * List of messages and adapter.
         */
        messageList = findViewById(R.id.message_list);
        messageList.setAdapter(messagesAdapter);
        Log.d(TAG,messagesAdapter.toString());



        Log.d(TAG, "Opening the database....");
        // open the database
        // Note use getApplicationContext, do not make DB depend on UI!
        chatDatabase = ChatDatabase.getInstance(getApplicationContext());
        messageDao = chatDatabase.messageDao();
        peerDao = chatDatabase.peerDao();




        Log.d(TAG, "Querying the database asynchronously....");
        // query the database asynchronously, registering an observer for the result.
        // Note: The adapter has a method for resetting the backing store.
        LiveData<List<Message>> messagesLiveData = messageDao.fetchAllMessages();
        messagesLiveData.observe(this, messages -> {
            try {
                messagesAdapter.setElements(messages);
                messageList.setAdapter(messagesAdapter);

            } catch (Exception e) {

                Log.e(TAG, "Error while updating UI with messages", e);
            }
        });
        Log.d(TAG, "Binding the callback for the NEXT button....");
        next=findViewById(R.id.next);



	}

    public void onClick(View v) {

		Datagram receivePacket = new Datagram();

		try {

            String sender = null;

            String room = null;

            String text = null;

            Date timestamp = null;

            Double latitude = null;

            Double longitude = null;

            /*
             * THere is an apparent bug in the emulator stack on Windows where
             * messages can arrive empty, we loop as a workaround.
             */

            Log.d(TAG, "Waiting for a message....");
            serverConnection.receive(receivePacket);
            Log.d(TAG, "Received a packet!");

            if (receivePacket.getData() == null) {
                Log.d(TAG, "....missing data, skipping....");
                return;
            }

            Log.d(TAG, "Source Address: " + receivePacket.getAddress());

            String content = receivePacket.getData();
            Log.d(TAG, "Message received: " + content);

            /*
             * Parse the JSON object
             */
            JsonReader rd = new JsonReader(new StringReader(content));

            rd.beginObject();
            if (SENDER_NAME.equals(rd.nextName())) {
                sender = rd.nextString();
            }
            if (CHATROOM.equals(rd.nextName())) {
                room = rd.nextString();
            }
            if (MESSAGE_TEXT.equals((rd.nextName()))) {
                text = rd.nextString();
            }
            if (TIMESTAMP.equals(rd.nextName())) {
                timestamp = new Date(rd.nextLong());
            }
            if (LATITUDE.equals(rd.nextName())) {
                latitude = rd.nextDouble();
            }
            if (LONGITUDE.equals((rd.nextName()))) {
                longitude = rd.nextDouble();
            }
            rd.endObject();

            rd.close();

            /*
             * Add the sender to our list of senders
             */
            Peer peer = new Peer();
            peer.name = sender;
            peer.timestamp = timestamp;
            peer.latitude = latitude;
            peer.longitude = longitude;

            Message message = new Message();
            message.messageText = text;
            message.chatroom = room;
            message.sender = sender;
            message.timestamp = timestamp;
            message.latitude = latitude;
            message.longitude = longitude;

            /*
			 *  upsert peer and insert message into the database
			 */

                peerDao.upsert(peer);

                long rowid=messageDao.persist(message);
            if (rowid != -1) {
                Log.d(TAG, "Message inserted successfully with row ID: " + rowid);
            } else {

                Log.e(TAG, "Failed to insert message.");
            }

            /*
             * End
             *
             * The livedata for the messages should update via observer automatically.
             */

		} catch (Exception e) {
			
			Log.e(TAG, "Problems receiving packet: " + e.getMessage(), e);
			socketOK = false;
		} 

	}

	/*
	 * Close the socket before exiting application
	 */
	public void closeSocket() {
	    if (serverConnection != null) {
            serverConnection.close();
            serverConnection = null;
        }
	}

	/*
	 * If the socket is OK, then it's running
	 */
	boolean socketIsOK() {
		return socketOK;
	}

    public void onDestroy() {
        super.onDestroy();
        chatDatabase = null;
        closeSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // inflate a menu with PEERS option
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chatserver_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.peers) {
            // PEERS provide the UI for viewing list of peers
            // The subactivity will query the database for the list of peers.
            Intent intent = new Intent(this, ViewPeersActivity.class);
            startActivity(intent);
        return true;

        }
        return false;
    }

}