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

    public final static String
        TAG = RatingPopupHelper.class.getName(),

        ROOT = "rating_popup",

        FEATURES_ROOT = "x",
        DEPENDENT_VAR_ROOT = "y",
        ACTION = "action",

        COUNTER_APP_OPEN_EVENT = "app_open",
        DAYS_SINCE_FIRST_OPEN = "days_since_first_open",
        COUNTER_FUNCTION_EVENT = "counter_function",
        COUNTER_GAME_EVENT = "counter_game",
        GAME_SCORE = "last_game_score",

        OBSERVED = "observed",

        REMOTE_LABEL_DATA = "label_data";

    private static RatingPopupHelper sInstance = null;

    // Firebase user: used to get the user ID and to know whether we're authenticated or not
    private FirebaseUser mUser;

    // Realtime database
    private DatabaseReference mDb;

    // Remote config
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private RatingPopupHelper() {
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

        // Remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        setupRemoteConfig();
    }

    public static RatingPopupHelper getInstance() {
        if (sInstance == null)
            sInstance = new RatingPopupHelper();
        return sInstance;
    }

    public void updateValue(Context context, String node, Object value) {
        if (available(context)) {
            mDb.child(FEATURES_ROOT)
                .child(node)
                .setValue(value);
        }
    }

    public void incrementValue(Context context, final String node) {
        if (available(context)) {
            final DatabaseReference db = mDb
                .child(FEATURES_ROOT)
                .child(node);
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

    private boolean didRating(Context context) {
        SharedPreferences s = context.getSharedPreferences(RatingPopupHelper.ROOT,
                Context.MODE_PRIVATE);
        return s.getBoolean("did_rating_popup", false);
    }

    private boolean available(Context context) {
        return mUser != null && mDb != null && !didRating(context);
    }

    private void setRatingDone(Context context) {
        SharedPreferences s = context.getSharedPreferences(RatingPopupHelper.ROOT,
                Context.MODE_PRIVATE);
        s.edit().putBoolean("did_rating_popup", true).apply();
    }

    public void ratingPopupIfNeeded(final Activity activity) {

        if (available(activity) && // can proceed
            mFirebaseRemoteConfig.getBoolean(REMOTE_LABEL_DATA)) // and it's time to label data
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
                                        mDb.child(DEPENDENT_VAR_ROOT)
                                                .child(OBSERVED)
                                                .setValue(1);

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
                                        mDb.child(DEPENDENT_VAR_ROOT)
                                                .child(OBSERVED)
                                                .setValue(0);
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
