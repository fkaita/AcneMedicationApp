package com.example.acnedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
//    private BarChart chart;
    private LineChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.chart);
        chart.getDescription().setEnabled(false);
        chart.setVisibleXRangeMaximum(7);
        chart.setMaxVisibleValueCount(7);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
//        xAxis.setValueFormatter(new BarChartXAxisValueFormatter());

        YAxis leftYAxis = chart.getAxisLeft();
        leftYAxis.setEnabled(false);
        leftYAxis.setDrawZeroLine(true);

        YAxis rightYAxis = chart.getAxisRight();
        rightYAxis.enableGridDashedLine(10f, 10f, 0f);

        Legend l = chart.getLegend();
        l.setEnabled(false);
        setData();

        final Button btnSlf = findViewById(R.id.btnSlf);
        btnSlf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AcneDetectionActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Hashtable<Integer, String> label_dict = new Hashtable<Integer, String>();
        label_dict.put(-1,"No acne found");
        label_dict.put(0,"Mild");
        label_dict.put(1,"Moderate");
        label_dict.put(2,"Severe");
        label_dict.put(3,"Very Severe");

        TextView txtCls = findViewById(R.id.txtCls);
        if (AcneDetectionActivity.outputClsFinal!=null){
            String label = label_dict.get(AcneDetectionActivity.outputClsFinal);
            txtCls.setText(label);
        }
        TextView txtCnt = findViewById(R.id.txtCnt);
        if (AcneDetectionActivity.outputCntFinal!=null){
            txtCnt.setText("Counts: " + AcneDetectionActivity.outputCntFinal);
        }
        setData();
    }

    private void setData() {

//         Make list of the last 30 days.
//        Date today = new Date();
//        Date startdate = new Date(today.getTime()-1000*60*60*24*30); // 30days before
        Date today = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date startdate = cal.getTime();

        List<Date> dates = getDaysBetweenDates(startdate, today);
        for (int i=0; i<dates.size(); i++){
            Date day = dates.get(i);
//            System.out.println("original date: " + day.toString() );
        }

//        ArrayList<BarEntry> values = new ArrayList<>();
        ArrayList<Entry> values0 = new ArrayList<>();
        ArrayList<Entry> values1 = new ArrayList<>();

        OpenHelper helper = new OpenHelper(getApplicationContext());

        for (int i=0; i<dates.size(); i++){
            Date day = dates.get(i);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String txt_date = dateFormat.format(day);
            float avgCnt = helper.countByDate(txt_date, txt_date);
            System.out.println("DB date: " + txt_date + " count: " + avgCnt );
            if (avgCnt == -1){
                values0.add(new BarEntry(i, 0)); // Not display here
            } else{
                values0.add(new BarEntry(i, avgCnt));
                values1.add(new BarEntry(i, avgCnt)); // Display even zero
            }
            i+=1;
        }

//        BarDataSet set1;
        LineDataSet set0, set1;

        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
//            set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
            set0 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(1);
            set0.setValues(values1);
            set1.setValues(values1);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();

        } else {
//            set1 = new BarDataSet(values, "test");
            set0 = new LineDataSet(values0, "test0");
            set1 = new LineDataSet(values1, "test1");

            set0.setDrawIcons(false);
            set1.setDrawIcons(false);

            set0.setColor(Color.TRANSPARENT); // Not display this chart
//
//            int startColor1 = ContextCompat.getColor(this, android.R.color.holo_orange_light);
//            int startColor2 = ContextCompat.getColor(this, android.R.color.holo_blue_light);
//            int startColor3 = ContextCompat.getColor(this, android.R.color.holo_orange_light);
//            int startColor4 = ContextCompat.getColor(this, android.R.color.holo_green_light);
//            int startColor5 = ContextCompat.getColor(this, android.R.color.holo_red_light);
//            int endColor1 = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
//            int endColor2 = ContextCompat.getColor(this, android.R.color.holo_purple);
//            int endColor3 = ContextCompat.getColor(this, android.R.color.holo_green_dark);
//            int endColor4 = ContextCompat.getColor(this, android.R.color.holo_red_dark);
//            int endColor5 = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
//
//            List<Fill> gradientFills = new ArrayList<>();
//            gradientFills.add(new Fill(startColor1, endColor1));
//            gradientFills.add(new Fill(startColor2, endColor2));
//            gradientFills.add(new Fill(startColor3, endColor3));
//            gradientFills.add(new Fill(startColor4, endColor4));
//            gradientFills.add(new Fill(startColor5, endColor5));
//
//            set1.setFills(gradientFills);
//
//            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set0);
            dataSets.add(set1);
//
//            BarData data = new BarData(dataSets);
            LineData data = new LineData(dataSets);
            data.setValueTextSize(20f);
//            data.setValueTypeface(tfLight);
//            data.setBarWidth(0.9f);

            chart.setData(data);
            chart.setVisibleXRangeMaximum(14);
            chart.moveViewToX(18);
        }
    }

    public static List<Date> getDaysBetweenDates(Date startdate, Date enddate)
    {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startdate);

        while (calendar.getTime().before(enddate))
        {
            Date result = calendar.getTime();
            dates.add(result);
            calendar.add(Calendar.DATE, 1);
        }
        Date result = calendar.getTime();
        dates.add(result);
        return dates;
    }

}