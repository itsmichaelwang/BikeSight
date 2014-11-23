/*
 * Copyright (C) 2014 Thalmic Labs Inc.
 * Distributed under the Myo SDK license agreement. See LICENSE.txt for details.
 */

package com.github.itsmichaelwang.bikesight.app;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

public class MainActivity extends Activity {

    // This code will be returned in onActivityResult() when the enable Bluetooth activity exits.
    private static final int REQUEST_ENABLE_BT = 1;

    private TextView mTextView;
    private ImageView mImageView;
    private Animation animation;

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {

        private Arm mArm = Arm.UNKNOWN;
        private XDirection mXDirection = XDirection.UNKNOWN;
        private Time startTime = null;
        private boolean isEngaged = false;
        private boolean isVibrating = false;
        private Pose currentPose;



        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Hide the status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

            // Hide the action bar as well
            ActionBar actionBar = getActionBar();
            actionBar.hide();

            // Set the text color of the text view to cyan when a Myo connects.
            mTextView.setTextColor(Color.CYAN);
        }

        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Show the status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);

            // Show the action bar
            ActionBar actionBar = getActionBar();
            actionBar.show();

            // Set the text color of the text view to red when a Myo disconnects.
            mTextView.setTextColor(Color.RED);
        }

        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm = arm;
            mXDirection = xDirection;

            mTextView.setText(arm == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
        }

        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
            mXDirection = XDirection.UNKNOWN;

            mTextView.setText(R.string.instructions);
        }

        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            TextView tvRoll = (TextView) findViewById(R.id.textView);
            TextView tvPitch = (TextView) findViewById(R.id.textView2);
            TextView tvYaw = (TextView) findViewById(R.id.textView3);

            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (mXDirection == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            tvRoll.setText(Float.toString(roll));
            tvPitch.setText(Float.toString(pitch));
            tvYaw.setText(Float.toString(yaw));

            float limit = 500;

            if (Math.abs(pitch) < 15 && currentPose == Pose.FIST) {
                if (startTime == null) {
                    startTime = new Time();
                    startTime.setToNow();
                } else {
                    Time now = new Time();
                    now.setToNow();
                    float holdTime = now.toMillis(true) - startTime.toMillis(true);
                    if (holdTime > limit) {
                        if (!isEngaged) {
                            myo.vibrate(Myo.VibrationType.MEDIUM);
                            isEngaged = true;
                        }
                        mTextView.setText("");
                        mImageView.setImageResource(R.drawable.left_arrow);
                        mImageView.startAnimation(animation);
                        startTime = now;
                    }
                }
            } else if (pitch < -50) {
                if (startTime == null) {
                    startTime = new Time();
                    startTime.setToNow();
                } else {
                    Time now = new Time();
                    now.setToNow();
                    float holdTime = now.toMillis(true) - startTime.toMillis(true);
                    if (holdTime > limit) {

                        if (!isEngaged) {
                            myo.vibrate(Myo.VibrationType.MEDIUM);
                            isEngaged = true;
                        }
                        mTextView.setText("");
                        mImageView.setImageResource(R.drawable.right_arrow);
                        mImageView.startAnimation(animation);
                        startTime = now;
                    }
                }
            } else {
                if (isEngaged) {
                    Time now = new Time();
                    now.setToNow();
                    float holdTime = now.toMillis(true) - startTime.toMillis(true);
                    if (holdTime > limit) {
                        mTextView.setText("");
                        mImageView.setImageResource(R.drawable.bike);
                        mImageView.clearAnimation();
                        startTime = null;
                        isEngaged = false;
                    }
                } else {
                    mTextView.setText("");
                    mImageView.setImageResource(R.drawable.bike);
                    mImageView.clearAnimation();
                    startTime = null;
                    isEngaged = false;
                }
            }
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            currentPose = pose;
            switch (pose) {
                case UNKNOWN:
                    break;
                case REST:
                    switch (mArm) {
                        case LEFT:
                            break;
                        case RIGHT:
                            break;
                    }
                    break;
                case FIST:
                    break;
                case WAVE_IN:
                    break;
                case WAVE_OUT:
                    break;
                case FINGERS_SPREAD:
                    break;
                case THUMB_TO_PINKY:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);
        mImageView = (ImageView) findViewById(R.id.imageView);

        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

        // Create animation
        animation = new AlphaAnimation(1, 0);
        animation.setDuration(250);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    @Override
    protected void onResume() {
        super.onResume();

        // If Bluetooth is not enabled, request to turn it on.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth, so exit.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }
}
