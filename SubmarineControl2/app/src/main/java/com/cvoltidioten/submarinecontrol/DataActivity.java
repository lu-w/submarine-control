package com.cvoltidioten.submarinecontrol;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity displays the data obtained from the dive.
 */
public class DataActivity extends AppCompatActivity {
    private final static String TAG = "DataActivity";

    private Dive dive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        ((Toolbar)findViewById(R.id.toolbar)).setTitle("Collected data");

        // Obtains dive data from the intent.
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            this.dive = extras.getParcelable("dive");
        } else {
            Log.e(TAG, "Could not retrieve dive data from intent.");
            // Displays some fake data.
            List<SubmarineProtos.Datum> fakeData = new ArrayList<>();
            for(int i = 0; i < 12; i++) {
                fakeData.add(SubmarineProtos.Datum.newBuilder()
                                .setDepth(i)
                                .setPressure((float) i * 10)
                                .setTemperature((float) ((30 - i) + Math.random() * 4))
                                .setTimestamp(1)
                                .build()
                );
            }
            this.dive = new Dive(0, 0, fakeData);
        }

        // Fills chart with the obtained data.
        LineChart chart = (LineChart)findViewById(R.id.data_chart);
        List<Datum> diveData = this.dive.getData();
        ArrayList<Entry> tempDataList = new ArrayList<>();
        ArrayList<Entry> depthDataList = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        double minTemp = diveData.get(0).getTemperatureC();
        double maxDepth = diveData.get(0).getDepthM();
        for(int i = 0; i < diveData.size(); i++) {
            tempDataList.add(new Entry((float)diveData.get(i).getTemperatureC(), i));
            depthDataList.add(new Entry((float)diveData.get(i).getDepthM(), i));
            labels.add(Integer.toString(i));
            if(diveData.get(i).getTemperatureC() < minTemp) {
                minTemp = diveData.get(i).getTemperatureC();
            }
            if(diveData.get(i).getDepthM() > maxDepth) {
                maxDepth = diveData.get(i).getDepthM();
            }
        }
        LineDataSet tempDataSet = new LineDataSet(tempDataList, "Temperature [°C]");
        LineDataSet depthDataSet = new LineDataSet(depthDataList, "Depth [m]");
        depthDataSet.setColor(getResources().getColor(R.color.data_orange));
        depthDataSet.setCircleColor(getResources().getColor(R.color.data_orange));
        LineData data = new LineData(labels);
        data.addDataSet(tempDataSet);
        data.addDataSet(depthDataSet);
        chart.setData(data);

        // Fills two additional cards with information.
        ((TextView)findViewById(R.id.min_temperature)).setText(new DecimalFormat("##.#").format(minTemp) + getString(R.string.temperature));
        ((TextView)findViewById(R.id.max_depth)).setText(new DecimalFormat("#.##").format(maxDepth) + " " + getString(R.string.meters));
    }

    /**
     * Is called when the user presses the save button. It saves the data to a CSV file on the
     * external storage.
     * @param view Is ignored.
     */
    public void save(View view) {
        String csv = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/dive.csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csv));
            List<String[]> data = new ArrayList<String[]>();
            for(Datum datum : this.dive.getData()) {
                data.add(new String[]{Double.toString(datum.getDepthM()), Double.toString(datum.getTemperatureC())});
            }
            writer.writeAll(data);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on writing CSV to storage", e);
        }

        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
    }

    /**
     * Is called when the user presses the discard button. It starts the start activity.
     * @param view Is ignored.
     */
    public void discard(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
    }
}
