package de.devfest_berlin.a2016.gdgworkshop;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Random;

import application.DevFest;
import helper.RatingPopupHelper;

public class MainActivity extends AppCompatActivity {

    // 1st: Integrate Firebase by authenticating the user etc

    // 10th: all data input feature

    private DevFest app = DevFest.getInstance();

    private TextView mLogs;
    private ScrollView mScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Feature update
        app.mRatingPopupHelper.incrementValue(this, RatingPopupHelper.COUNTER_APP_OPEN_EVENT);
        SharedPreferences s = getSharedPreferences(RatingPopupHelper.ROOT, Context.MODE_PRIVATE);
        long time = s.getLong("first_open", 0);
        if (time == 0) {
            // First time I open the app: I record the time
            s.edit()
                    .putLong("first_open", System.currentTimeMillis())
                    .apply();
        } else {
            app.mRatingPopupHelper.updateValue(this,
                    RatingPopupHelper.DAYS_SINCE_FIRST_OPEN,
                    ((int) Math.round((System.currentTimeMillis() - time) / (double) 86400000) + 1)
            );
        }

        // Setup listener


        // Views
        mLogs = (TextView) findViewById(R.id.main_txt_logs);
        mScroll = (ScrollView) findViewById(R.id.main_scrollview);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // FINAL FIRST STAGE: 12th: call each time activity is called
        // Show rating popup if needed
        // [1st step] it was: app.mRatingPopupHelper.ratingPopupIfNeeded(MainActivity.this);
        // Now:

        // SECOND STAGE: 4th: register the observer
        app.mRatingPopupHelper.addPopupActionListener(MainActivity.this);
    }

    @Override
        protected void onPause() {
            super.onPause();

        // SECOND STAGE:  4th: unregister the observer
        app.mRatingPopupHelper.removePopupActionListener();
    }

    public void function(View view) {
        log("FUNCTION");

        // Feature update
        app.mRatingPopupHelper.incrementValue(this, RatingPopupHelper.COUNTER_FUNCTION_EVENT);
    }

    public void game(View view) {
        Random r = new Random();
        int score = r.nextInt(100);
        log("GAME - score: " + score);

        // Features update
        app.mRatingPopupHelper.incrementValue(this, RatingPopupHelper.COUNTER_GAME_EVENT);
        app.mRatingPopupHelper.updateValue(this, RatingPopupHelper.GAME_SCORE, score);
    }

    public void log(String action) {
        mLogs.append(action + "\n");
        mScroll.fullScroll(View.FOCUS_DOWN);
    }

}
