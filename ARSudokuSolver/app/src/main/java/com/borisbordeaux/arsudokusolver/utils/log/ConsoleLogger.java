package com.borisbordeaux.arsudokusolver.utils.log;

public class ConsoleLogger implements ILogger {

    /**
     * {@inheritDoc} using the console log system
     *
     * @param tag the tag to use
     * @param msg the message to be logged
     */
    @Override
    public void log(String tag, String msg) {
        System.out.println(tag + ": " + msg);
    }
}
