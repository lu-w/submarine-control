package com.cvoltidioten.submarinecontrol;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

/**
 * The main activity which displays an overview of the submarine data. If the submarine sends new
 * data information such as battery status, this view is updated automatically.
 * From this view, the user can start a new dive.
 */
public class MainActivity extends AppCompatActivity implements SubmarineConnector.SubmarineConnectionNotifyable, SubmarineConnector.SubmarineMessageNotifyable {
    private static final String TAG = "Main Activity";

    // The submarine which was obtained from the start activity.
    private static Submarine submarine;
    // The sidebar drawer toggle.
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sets up toolbar and drawer.
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.main_drawer_layout);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawer,
                    toolbar,
                    R.string.drawer_open,
                    R.string.drawer_close
            );
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawer.setDrawerListener(drawerToggle);
        }
        NavigationView navView = ((NavigationView) findViewById(R.id.main_drawer_navigation));
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        return selectDrawerItem(menuItem);
                    }
                });
        View header = LayoutInflater.from(this).inflate(R.layout.drawer_header, null);
        navView.addHeaderView(header);
        ((ImageView)(header.findViewById(R.id.drawer_image))).setImageResource(R.drawable.logo);

        // Sets up submarine information.
        if(submarine != null) {
            ((TextView) header.findViewById(R.id.drawer_text)).setText(submarine.getName());
            submarine.registerMessageReceiver(TAG, this);
            submarine.registerConnectionStatusReceiver(TAG, this);
        } else {
            ((TextView) header.findViewById(R.id.drawer_text)).setText(R.string.default_submarine_name);
        }
        updateSubmarineData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(submarine != null) {
            submarine.removeConnectionStatusReceiver(TAG);
            submarine.removeMessageReceiver(TAG);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(submarine != null) {
            // Kills all remaining threads on stop.
            submarine.removeConnectionStatusReceiver(TAG);
            submarine.removeMessageReceiver(TAG);
            submarine.disconnect();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Called when the user selects an item from the sidebar drawer.
     * @param item The item the user clicked.
     * @return True if the item was a valid item we can handle.
     */
    public boolean selectDrawerItem(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.main_nav_home:
                ((DrawerLayout)findViewById(R.id.main_drawer_layout)).closeDrawers();
                return true;
            case R.id.main_nav_previous_dives:
                // TODO start new intent to previous dive activity
                return true;
            case R.id.main_nav_new_dive:
                ((DrawerLayout)findViewById(R.id.main_drawer_layout)).closeDrawers();
                newDive(null);
                return true;
            case R.id.main_nav_settings:
                // TODO start new intent to settings activity
                return true;
            default:
                return false;
        }
    }

    /***********************************************************************************************
     * SUBMARINE CALLBACK METHODS
     **********************************************************************************************/

    /**
     * Updates the submarine data according to the new status.
     * @param status The new status of the submarine.
     */
    public void receiveConnectionStatus(boolean status) {
        if(status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Case: Submarine was diving and is now available again.
                    if(submarine != null && submarine.getStatus() == SubmarineProtos.Status.StatusType.DIVING) {
                        // We then ask for a status update of the submarine.
                        submarine.updateStatus();
                        updateSubmarineData();
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    submarineDisconnect(null);
                }
            });
        }
    }

    /**
     * If we get a status message, we update the submarine information accordingly.
     * @param message The message to handle.
     */
    public void receiveMessage(final SubmarineProtos.SubmarineMessage message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(message.getType()) {
                    case STATUS:
                        updateSubmarineData();
                        break;
                }
            }
        });
    }

    public static void setSubmarine(Submarine _submarine) {
        submarine = _submarine;
    }

    /***********************************************************************************************
     * BUTTON HANDLER
     **********************************************************************************************/

    /**
     * Refreshs the card with the submarine data. Called when the user presses the refresh button.
     * @param view Is ignored.
     */
    public void submarineRefresh(View view) {
        updateSubmarineData();
    }

    /**
     * Called when the user presses the disconnect button.
     * @param view Is ignored.
     */
    public void submarineDisconnect(View view) {
        if(submarine != null) {
            submarine.disconnect();
            submarine = null;
        }
        Intent intent = new Intent(this, StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Kills this activity.
        startActivity(intent);
    }

    /**
     * Called when the user presses the edit button.
     * @param view Is ignored.
     */
    public void submarineEdit(View view) {
        // TODO show submarine edit dialog
    }

    /**
     * Called when the user presses the new dive button.
     * @param view
     */
    public void newDive(View view) {
        NewDiveDialog newDiveDialog = new NewDiveDialog();
        newDiveDialog.setSubmarine(submarine);
        newDiveDialog.setMainActivity(this);
        newDiveDialog.show(getFragmentManager(), "New dive");
    }

    /**
     * Called when the user presses the cancel dive button.
     * @param view
     */
    public void cancelDive(View view) {
        if(submarine != null) {
            submarine.cancelDive();
        }
        updateSubmarineData();
    }

    /**
     * Updates the view containing all the submarine data with the current data of the submarine
     * object.
     */
    protected void updateSubmarineData() {
        if(submarine != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.submarine_title)).setText(submarine.getName());
                    TextView statusView = (TextView) findViewById(R.id.submarine_status);
                    switch (submarine.getStatus()) {
                        case AVAILABLE:
                            findViewById(R.id.cancel_dive_button).setVisibility(View.INVISIBLE);
                            findViewById(R.id.dive_schedule_bar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.new_dive_button).setVisibility(View.VISIBLE);
                            statusView.setText(R.string.online);
                            break;
                        case DIVE_SCHEDULED:
                            findViewById(R.id.cancel_dive_button).setVisibility(View.VISIBLE);
                            findViewById(R.id.dive_schedule_bar).setVisibility(View.VISIBLE);
                            findViewById(R.id.new_dive_button).setVisibility(View.INVISIBLE);
                            statusView.setText(R.string.dive_scheduled);
                            break;
                        case DIVING:
                            findViewById(R.id.cancel_dive_button).setVisibility(View.INVISIBLE);
                            findViewById(R.id.dive_schedule_bar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.new_dive_button).setVisibility(View.INVISIBLE);
                            statusView.setText(R.string.diving);
                            break;
                        default:
                            findViewById(R.id.cancel_dive_button).setVisibility(View.INVISIBLE);
                            findViewById(R.id.dive_schedule_bar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.new_dive_button).setVisibility(View.INVISIBLE);
                            statusView.setText(R.string.offline);
                            break;
                    }
                    ((TextView) findViewById(R.id.submarine_battery)).setText(submarine.getBatteryPercentage() + getResources().getString(R.string.percent));
                    if(submarine.getDives().size() > 0 && submarine.getDives().get(0).getData() != null && submarine.getDives().get(submarine.getDives().size() - 1).getData().size() > 0) {
                        ((TextView) findViewById(R.id.submarine_last_dive)).setText(submarine.getDives().get(0).getStartingTime().toString());
                        ((TextView) findViewById(R.id.submarine_temperature)).setText(new DecimalFormat("##.#").format(submarine.getDives().get(submarine.getDives().size() - 1).getData().get(0).getTemperatureC()) + R.string.degc);
                    } else {
                        ((TextView) findViewById(R.id.submarine_last_dive)).setText(R.string.no_dive);
                        ((TextView) findViewById(R.id.submarine_temperature)).setText(R.string.default_temp);
                    }
                }
            });
        }
    }

    /**
     * Is called by the new dive dialog in case the timer of the new dive is expired. We disconnect
     * from the submarine and display the diving activity.
     */
    protected void beginDive() {
        if(submarine != null) {
            submarine.removeMessageReceiver(TAG);
            submarine.removeConnectionStatusReceiver(TAG);
            submarine.disconnect();
        }
        Intent intent = new Intent(this, DiveActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Kills this activity.
        startActivity(intent);
        finish();
    }
}
