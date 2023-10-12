package edu.stevens.cs522.chatserver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.stevens.cs522.chatserver.R;
import edu.stevens.cs522.chatserver.entities.Peer;
import edu.stevens.cs522.chatserver.ui.TextAdapter;
import edu.stevens.cs522.chatserver.viewmodels.PeersViewModel;


public class ViewPeersActivity extends FragmentActivity implements TextAdapter.OnItemClickListener {

    /*
     *  See ChatServer for example of what to do, query peers database instead of messages database.
     */
    public static final String TAG = ViewPeersActivity.class.getCanonicalName();
    private TextAdapter<Peer> peerAdapter;
    private PeersViewModel peersViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_peers);

        Log.i(TAG, "(Re)starting View Peers Activity");

        RecyclerView peersList = findViewById(R.id.peer_list);
        peersList.setLayoutManager(new LinearLayoutManager(this));

        peerAdapter = new TextAdapter<>(peersList, this);
        peersList.setAdapter(peerAdapter);

        peersViewModel=new ViewModelProvider(this).get(PeersViewModel.class);

        peersViewModel.fetchAllPeers().observe(this, new Observer<List<Peer>>() {
            @Override
            public void onChanged(List<Peer> peers) {
                peerAdapter.setDataset(peers);
                peersList.setAdapter(peerAdapter);
            }
        });


        }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "Leaving View Peers Activity");
    }

    @Override
    public void onItemClick(RecyclerView parent, View view, int position) {

        Peer peer = peerAdapter.getItem(position);

        Intent intent = new Intent(this, ViewPeerActivity.class);
        intent.putExtra(ViewPeerActivity.PEER_KEY, peer);
        startActivity(intent);

    }
}
