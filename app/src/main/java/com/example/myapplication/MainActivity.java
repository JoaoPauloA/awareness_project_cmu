package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final String FENCE_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";
    private final String MOVIING_FENCE_KEY = "moving_fence_key";
    private final String IDLE_FENCE_KEY = "idle_fence_key";

    public String fenceValue = "";

    private final String TAG = getClass().getSimpleName();

    private PendingIntent mPendingIntent;

    private FenceReceiver mFenceReceiver;



    GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.content.Context context;
        client = new GoogleApiClient.Builder(this)
                .addApi(Awareness.API)
                .build();
        client.connect();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        mFenceReceiver = new FenceReceiver();
        registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFences();
    }

    @Override
    protected void onPause() {
        // Unregister the fence:
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(MOVIING_FENCE_KEY)
                .removeFence(IDLE_FENCE_KEY)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be unregistered: " + e);
                    }
                });

        super.onPause();
    }

    public void activityPerformed(View v) {
        TextView t = findViewById(R.id.testResult);
        Awareness.getSnapshotClient(this).getDetectedActivity()
                .addOnSuccessListener(new OnSuccessListener<DetectedActivityResponse>() {
                    @Override
                    public void onSuccess(DetectedActivityResponse dar) {
                        ActivityRecognitionResult arr = dar.getActivityRecognitionResult();
                        DetectedActivity probableActivity = arr.getMostProbableActivity();
                        int confidence = probableActivity.getConfidence();
                        String activityStr = probableActivity.toString();
                        t.setText(activityStr);
                    }
                });
    }

    public void detect_walk(View v){
        onPause();
        onResume();
        TextView t = findViewById(R.id.testResult);
        t.setText(mFenceReceiver.fenceStateStr);
    }

    private void setupFences() {
        AwarenessFence movingFence = DetectedActivityFence.during(DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING, DetectedActivity.WALKING);
        AwarenessFence idleFence = DetectedActivityFence.during(DetectedActivity.STILL);

        Awareness.getFenceClient(getApplicationContext()).updateFences(new FenceUpdateRequest.Builder()
                .addFence(MOVIING_FENCE_KEY, movingFence, mPendingIntent)
                .addFence(IDLE_FENCE_KEY, idleFence, mPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be registered: " + e);
                    }
                });
    }


    public class FenceReceiver extends BroadcastReceiver {
        private static final String TAG = "FenceBroadcastReceiver";
        public String fenceStateStr = "";
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                Toast.makeText(context, "Received an unsupported action in FenceReceiver: action="
                        + intent.getAction(), Toast.LENGTH_LONG).show();
                return;
            }
            FenceState fenceState = FenceState.extract(intent);


            if (TextUtils.equals(fenceState.getFenceKey(), MOVIING_FENCE_KEY)) {
                TextView t = findViewById(R.id.testResult);
                switch (fenceState.getCurrentState()) {

                    case FenceState.TRUE:
                        fenceStateStr = "Moving";
                         fenceValue = "Moving";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "Not Moving";
                        fenceValue = "Not Moving";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        fenceValue = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                        fenceValue = "unknown value";

                }
            }

            if (TextUtils.equals(fenceState.getFenceKey(), IDLE_FENCE_KEY)) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "Idle";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "Not Idle";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
            }
            Toast.makeText(context, "Fence state: " + fenceStateStr, Toast.LENGTH_LONG).show();
        }
    }

}