package helper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gianluca on 29/10/2016.
 *
 * This class will be used by the app to deal with Firebase for everything that concerns
 * the rating request mechanism. It has the references to the Firebase Realtime Database values,
 * the references to the Firebase Remoteconfig values and the instances of the two classes.
 *
 * It also manages Firebase authentication if needed - in this case it's an anonymous authentication
 * but dealing with different methods should not be tricky.
 */

public class RatingPopupHelper {

    // 2nd: Create this class, singleton method and intance it in the Application class (to be create)

    // 3rd: talking about firebase nodes

    public final static String
        TAG = RatingPopupHelper.class.getName(),

        ROOT = "rating_popup",

        COUNTER_APP_OPEN_EVENT = "x_app_open",
        DAYS_SINCE_FIRST_OPEN = "x_days_since_first_open",
        COUNTER_FUNCTION_EVENT = "x_counter_function",
        COUNTER_GAME_EVENT = "x_counter_game",
        GAME_SCORE = "x_last_game_score",

        OBSERVED = "y_observed",

        ACTION = "action",

        REMOTE_LABEL_DATA = "label_data";

    private static RatingPopupHelper sInstance = null;

    // 3rd: all the variables I'm going to need but the last

    // Firebase user: used to get the user ID and to know whether we're authenticated or not
    private FirebaseUser mUser;

    // Realtime database
    private DatabaseReference mDb;

    // Remote config
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    // Action listener
    private ValueEventListener mPopupActionListener;

    private RatingPopupHelper() {

        // 4th: auth, why we need it etc

        // Auth
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mUser = firebaseAuth.getCurrentUser();
                if (mUser != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + mUser.getUid());

                    // Initialize Database reference
                    mDb = FirebaseDatabase.getInstance().getReference(ROOT).child(mUser.getUid());

                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    auth.signInAnonymously();
                }
            }
        });

        // 5th: remote config & setup remote config

        // Remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        setupRemoteConfig();
    }

    public static RatingPopupHelper getInstance() {
        if (sInstance == null)
            sInstance = new RatingPopupHelper();
        return sInstance;
    }

    // 6th: update function without available, then talk about available and write it
    public void updateValue(Context context, String node, Object value) {
        if (available(context)) {
            mDb.child(node)
                .setValue(value);
        }
    }

    // 9th: increment value
    public void incrementValue(Context context, final String node) {
        if (available(context)) {
            final DatabaseReference db = mDb.child(node);
            db.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int previous = dataSnapshot.exists() ?
                            dataSnapshot.getValue(Integer.class) : 0;
                        // I will not use a Transaction because in this case
                        // the node is not updated by more than one client,
                        // and with a simple setValue the code is much more
                        // readable
                        db.setValue(previous+1);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // No internet connection or access not allowed
                    }
                });
        }
    }

    // 8th: did rating
    private boolean didRating(Context context) {
        SharedPreferences s = context.getSharedPreferences(RatingPopupHelper.ROOT,
                Context.MODE_PRIVATE);
        return s.getBoolean("did_rating_popup", false);
    }

    // 7th: available -> didRating
    private boolean available(Context context) {
        return mUser != null && mDb != null && !didRating(context);
    }

    // SECOND STAGE:
    private void setRatingDone(Context context) {
        SharedPreferences s = context.getSharedPreferences(RatingPopupHelper.ROOT,
                Context.MODE_PRIVATE);
        s.edit().putBoolean("did_rating_popup", true).apply();
    }

    // SECOND STAGE: 2nd: when should the rating be shown then?
    public void addPopupActionListener(final Activity activity) {
        if (!available(activity))
            return;

        // Popup listener
        mPopupActionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int action = dataSnapshot.getValue(Integer.class);
                    if (action == 1) {
                        showRatingPopup(activity);
                    } // else: not the right moment yet
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing: either no connection, or no permissions
            }
        };

        mDb.child(ACTION).addValueEventListener(mPopupActionListener);
    }

    // SECOND STAGE: 3rd: remove it when don't needed anymore
    public void removePopupActionListener() {
        if (mPopupActionListener != null)
            mDb.child(ACTION).removeEventListener(mPopupActionListener);
    }

    // SECOND STAGE: 1st: with the other one
    private void showRatingPopup(Activity activity) {
        ratingPopupIfNeeded(activity);
    }

    // 11th: popup & observation when required
    // SECOND STAGE: 1st: getting rid of remote config and explicit this function
    /**
     * Retained the old name just for code history consistency
     */
    private void ratingPopupIfNeeded(final Activity activity) {

        if (available(activity)
            // [1st step] it was:
            // && mFirebaseRemoteConfig.getBoolean(REMOTE_LABEL_DATA)
            )
        {
            try {

                final AlertDialog popup = new AlertDialog.Builder(activity)
                        .setMessage("Would you be so kind to leave us a rating pls? :) ")
                        .setPositiveButton("YEAH! Luv u", null)
                        .setNegativeButton("Nope, go away", null)
                        .setCancelable(false)
                        .create();

                popup.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        popup.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        popup.dismiss();

                                        // Set the observed dependent variable as TRUE (1)
                                        mDb.child(OBSERVED).setValue(1);

                                        // Set locally rating as done
                                        setRatingDone(activity);

                                        // Go to store
                                        Toast.makeText(activity, "GO TO STORE! YEAH!", Toast.LENGTH_LONG)
                                                .show();
                                    }
                                });

                        popup.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        popup.dismiss();

                                        // Set the observed dependent variable as FALSE (0)
                                        mDb.child(OBSERVED).setValue(0);
                                    }
                                });
                    }
                });

                popup.show();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void setupRemoteConfig() {
        // Standard values
        Map<String, Object> standard = new HashMap<>();
        standard.put(REMOTE_LABEL_DATA, false);
        mFirebaseRemoteConfig.setDefaults(standard);

        mFirebaseRemoteConfig.fetch(0) // 3600, 3600*24, whatever
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Once the config is successfully fetched
                            // it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        }
                    }
                });
    }

}
