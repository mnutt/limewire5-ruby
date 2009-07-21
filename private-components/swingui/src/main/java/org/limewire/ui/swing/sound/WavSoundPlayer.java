package org.limewire.ui.swing.sound;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public class WavSoundPlayer implements Runnable {
    private static final Log LOG = LogFactory.getLog(WavSoundPlayer.class);
    
    private final String filename;
    private final Position curPosition;

    private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
 
    enum Position {
        LEFT, RIGHT, NORMAL
    };
 
    public WavSoundPlayer(String wavfile) {
        filename = wavfile;
        curPosition = Position.NORMAL;
    }
 
    public WavSoundPlayer(String wavfile, Position p) {
        filename = wavfile;
        curPosition = p;
    }
 
    public void run() {
 
        File soundFile = new File(filename);
        if (!soundFile.exists()) {
            LOG.warnf("Wave file not found: {0}", filename);
            return;
        }
 
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (UnsupportedAudioFileException e1) {
            LOG.error("Bad audio file", e1);
            return;
        } catch (IOException e1) {
            LOG.error("", e1);
            return;
        }
 
        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine auline = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
 
        try {
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
        } catch (LineUnavailableException e) {
            LOG.error("Could not open audio file", e);
            return;
        } catch (Exception e) {
            LOG.error("", e);
            return;
        }
 
        if (auline.isControlSupported(FloatControl.Type.PAN)) {
            FloatControl pan = (FloatControl) auline
                    .getControl(FloatControl.Type.PAN);
            if (curPosition == Position.RIGHT)
                pan.setValue(1.0f);
            else if (curPosition == Position.LEFT)
                pan.setValue(-1.0f);
        } 
 
        auline.start();
        int nBytesRead = 0;
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
 
        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0)
                    auline.write(abData, 0, nBytesRead);
            }
        } catch (IOException e) {
            LOG.error("Error reading audio file", e);
        } finally {
            auline.drain();
            auline.close();
        }
    }
}