package edu.stevens.cs522.chatserver.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.contracts.MessageContract;
import edu.stevens.cs522.chatserver.contracts.PeerContract;
import edu.stevens.cs522.chatserver.entities.Peer;


public class ViewPeersActivity extends FragmentActivity implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    /*
     * See ChatServer for example of what to do, query peers database instead of messages database.
     */

    private SimpleCursorAdapter peerAdapter;

    static final private int LOADER_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_peers);

        // use SimpleCursorAdapter (with flags=0 and null initial cursor) to display the chat peers.
        /*
         * You can use android.R.simple_list_item_1 as layout for each row.
         *
         * peersAdapter = new SimpleCursorAdapter(..., 0);
         */
        peerAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                null,
                new String[] { PeerContract.NAME },
                new int[] { android.R.id.text1 },
                0
        );


        // set item click listener to this activity
        ListView peerListView = findViewById(R.id.peer_list);
        peerListView.setAdapter(peerAdapter);
        peerListView.setOnItemClickListener(this);

        //  Use loader manager to initiate a query of the database
        // Make sure to use the Jetpack library, not the deprecated core implementation.
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*
         * Clicking on a peer brings up details
         */
        Cursor cursor = peerAdapter.getCursor();
        if (cursor.moveToPosition(position)) {
            Intent intent = new Intent(this, ViewPeerActivity.class);
            Peer peer = new Peer(cursor);
            intent.putExtra(ViewPeerActivity.PEER_KEY, peer);
            startActivity(intent);
        } else {
            throw new IllegalStateException("Unable to move to selected position in cursor: "+position);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case LOADER_ID:
                // use a CursorLoader to initiate a query on the database
                // Use PeerContact.CONTENT_URI to specify the content
                String[] projection = {
                        PeerContract._ID,
                        PeerContract.NAME,
                        PeerContract.TIMESTAMP,
                        PeerContract.LATITUDE,
                        PeerContract.LONGITUDE
                };
                return  new CursorLoader(
                        this,
                        PeerContract.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                );

            default:
                throw new IllegalStateException(("Unexpected loader id: "+id));
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // populate the UI with the result of querying the provider
        peerAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //  reset the UI when the cursor is empty
        peerAdapter.swapCursor(null);

    }

}
