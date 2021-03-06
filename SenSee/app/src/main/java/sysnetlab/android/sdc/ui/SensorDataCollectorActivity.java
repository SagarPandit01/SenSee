/*
 * Copyright (c) 2014, the SenSee authors.  Please see the AUTHORS file
 * for details. 
 * 
 * Licensed under the GNU Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 			http://www.gnu.org/copyleft/gpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package sysnetlab.android.sdc.ui;

import sysnetlab.android.sdc.R;
import sysnetlab.android.sdc.datacollector.DropboxHelper;
import sysnetlab.android.sdc.datacollector.Experiment;
import sysnetlab.android.sdc.datacollector.ExperimentManagerSingleton;
import sysnetlab.android.sdc.datastore.StoreSingleton;
import sysnetlab.android.sdc.sensor.AbstractSensor;
import sysnetlab.android.sdc.sensor.SensorDiscoverer;
import sysnetlab.android.sdc.ui.fragments.ExperimentListFragment;
import sysnetlab.android.sdc.ui.fragments.FragmentUtils;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class SensorDataCollectorActivity extends FragmentActivityBase
        implements
        ExperimentListFragment.OnFragmentClickListener {

    public final static String APP_OPERATION_KEY = "operation";
    public final static int APP_OPERATION_CREATE_NEW_EXPERIMENT = 1;
    public final static int APP_OPERATION_CLONE_EXPERIMENT = 2;

    private ExperimentListFragment mExperimentListFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);

        ExperimentManagerSingleton.getInstance().addExperimentStore(
                StoreSingleton.getInstance());

        if (!SensorDiscoverer.isInitialized())
            SensorDiscoverer.initialize(getApplicationContext());

        mLoadingTask = new TaskLoadingSpinner();
        mLoadingTask.execute();

        /**
         * Remove the spinning wheel so that it doesn't overlaps with the
         * experiment loading progress bar
         */
        mProgressBar.setVisibility(View.GONE);

        mExperimentListFragment = new ExperimentListFragment();
        FragmentUtils.addFragment(this, mExperimentListFragment);
    }

    @Override
    protected void onRestart() {
        boolean isNewExperiment;
        isNewExperiment = getIntent().getBooleanExtra("newExperiment", false);
        if (isNewExperiment) {
            getIntent().removeExtra("newExperiment");
            mExperimentListFragment.reloadExperiments(ExperimentManagerSingleton.getInstance()
                    .getActiveExperiment());
        }
        super.onRestart();
    }

    @Override
    protected void loadTask() {
        // Dropbox
        DropboxHelper.getInstance(getApplicationContext());

        for (AbstractSensor sensor : SensorDiscoverer.discoverSensorList()) {
            sensor.setSelected(false);
        }
    }

    public void onResume() {
        super.onResume();

        // Complete the Dropbox Authorization
        DropboxHelper.getInstance(SensorDataCollectorActivity.this).completeAuthentication();

        // help determine what screen resolution the device is and plan
        // resources based on 
        // http://developer.android.com/guide/practices/screens_support.html
        Log.d("SensorDataCollector", "DensityDpi = "
                + getResources().getDisplayMetrics().densityDpi);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sensordatacollector_activity_menu, menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_dropbox);
        if (DropboxHelper.getInstance().isLinked()) {
            item.setTitle(getString(R.string.text_unlink_from_dropbox));
        } else {
            item.setTitle(getString(R.string.text_link_to_dropbox));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        DropboxHelper dbHelper = DropboxHelper.getInstance();
        switch (item.getItemId()) {
            case R.id.action_dropbox:
                // This logs you out if you're logged in, or vice versa
                if (dbHelper.isLinked()) {
                    dbHelper.unlink();
                } else {
                    dbHelper.link();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onExperimentClicked_ExperimentListFragment(Experiment experiment) {
        Log.d("SensorDataCollector", "Creating ViewExperimentActivity with experiment = "
                + experiment);

        Intent intent = new Intent(this, ViewExperimentActivity.class);
        ExperimentManagerSingleton.getInstance().setActiveExperiment(experiment);
        startActivity(intent);
    }

    @Override
    public void onCreateExperimentButtonClicked_ExperimentListFragment(Button b) {
        Log.d("SensorDataCollector", "Creating CreateExperimentActivity ...");

        Intent intent = new Intent(this, CreateExperimentActivity.class);
        intent.putExtra(SensorDataCollectorActivity.APP_OPERATION_KEY,
                SensorDataCollectorActivity.APP_OPERATION_CREATE_NEW_EXPERIMENT);
        startActivity(intent);
    }

    public ExperimentListFragment getExperimentListFragment() {
        return mExperimentListFragment;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
