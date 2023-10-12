package edu.stevens.cs522.chatserver.databases;

import static edu.stevens.cs522.chatserver.activities.ChatServerActivity.TAG;

import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import edu.stevens.cs522.chatserver.entities.Peer;

/*
 * add annotations (NB insert should ignore conflicts, for upsert)
 *
 * We will continue to allow insertion to be done on main thread for noew.
 */
@Dao
public abstract class PeerDao {

    /**
     * Get all peers in the database.
     * @return
     */
    @Query("SELECT * FROM peer")
    public abstract LiveData<List<Peer>> fetchAllPeers();

    /**
     * Get the database primary key for a peer, based on chat name.
     * @param name
     * @return
     */
    @Query("SELECT id FROM peer WHERE name = :name")
    protected abstract long getPeerId(String name);

    /**
     *  Insert a peer and return their primary key (must not already be in database)
     * @param peer
     * @return
     */
    @Insert
    protected abstract void insert(Peer peer);

    /**
     * Update the metadata for a peer (GPS coordinates, last seen)
     * @param peer
     */
    @Update
    protected abstract void update(Peer peer);

    @Transaction
    /**
     * Add a peer record if it does not already exist;
     * update information if it is already defined.
     * This operation must be transactional, to avoid race condition
     * between search and insert
     */
    public void upsert(Peer peer) {
        long id = getPeerId(peer.name);
        if (id ==0) {
            insert(peer);
            Log.d(TAG, "Peer Data Upserted.");


        } else {
            peer.id=id;
            Log.d(TAG, "Peer Data Updated.");
            update(peer);


        }
    }
}
