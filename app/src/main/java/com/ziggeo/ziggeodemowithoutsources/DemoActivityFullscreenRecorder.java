package com.ziggeo.ziggeodemowithoutsources;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ziggeo.androidsdk.Ziggeo;
import com.ziggeo.androidsdk.net.HttpStatusCodes;
import com.ziggeo.androidsdk.net.rest.ProgressCallback;
import com.ziggeo.androidsdk.net.rest.exceptions.RestResponseException;
import com.ziggeo.androidsdk.recording.VideoRecordingCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class DemoActivityFullscreenRecorder extends Activity implements ProgressCallback {

    public static final String TAG = DemoActivityFullscreenRecorder.class.getSimpleName();

    public static final String APP_TOKEN = ""; // TODO place your token here

    private Ziggeo mZiggeo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mZiggeo = new Ziggeo(APP_TOKEN);

        long maxDuration = 20000L;
        mZiggeo.setSendImmediately(false);
        mZiggeo.setTurnOffCameraWhileUploading(true);
        mZiggeo.setColorForStoppedCameraOverlay(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        HashMap<String, String> args = new HashMap<>();
        args.put("key", "key_for_video");
        args.put("tags", "tag_for_video1, tag_for_video2");
        mZiggeo.setExtraArgsForCreateVideo(args);
        mZiggeo.createVideo(this, maxDuration, this);
        mZiggeo.setVideoRecordingProcessCallback(new VideoRecordingCallback() {
            @Override
            public void onStarted() {
                Log.d(TAG, "onStarted");
            }

            @Override
            public void onStopped(String path) {
                Log.d(TAG, "onStopped");
            }

            @Override
            public void onError() {
                Log.d(TAG, "onError");
            }
        });
    }

    @Override
    public void onProgressUpdate(int percent) {
        Log.e(TAG, "Progress:" + percent);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.e(TAG, "Request failure. Exception:" + e.toString());
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
            String responseString = response.body().string();
            Log.d(TAG, "Request success:" + responseString);

//            do something here

        } else {
            RestResponseException exception = new RestResponseException(
                    response.code(), response.message()
            );
            onFailure(call, exception);
        }
    }

    private void startVideoRecordingWithDelay() {
        // starting preview with delay
        final long previewDelay = 3000L; // 3 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final long autostartRecording = 5000L; // after 5 seconds
                mZiggeo.setAutostartRecordingAfter(autostartRecording);
                mZiggeo.createVideo(DemoActivityFullscreenRecorder.this, 20000, DemoActivityFullscreenRecorder.this);
            }
        }, previewDelay);
    }

    /**
     * Simple video file uploading.
     */
    private void createVideoApiCallExample(Callback callback) {
        String videoPath = "";
        File file = new File(videoPath);
        if (file.exists()) {
            HashMap<String, String> argsMap = new HashMap<>();
            argsMap.put("tags", "tag1,tag2");
            argsMap.put("key", "The best video ever");

            mZiggeo.videos().create(file, argsMap, callback);
        } else {
            Log.e(TAG, "Video file doesn't exists");
        }
    }

    /**
     * Will update video data, not video file itself.
     * To update video file you need to delete old video and create new one.
     */
    private void updateVideoApiCallExample(String videoToken, Callback callback) {
        HashMap<String, String> argsMap = new HashMap<>();
        argsMap.put("tags", "newTag");

        mZiggeo.videos().update(videoToken, argsMap, callback);
    }

    /**
     * Will delete selected video.
     * Make sure your application was allowed to do delete.
     */
    private void deleteVideoApiCallExample(String videoToken, Callback callback) {
        mZiggeo.videos().delete(videoToken, callback);
    }

    /**
     * Get list with video
     * Make sure your application was allowed to do index
     */
    private void index(Callback callback) {
        HashMap<String, String> args = new HashMap<>();
        args.put("tags", "your_tags_here");
        mZiggeo.videos().index(args, callback);
    }

    /**
     * Here is example how we can combine video and stream services
     * and use this combination for upload custom preview image for video.
     * <p/>
     * How we will do it
     * 1. Create video object
     * 2. Create stream for it
     * 3. Attach image to stream
     * 4. Attach video to stream
     * 5. Bind the stream.
     */
    private File mVideoFile;
    private File mSnapshotFile;
    private String mVideoToken;
    private String mStreamToken;

    private void uploadVideoWithCustomSnapshotExample() {
        String videoFilePath = "";
        String snapshotImagePath = "";
        mVideoFile = new File(videoFilePath);
        mSnapshotFile = new File(snapshotImagePath);
        // make sure files exists
        if (mVideoFile.exists() && mSnapshotFile.exists()) {
            HashMap<String, String> args = new HashMap<>();
            args.put("tags", "t1,t2");
            args.put("key", "test_key");

            mZiggeo.videos().create(args, mCreateVideoCallback);
        } else {
            Log.e(TAG, "Video or snapshot file not found");
        }
    }

    private Callback mCreateVideoCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Create video object failure. Exception:" + e.toString());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
                    String responseString = response.body().string();
                    Log.d(TAG, "Create video object success:" + responseString);

                    final JSONObject responseJson = new JSONObject(responseString);
                    mVideoToken = responseJson.getString("token");

                    mZiggeo.streams().create(mVideoToken, mCreateStreamCallback);

                } else {
                    RestResponseException exception = new RestResponseException(
                            response.code(), response.message()
                    );
                    onFailure(call, exception);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    private Callback mCreateStreamCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Create stream failure. Exception:" + e.toString());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
                    String responseString = response.body().string();
                    Log.d(TAG, "Create stream success:" + responseString);

                    JSONObject responseJson = new JSONObject(responseString);
                    mStreamToken = responseJson.getString("token");

                    mZiggeo.streams().attachImage(mVideoToken, mStreamToken, mSnapshotFile,
                            mAttachImageToStreamCallback);

                } else {
                    RestResponseException exception = new RestResponseException(
                            response.code(), response.message()
                    );
                    onFailure(call, exception);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    private Callback mAttachImageToStreamCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Attach image failure. Exception:" + e.toString());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
                    String responseString = response.body().string();
                    Log.d(TAG, "Attach image success:" + responseString);

                    mZiggeo.streams().attachVideo(mVideoToken, mStreamToken, mVideoFile,
                            mAttachVideoToStreamCallback);

                } else {
                    RestResponseException exception = new RestResponseException(
                            response.code(), response.message()
                    );
                    onFailure(call, exception);
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

    };

    private Callback mAttachVideoToStreamCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Attach video failure. Exception:" + e.toString());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
                    String responseString = response.body().string();
                    Log.d(TAG, "Attach video success:" + responseString);

                    mZiggeo.streams().bind(mVideoToken, mStreamToken, mBindStreamCallback);

                } else {
                    RestResponseException exception = new RestResponseException(
                            response.code(), response.message()
                    );
                    onFailure(call, exception);
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

    };

    private Callback mBindStreamCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Bind stream failure. Exception:" + e.toString());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                if (HttpStatusCodes.isSuccess(response.code()) && response.body() != null) {
                    String responseString = response.body().string();

                    Log.d(TAG, "Bind stream success:" + responseString);

                } else {
                    RestResponseException exception = new RestResponseException(
                            response.code(), response.message()
                    );
                    onFailure(call, exception);
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

}
