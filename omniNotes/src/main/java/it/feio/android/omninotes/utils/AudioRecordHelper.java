package it.feio.android.omninotes.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import it.feio.android.omninotes.R;
import it.feio.android.omninotes.models.ONStyle;

/**
 * Created by Sanz01 on 14.01.2018.
 */

public class AudioRecordHelper {
  protected Context mContext;
  protected MediaRecorder mRecorder;
  protected String mRecordPath;
  protected long mAudioRecordingTimeStart;
  protected long mAudioRecordingTime;

  public AudioRecordHelper(Context filesContext){
    mContext = filesContext;
  }


  /**
   * Creates an inner MediaRecorder and a file on external memory.
   * */
  public void startRecording()
    throws IOException, IllegalStateException {

    // It creates a file not on sd in Android/data/<package/app>/files
    File f = StorageHelper.createNewAttachmentFile(mContext, Constants.MIME_TYPE_AUDIO_EXT);
    if (f == null) {
      throw new IOException("File did not make it.");
    }

    if (mRecorder == null) {
      mRecorder = new MediaRecorder();
      mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
      mRecorder.setAudioEncodingBitRate(96000);
      mRecorder.setAudioSamplingRate(44100);
    }
    mRecordPath = f.getAbsolutePath();
    mRecorder.setOutputFile(mRecordPath);

    try {
      mAudioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
      mRecorder.prepare();
      mRecorder.start();
    } catch (IOException | IllegalStateException e) {
      Log.e(Constants.TAG, "prepare() failed", e);
      throw e;
    }
  }


  public void stopRecording() {
    if (mRecorder != null) {
      mRecorder.stop();
      mAudioRecordingTime = Calendar.getInstance().getTimeInMillis() - mAudioRecordingTimeStart;
      mRecorder.release();
      mRecorder = null;
    }
  }

  public void destroyRecorder(){
    // in previous version this was made in fragment's onPause. Without stop(). Why?
    if (mRecorder != null) {
      mRecorder.release();
      mRecorder = null;
    }
  }



  public String getRecordPath(){ return mRecordPath; }
  public long getRecordingTime(){ return mAudioRecordingTime; }

}
