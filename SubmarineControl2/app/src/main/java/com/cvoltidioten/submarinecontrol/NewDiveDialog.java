package com.cvoltidioten.submarinecontrol;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A dialog which allows the user to issue a new dive. It contains two input fields, one for the
 * depth and one for the offset. On "okay", it starts a timer which represents the dive scheduler.
 * On expiration, the main acitivity is informed by the timer. It also adds the entered dive to the
 * submarine object.
 */
public class NewDiveDialog extends DialogFragment {
    private static final String TAG = "New Dive Dialog";

    private Submarine submarine;
    private MainActivity mainActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater linf = LayoutInflater.from(getActivity());
        final View inflator = linf.inflate(R.layout.new_dive_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.new_dive)
                .setView(inflator)
                .setPositiveButton(R.string.start_dive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String depth = ((EditText)inflator.findViewById(R.id.new_dive_depth)).getText().toString();
                        String offset = ((EditText)inflator.findViewById(R.id.new_dive_offset)).getText().toString();
                        // Validation
                        if(!depth.matches("^-?\\d+$")) {
                            ((EditText)inflator.findViewById(R.id.new_dive_depth)).setError("Please insert an integer.");
                            return;
                        }
                        if(!depth.matches("^-?\\d+$")) {
                            ((EditText)inflator.findViewById(R.id.new_dive_offset)).setError("Please insert an integer.");
                            return;
                        }
                        if (submarine != null) {
                            final int depthM = Integer.parseInt(depth);
                            final int offsetS = Integer.parseInt(offset);
                            Dive dive = new Dive(depthM, offsetS);
                            submarine.dive(dive);
                            ((TextView) getActivity().findViewById(R.id.submarine_status)).setText(R.string.dive_scheduled);
                            final ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.dive_schedule_bar);
                            getActivity().findViewById(R.id.new_dive_button).setVisibility(View.INVISIBLE);
                            getActivity().findViewById(R.id.cancel_dive_button).setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setMax(offsetS * 100);
                            new Thread() {
                                @Override
                                public void run() {
                                    int time = 0;
                                    while(time < offsetS * 100) {
                                        try {
                                            sleep(10);
                                            time++;
                                            progressBar.setProgress(time);
                                        } catch (InterruptedException e) {
                                            Log.e(TAG, "Progress bar update: ", e);
                                        }
                                    }
                                    if(submarine.getStatus() != SubmarineProtos.Status.StatusType.AVAILABLE) {
                                        submarine.setStatus(SubmarineProtos.Status.StatusType.DIVING);
                                        mainActivity.beginDive();
                                    }
                                    if(mainActivity != null) {
                                        mainActivity.updateSubmarineData();
                                    }
                                }
                            }.start();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public void setSubmarine(Submarine submarine) {
        this.submarine = submarine;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

}
