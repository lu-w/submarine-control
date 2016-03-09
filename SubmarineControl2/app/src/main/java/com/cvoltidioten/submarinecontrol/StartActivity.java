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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

/**
 * Represents the activity which is displayed to the user initially. It allows to connect to the
 * submarine. If the submarine is found, we redirect to the main activity. If not, the user is
 * allowed to issue another discovery.
 */
public class StartActivity extends AppCompatActivity implements SubmarineConnector.SubmarineConnectionNotifyable {
    private static final String TAG = "Start Activity";

    // The submarine to search for.
    private Submarine submarine;
    // The sidebar navigation.
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.start_drawer_layout);
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
            // Set the drawer toggle as the DrawerListener
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawer.setDrawerListener(drawerToggle);
        }

        ((NavigationView)findViewById(R.id.start_drawer_navigation)).setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        return selectDrawerItem(menuItem);
                    }
                });
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

    @Override
    public void onPause() {
        super.onPause();
        if(this.submarine != null) {
            this.submarine.removeConnectionStatusReceiver(TAG);
        }
    }

    /**
     * Called when the user selects an item from the sidebar drawer.
     * @param item The item the user clicked.
     * @return True if the item was a valid item we can handle.
     */
    public boolean selectDrawerItem(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.start_nav_home:
                ((DrawerLayout)findViewById(R.id.start_drawer_layout)).closeDrawers();
                return true;
            case R.id.start_nav_previous_dives:
                // TODO start new intent to previous dive activity
                return true;
            case R.id.start_nav_settings:
                // TODO start new intent to settings activity
                return true;
            default:
                return false;
        }
    }

    /***********************************************************************************************
     * BUTTON HANDLER
     **********************************************************************************************/

    /**
     * Called when the user pressed on the discover submarine button.
     * @param view Is ignored.
     */
    public void discoverSubmarine(View view) {
        try {
            findViewById(R.id.discover_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.cancel_button).setVisibility(View.VISIBLE);
            findViewById(R.id.discover_progress_bar).setVisibility(View.VISIBLE);
            this.submarine = new Submarine();
            this.submarine.registerConnectionStatusReceiver(TAG, this);
            this.submarine.connect();
        } catch(SubmarineBluetoothConnector.HardwareException hwe) {
            Log.v(TAG, "Unable to connect to submarine", hwe);
        }
    }

    /**
     * Called when the user cancels the ongoing discovery.
     * @param view Is ignored.
     */
    public void cancelDiscovery(View view) {
        if(this.submarine != null) {
            this.submarine.disconnect();
            this.submarine = null;
        }
        findViewById(R.id.discover_button).setVisibility(View.VISIBLE);
        findViewById(R.id.cancel_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.discover_progress_bar).setVisibility(View.INVISIBLE);
    }

    /***********************************************************************************************
     * SUBMARINE CALLBACK METHODS
     **********************************************************************************************/

    /**
     * If we receive a positive connection status, we start the main acitivity. If not, we let the
     * user issue another discovery.
     * @param status The new connection status.
     */
    public void receiveConnectionStatus(boolean status) {
        if(status) {
            final StartActivity thisActivity = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(submarine != null) {
                        submarine.removeConnectionStatusReceiver(TAG);
                    }
                    Intent intent = new Intent(thisActivity, MainActivity.class);
                    MainActivity.setSubmarine(submarine);
                    thisActivity.startActivity(intent);
                    findViewById(R.id.discover_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.cancel_button).setVisibility(View.INVISIBLE);
                    findViewById(R.id.discover_progress_bar).setVisibility(View.INVISIBLE);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(submarine != null) {
                        cancelDiscovery(null);
                    }
                }
            });
        }
    }
}
