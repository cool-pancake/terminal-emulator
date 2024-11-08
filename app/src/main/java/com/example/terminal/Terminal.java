package com.example.terminal;

import static com.example.terminal.JNI.close;
import static com.example.terminal.JNI.createPty;

import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;


public class Terminal {
    // Sequences codes
    private static final int NONE = 0;
    private static final int ESC = 1;
    private static final int CSI_START = '[';
    private static final int DCS_START = 'P';
    private static final int OSC_START = ']';

    private int currentSequence = NONE;
    private boolean continueSequence = false;
    private int[] sequencesArgs = new int[10];

    public boolean isCtrlChecked = false;

    private int row = 0;
    private int column = 0;
    private String terminalBuffer = "";

    private static final String LOG_TAG = "DebugTag";
    private FileDescriptor terminalFd = null;
    private TextView terminalTextView = null;

    Terminal(TextView textView) {
        // creating pty
        int pid = createPty();
        Log.d(LOG_TAG, "PID: " + pid);
        terminalFd = createTerminalFd(pid);
        terminalTextView = textView;

        // thread that is reading from terminal file descriptor and adding it to terminalOut TextView
        new Thread() {
            @Override
            public void run() {
                try (InputStream inputStream = new FileInputStream(terminalFd)) {
                    while(true) {
                        byte[] bytes = new byte[4096];
                        int read = inputStream.read(bytes);
                        //Log.d("DebugTag", "Read bytes " + read);
                        if (read <= 0) {
                            Log.d(LOG_TAG, "Closing pid");
                            close(pid);
                            break;
                        } else {
                            // parse bytes and add to output
                            for (byte b : bytes) {
                                parseByte(b);
                            }
                            // TODO: after successful parse it can be removed, but for now that's fine
                            terminalBuffer = new String(bytes, StandardCharsets.UTF_8).replaceAll("\0", "");

                            terminalTextView.append(terminalBuffer);
                            column = terminalBuffer.length();
                            row = terminalTextView.getLineCount();
                            Log.d(LOG_TAG, "Message:\n" + new String(bytes, StandardCharsets.UTF_8).replaceAll("\0", ""));
                            Log.d(LOG_TAG, "Row: " + row + ", Column: " + column);
                        }
                    }
                } catch (IOException e) {}
            }
        }.start();
    }

    @NonNull
    private FileDescriptor createTerminalFd(int pid) {
        FileDescriptor fd = new FileDescriptor();
        try {
            Field fdField;
            fdField = FileDescriptor.class.getDeclaredField("descriptor");
            fdField.setAccessible(true);
            fdField.set(fd, pid);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        return fd;
    }

    void writeBytes(byte[] bytes) {
        // writing command to pty
        try (OutputStream outputStream = new FileOutputStream(terminalFd)) {
            outputStream.write(bytes);
        } catch (Exception e) {}
    }

    void parseByte(byte b) {
        switch (b) {
            case 7: // BEL
                break;
            case 8: // BS
                break;
            case 9: // HT
                break;
            case 10: // LF
                break;
            case 11: // VT
                break;
            case 12: // FF
                break;
            case 13: // CR
                break;
            case 27: // ESC
                continueSequence = true;
                currentSequence = ESC;
            case 127: // DEL
                break;
            default:
                continueSequence = false;
                switch (currentSequence) {
                    case CSI_START: // CSI
                        switch (b) {

                        }
                        break;
                    case DCS_START: // DCS
                        currentSequence = NONE;
                        break;
                    case OSC_START: // OSC
                        currentSequence = NONE;
                        break;
                }
                if (!continueSequence) currentSequence = NONE;
                break;
        }
    }
}
