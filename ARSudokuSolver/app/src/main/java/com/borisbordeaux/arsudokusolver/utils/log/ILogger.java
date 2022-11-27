package com.borisbordeaux.arsudokusolver.utils.log;

public interface ILogger {
    /**
     * Logs given message using the given tag
     *
     * @param tag the tag to use
     * @param msg the message to be logged
     */
    void log(String tag, String msg);
}
