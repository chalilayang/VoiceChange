package com.chalilayang.speex.encode;

import com.chalilayang.speex.SpeexRecorder;
import com.chalilayang.speex.writer.SpeexWriter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SpeexEncoder implements Runnable {

    // private long ts;
    public static int encoder_packagesize = 4000;
    // private Logger log = LoggerFactory.getLogger(SpeexEncoder.class);
    private final Object mutex = new Object();
    List<ReadData> list = null;
    private Speex speex = new Speex();
    private byte[] processedData = new byte[encoder_packagesize];
    private volatile boolean isRecording;
    private String fileName;
    private SpeexRecorder mRecorder;

    public SpeexEncoder(String fileName) {
        super();
        speex.init();
        list = Collections.synchronizedList(new LinkedList<ReadData>());
        this.fileName = fileName;
    }

    public void setRecorder(SpeexRecorder recorder) {
        this.mRecorder = recorder;
    }

    public void run() {

        SpeexWriter fileWriter = new SpeexWriter(fileName);
        fileWriter.setRecorder(mRecorder);
        Thread consumerThread = new Thread((Runnable) fileWriter);
        fileWriter.setRecording(true);
        consumerThread.start();

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        int getSize = 0;
        while (this.isRecording()) {
            if (list.size() == 0) {
                // log.debug("no data need to do encode");
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            if (list.size() > 0) {
                synchronized (mutex) {
                    ReadData rawdata = list.remove(0);
                    getSize = speex.encode(rawdata.ready, 0, processedData, rawdata.size);

                    // log.info("after encode......................before=" +
                    // rawdata.size + " after=" + processedData.length +
                    // " getsize="
                    // + getSize);
                }
                if (getSize > 0) {
                    fileWriter.putData(processedData, getSize);
                    // log.info("............clear....................");
                    processedData = new byte[encoder_packagesize];
                }
            }
        }
        // log.debug("encode thread exit");
        fileWriter.setRecording(false);
    }

    public void putData(short[] data, int size) {
        if (size > 0) {
            ReadData rd = new ReadData();
            synchronized (mutex) {
                rd.size = size;
                System.arraycopy(data, 0, rd.ready, 0, size);
                list.add(rd);
            }
        }
    }

    public boolean isRecording() {
        synchronized (mutex) {
            return isRecording;
        }
    }

    public void setRecording(boolean isRecording) {
        synchronized (mutex) {
            this.isRecording = isRecording;
        }
    }

    class ReadData {
        private int size;
        private short[] ready = new short[encoder_packagesize];
    }
}
