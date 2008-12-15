package com.limegroup.gnutella.downloader;

/** 
 * Thrown if we can't resume to a file, i.e., because it's not a valid
 * incomplete file. 
 */
public class CantResumeException extends Exception {
    private String _file;

    /** @param f the name of the file that couldn't be resumed */
    public CantResumeException(String file) {
        this._file=file;
    }

    /** Returns the name of the file that couldn't be resumed. */
    public String getFilename() {
        return _file;
    }
}
