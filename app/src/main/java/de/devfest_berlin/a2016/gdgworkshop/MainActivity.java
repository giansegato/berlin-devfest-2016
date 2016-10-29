package de.devfest_berlin.a2016.gdgworkshop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView mLogs;
    private ScrollView mScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogs = (TextView) findViewById(R.id.main_txt_logs);
        mScroll = (ScrollView) findViewById(R.id.main_scrollview);
    }

    public void function(View view) {
        log("FUNCTION");
    }

    public void game(View view) {
        Random r = new Random();
        int score = r.nextInt(100);
        log("GAME - score: " + score);
    }

    public void log(String action) {
        mLogs.append(action + "\n");
        mScroll.fullScroll(View.FOCUS_DOWN);
    }

}
