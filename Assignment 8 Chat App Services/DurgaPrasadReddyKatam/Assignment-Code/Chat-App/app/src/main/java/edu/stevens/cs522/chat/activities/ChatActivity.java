/*********************************************************************

    Chat server: accept chat messages from clients.
    
    Sender name and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2017 Stevens Institute of Technology

**********************************************************************/
package edu.stevens.cs522.chat.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import edu.stevens.cs522.base.DateUtils;
import edu.stevens.cs522.base.InetAddressUtils;
import edu.stevens.cs522.chat.R;
import edu.stevens.cs522.chat.databases.ChatDatabase;
import edu.stevens.cs522.chat.databases.ChatroomDao;
import edu.stevens.cs522.chat.dialog.SendMessage;
import edu.stevens.cs522.chat.entities.Chatroom;
import edu.stevens.cs522.chat.location.CurrentLocation;
import edu.stevens.cs522.chat.services.ChatService;
import edu.stevens.cs522.chat.services.IChatService;
import edu.stevens.cs522.chat.services.ResultReceiverWrapper;
import edu.stevens.cs522.chat.settings.Settings;
import edu.stevens.cs522.chat.viewmodels.SharedViewModel;

public class ChatActivity extends AppCompatActivity implements ChatroomsFragment.IChatroomListener, MessagesFragment.IChatListener, SendMessage.IMessageSender, ServiceConnection, ResultReceiverWrapper.IReceive {

    /*
     * We are using AppCompat to support Floating Action Button.
     */

    final static public String TAG = ChatActivity.class.getCanonicalName();

    /*
     * Fragments for two-pane UI
     */
    private final static String SHOWING_CHATROOMS_TAG = "INDEX-FRAGMENT";

    private final static String SHOWING_MESSAGES_TAG = "CHAT-FRAGMENT";

    private boolean isTwoPane;

    /*
     * Tag for dialog fragment
     */
    private final static String ADDING_MESSAGE_TAG = "ADD-MESSAGE-DIALOG";

    /*
     * Shared with both the index and detail fragments
     */
    private SharedViewModel sharedViewModel;

    /*
     * Reference to the service, for sending a message
     */
    private IChatService chatService;

    /*
     * For receiving ack when message is sent.
     */
    private ResultReceiverWrapper sendResultReceiver;

    /*
     * For inserting a chatroom.
     */
    private final Executor executor = Executors.newSingleThreadExecutor();

    private ChatroomDao chatroomDao;


    /*
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_activity);

        isTwoPane = getResources().getBoolean(R.bool.is_two_pane);

        if (!isTwoPane) {
            // Add an index fragment as the fragment in the frame layout (single-pane layout)
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, new ChatroomsFragment(),SHOWING_CHATROOMS_TAG)
                    // Don't add this (why not?): .addToBackStack(SHOWING_CHATROOMS_TAG)
                    .commit();
        }


        // get shared view model for current chatroom
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        

        //initialize sendResultReceiver (for receiving notification of message sent)
        sendResultReceiver = new ResultReceiverWrapper(new Handler(Looper.getMainLooper()));
        sendResultReceiver.setReceiver(this);


        // initiate binding to the service
        Intent intent = new Intent(this, ChatService.class);
        bindService(intent, this, BIND_AUTO_CREATE);



        // Only used to insert a chatroom
        chatroomDao = ChatDatabase.getInstance(getApplicationContext()).chatroomDao();
	}

    @Override
    public void onStart() {
	    super.onStart();
    }

    @Override
	public void onResume() {
        super.onResume();
        //  register result receiver
        sendResultReceiver.setReceiver(this);
     }

    @Override
    public void onPause() {
        super.onPause();
        //  unregister result receiver
        sendResultReceiver.setReceiver(null);
    }

    @Override
    public void onStop() {
	    super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // unbind the service
        unbindService(this);
    }

    @Override
    public void onBackPressed() {
        if (!isTwoPane) {
            super.onBackPressed();
            return;
        }
        /*
         * We are in two-pane mode, is a chatroom selected?
         */
        if (sharedViewModel.getSelected() == null) {
            super.onBackPressed();
            return;
        }
        /*
         * Unset the currently selected chatroom (in two-pane mode).
         */
        setChatroom(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // inflate a menu with PEERS options
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();

        if (itemId == R.id.register) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            return true;

        } else if (itemId == R.id.peers) {
            //  provide the UI for viewing list of peers
            Intent intent = new Intent(this, ViewPeersActivity.class);
            startActivity(intent);
             return true;

        }
        return false;
    }

    @Override
    /*
     * Callback for the SEND button.
     */
    public void sendMessageDialog(Chatroom chatroom) {

        if (chatroom == null) {
            return;
        }

        if (!Settings.isRegistered(this)) {
            Toast.makeText(this, R.string.register_necessary, Toast.LENGTH_LONG).show();
            return;
        }

        SendMessage.launch(this, chatroom, ADDING_MESSAGE_TAG);
    }

    @Override
    /*
     * Callback for the dialog to send a message.
     */
    public void send(String destinationAddr, String chatroomName, String clientName, String text) {

        if (chatService != null) {
            /*
             * On the emulator, which does not support WIFI stack, we'll send to
             * (an AVD alias for) the host loopback interface, with the server
             * port on the host redirected to the server port on the server AVD.
             */

            Date timestamp = DateUtils.now();

            CurrentLocation location = CurrentLocation.getLocation(this);
            // use chatService to send the message
            chatService.send(destinationAddr, chatroomName, text, timestamp, location.getLatitude(), location.getLongitude(), sendResultReceiver);
            Log.i(TAG, "Sent message: " + text);

        }
    }

    @Override
    /**
     * Show a text message when notified that sending a message succeeded or failed
     */
    public void onReceiveResult(int resultCode, Bundle data) {
        if (resultCode == RESULT_OK) {//  show a success toast message
            Toast.makeText(this, "Sucess", Toast.LENGTH_LONG).show();
        } else {// show a failure toast message
            Toast.makeText(this, "Failure", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    /**
     * Called by ChatroomsFragment when a new chatroom is added.
     */
    public void addChatroom(String chatroomName) {
        Chatroom chatroom = new Chatroom();
        chatroom.name = chatroomName;
        executor.execute(() -> {
            chatroomDao.insert(chatroom);
        });
    }

    @Override
    /**
     * Called by the ChatroomsFragment when a chatroom is selected.
     *
     * For two-pane UI, do nothing, but for single-pane, need to push the detail fragment.
     */
    public void setChatroom(Chatroom chatroom) {
        sharedViewModel.select(chatroom);
        if (!isTwoPane) {
            // For single pane, replace chatrooms fragment with messages fragment.
            // Add chatrooms fragment to backstack, so pressing BACK key will return to index.
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MessagesFragment(), SHOWING_MESSAGES_TAG)
                    .addToBackStack(SHOWING_CHATROOMS_TAG)
                    .commit();

        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Connected to the chat service.");
        chatService = ((ChatService.ChatBinder) service).getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "Disconnected from the chat service.");
        chatService = null;
    }
}
