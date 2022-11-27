package com.borisbordeaux.arsudokusolver.utils.log;

import android.util.Log;

public class AndroidLogger implements ILogger {

    /**
     * {@inheritDoc} using the Android log system
     *
     * @param tag the tag to use
     * @param msg the message to be logged
     */
    @Override
    public void log(String tag, String msg) {
        Log.d(tag, msg);
    }
}
