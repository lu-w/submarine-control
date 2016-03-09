package com.cvoltidioten.submarinecontrol;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class DiveActivity extends AppCompatActivity implements SubmarineConnector.SubmarineConnectionNotifyable, SubmarineConnector.SubmarineMessageNotifyable {
    private final static String TAG = "DiveActivity";

    private Submarine submarine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dive);
        try {
            this.submarine = new Submarine();
        } catch(SubmarineBluetoothConnector.HardwareException hwe) {
            Log.v(TAG, "Unable to connect to submarine", hwe);
        }

        ((Toolbar)findViewById(R.id.toolbar)).setTitle("Diving");

        final ImageView submarineView = (ImageView)findViewById(R.id.submarine_outline);
        final int newMargin = 200;
        Animation diveAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)submarineView.getLayoutParams();
                params.topMargin = (int)(newMargin * interpolatedTime);
                submarineView.setLayoutParams(params);
            }
        };
        diveAnimation.setDuration(10000); // in ms
        diveAnimation.setRepeatMode(Animation.REVERSE);
        diveAnimation.setRepeatCount(Animation.INFINITE);
        submarineView.startAnimation(diveAnimation);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(this.submarine != null) {
            this.submarine.removeConnectionStatusReceiver(TAG);
            this.submarine.removeMessageReceiver(TAG);
        }
    }

    @Override
    public void receiveConnectionStatus(boolean status) {
        if(status) {
            submarine.updateStatus();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.data_progress_bar).setVisibility(View.INVISIBLE);
                    findViewById(R.id.data_button).setVisibility(View.VISIBLE);
                }
            });
        }
    }

            @Override
    public void receiveMessage(SubmarineProtos.SubmarineMessage message) {
        if(message.getType() == SubmarineProtos.SubmarineMessage.MessageType.STATUS) {
            submarine.updateData();
        } else if(message.getType() == SubmarineProtos.SubmarineMessage.MessageType.DATA) {
            final DiveActivity thisActivity = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(thisActivity, DataActivity.class);
                    // Passes data to data activity.
                    if(submarine != null) {
                        intent.putExtra("dive", (Parcelable)submarine.getDives().get(submarine.getDives().size() - 1));
                    }
                    if(submarine != null) {
                        submarine.removeConnectionStatusReceiver(TAG);
                        submarine.removeMessageReceiver(TAG);
                        submarine.disconnect();
                        submarine = null;
                    }
                    startActivity(intent);
                }
            });
        }
    }

    public void getData(View view) {
        if(this.submarine != null) {
            findViewById(R.id.data_progress_bar).setVisibility(View.VISIBLE);
            findViewById(R.id.data_button).setVisibility(View.INVISIBLE);
            this.submarine.registerMessageReceiver(TAG, this);
            this.submarine.registerConnectionStatusReceiver(TAG, this);
            this.submarine.connect();
        }
    }
}
