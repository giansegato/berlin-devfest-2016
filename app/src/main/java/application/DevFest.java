package application;

import android.app.Application;

import helper.RatingPopupHelper;

/**
 * Created by gianluca on 29/10/2016.
 *
 * This class is an Application-level helper class that is instanced at the very
 * creation of the application, before the first Activity is launched.
 *
 * In this class we are going to instance the {@link RatingPopupHelper} class, that
 * will help us at the whole application level to deal with Firebase.
 *
 */

public class DevFest extends Application {

    private static DevFest sInstance;

    public RatingPopupHelper mRatingPopupHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // WARNING:
        // instancing the Firebase DB at this level causes a problem in the case
        // you want to use Firebase Crash Reporting,
        // because of threading. Be aware!
        mRatingPopupHelper = RatingPopupHelper.getInstance();

    }

    public static DevFest getInstance() {
        return sInstance;
    }

}
