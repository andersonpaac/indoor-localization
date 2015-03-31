package asc.sensor_collector_498;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Environment;
import java.io.FileOutputStream;
import java.io.*;//File;
import java.util.*;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;
import android.annotation.SuppressLint;
import android.widget.ArrayAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.media.MediaRecorder;

public class Main extends ActionBarActivity implements SensorEventListener{
    //@dev
    // N - 0
    // E = 1
    // W = 2
    // S = 3
    // NW = 4
    // SW = 5
    // NE = 6
    // SE = 7
    // setter =8
    //private static final String TAG = activity_main.class.getSimpleName();
    private SensorManager mSensorManager;
    private Sensor aSensor , gSensor , mSensor, lSensor;
    String aName;// = "MPL Accelerometer";
    String gName;// = "MPL Gyroscope";
    String mName;// = "AKM Magnetic Field";
    String lName;
    int acounter =0;
    int gcounter =0;
    int mcounter =0;
    int lcounter=0;
    int limit=0;
    int steps=0;
    float declination = 0;//+(float)-3.08;//@dev param declination
    long updateinterval=(long)2000000000;
    int wifidelay = 2000;
    int micdelay=2000;
    long nextupdateat;
    TextView mag_c;
    TextView acc_c;
    TextView gyro_c;
    TextView writer_c;
    TextView steps_c;
    TextView dist_c;
    int internal_lim = 18000;  //7000 = 35
    double stridelength = 2.2; //Stridelength in feet
    long stime;
    String fname= "button_test";
    String ext=".csv"; //stable
    int volatile_direction = 400; //@dev param volatile
    String aprev="";
    String mprev="";
    String gprev="";
    String lprev="";
    String mvalprev="";
    String mic_db="MIC_UNSET, ";
    String rprev="RSSI_UNSET, ";
    float medium;
    float azimuth , bearing;
    int ssteps=0;
    int wcounter=0;
    List<Statistic> data  = new ArrayList<Statistic>();
    List<StepStat> step_data  = new ArrayList<StepStat>();
    List<Integer> RSSI = new ArrayList<Integer>();
    double threshold = 1.4;
    long timediff=1000;
    List<Long> running = new ArrayList<Long>();
    List<Direction>Directional = new ArrayList<Direction>();
    String Results[] ;
    WifiManager wiman;
    WifiScanReceiver WiRec;
    Button button;
    int starter=-1;
    TextView bearing_c;
    audio mic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        String bacon;
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensor=  mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        lSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mSensorManager.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,gSensor,SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,lSensor,SensorManager.SENSOR_DELAY_FASTEST);
        aName=aSensor.getName();
        gName=gSensor.getName();
        mName=mSensor.getName();
        lName=lSensor.getName();

        acc_c = (TextView) findViewById(R.id.acc_c);
        gyro_c = (TextView) findViewById(R.id.gyro_c);
        mag_c = (TextView) findViewById(R.id.mag_c);
        writer_c = (TextView) findViewById(R.id.writerstat);
        steps_c = (TextView) findViewById(R.id.StepC);
        dist_c = (TextView) findViewById(R.id.distance_t);
        bearing_c = (TextView) findViewById(R.id.bearing);
        button = (Button) findViewById(R.id.cont);
        wiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WiRec = new WifiScanReceiver();
        wiman.startScan();
        Log.i("MNAME", getFilesDir().getName());
        mic = new audio();
        mic.start();
    }
    public void ButtClick(View view) {
        starter=starter+1;
        button.setText("STOP!!");
    }
    class WifiScanReceiver extends BroadcastReceiver {
        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            wcounter=wcounter+1;
            List<ScanResult> wifiScanList = wiman.getScanResults();
            ArrayList<Integer> tempdbm = new ArrayList<Integer>();
            for (int i=0;i<wifiScanList.size();i++){
                tempdbm.add(wifiScanList.get(i).level);
            }
            if(Collections.max(tempdbm)!=null){
                rprev=Float.toString(Collections.max(tempdbm))+", ";
                RSSI.add(Collections.max(tempdbm));
                mag_c.setText(rprev);
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    wiman.startScan();
                }
            }, wifidelay);
        }

    }
    protected void onResume() {
        registerReceiver(WiRec, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    int prevsize=0;

    public void updateDisplay(){
        return;
        /*
        float a_x;
        float ts;
        int prev_stat=1; //1 indicates positive
        int curr_stat =1;
        int changes=0;
        for (int a = prevsize; a < step_data.size(); a++) {
            a_x= step_data.get(a).accel_x;
            if(a_x>0)
            {
                curr_stat = 1;
            }
            if(a_x<0)
            {
                curr_stat = 0;
            }
            if (curr_stat != prev_stat)
            {
                changes = changes+1;
                prev_stat = curr_stat;
            }
        }
        if(changes-22>0){
            ssteps = ssteps + changes - 22;
        }
        prevsize = step_data.size();
        acc_c.setText(Float.toString(changes));
        mag_c.setText(Float.toString(ssteps));
        //changes=0;
        */
    }

    public void directions(){
        writer_c.setText("Now Directions!");
        ArrayList<Direction> moveddir = new ArrayList<Direction>();
        ArrayList<Integer>onlychanges = new ArrayList<Integer>();
        if(running.size()>0){
            long lval=running.get(0);
            long uval=running.get(running.size()-1);
            int idxx=0;
            int runner_stop=0;
            for (int k=0;k<Directional.size();k++){
                if(runner_stop==0) {
                    if ((Directional.get(k).timestamp >= lval) && (Directional.get(k).timestamp <= uval)) {
                        moveddir.add(Directional.get(k));
                    }
                    if (Directional.get(k).timestamp >= uval) {
                        if (idxx + 2 < running.size()) {
                            idxx=idxx+2;
                            lval=running.get(idxx);
                            uval=running.get(idxx+1);
                        }
                        if (idxx + 2 >= running.size()) {
                            runner_stop = 1;
                            break;
                        }
                    }
                }
            }

            ArrayList<Direction> netmovement = new ArrayList<Direction>();
            int l=0;
            /*
            while ((l+1)<moveddir.size()){
                if(moveddir.get(l+1).dir!=moveddir.get(l).dir){
                    netmovement.add(moveddir.get(l));
                    l=l+volatile_direction;
                }
                else{
                    l=l+1;
                }
            }
            */
            long rq_t=0;
            int k=0;
            while ((l+1)<moveddir.size()){
                if((moveddir.get(l+1).dir!=moveddir.get(l).dir)&&(moveddir.get(l).timestamp>=rq_t)){
                    int somet=netmovement.size();
                    //int somed=netmovement.size()
                    if(k==0){
                        if(moveddir.get(l).dir!=-1){//&&(moveddir.get(l).dir{
                            netmovement.add(moveddir.get(l));
                            k=k+1;
                        }
                    }
                    else if((netmovement.get(somet-1).dir!=moveddir.get(l).dir) && (k!=0)){
                        if(moveddir.get(l).dir!=-1){
                            netmovement.add(moveddir.get(l));
                        }
                    }
                    rq_t=moveddir.get(l).timestamp+volatile_direction;
                }
                l=l+1;
            }
            /*
            if(netmovement.size()>0){
                if(netmovement.get(netmovement.size()-1).dir!=moveddir.get(moveddir.size()-1).dir){
                    netmovement.add(moveddir.get(moveddir.size()-1));
                }
            }//Adding last direction
            */
            int j=netmovement.size();
            int jj=netmovement.size()*90;
            bearing_c.setText(Float.toString(jj)+" degrees!");


        }
        else
        {
            bearing_c.setText("0 Degrees!(No movement)");
        }

        /*
        intellifreq freq_n = new intellifreq();
        intellifreq freq_s= new intellifreq();
        intellifreq freq_w= new intellifreq();
        intellifreq freq_e = new intellifreq();
        freq_n.sval=0;
        freq_s.sval=0;
        freq_w.sval=0;
        freq_e.sval=0;
        for(int l=0;l<moveddir.size();l++){
            int currdir = moveddir.get(l).dir;
            switch (currdir)
            {
                case 0: freq.n = freq_n
                case 1: freq_e = freq_e +1;
                case 2: freq_w = freq_w +1;
                case 3: freq_s = freq_s +1;
            }
        }*/

    }
    public class audio {
        private MediaRecorder mRecorder = null;

        public void start() {
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                try {
                    mRecorder.prepare();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Log.i("The max amplitude is", Float.toString(mRecorder.getMaxAmplitude()));
                mRecorder.start();
            }
            int ax=mRecorder.getMaxAmplitude();
            mic_db=Integer.toString(ax)+", ";
            if(ax>0){
                gyro_c.setText(Integer.toString(ax));
            }
            Handler nh = new Handler();
            nh.postDelayed(new Runnable() {
                public void run() {
                    mic.start();
                }
            }, micdelay);
        }
        public void stop() {
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;

            }

        }


    }
    public class intellifreq{
        int frequency;
        long sval;
        long uval;
    }

    public void ported_calc(){
        boolean unset = true;
        long t_lval = step_data.get(0).timestamp + timediff;
        int idx=0;
        List<Float>somearr=new ArrayList<Float>();
        float distance;

        while (unset==true){
            if(step_data.get(idx).timestamp<=t_lval){
                somearr.add(step_data.get(idx).accel_y);
            }
            if(step_data.get(idx).timestamp>=t_lval){
                distance= Collections.max(somearr)-Collections.min(somearr);
                if(distance>threshold){
                    running.add(t_lval-timediff);
                    running.add(t_lval);
                }
                somearr.clear();
                t_lval=t_lval+timediff;

            }
            idx=idx+1;
            if(idx >= step_data.size()){
                unset=false;
            }
        }
        if(running.size()>0){
            if(running.size()%2!=0)
            {
                Log.i("Adjusted running size",Float.toString(running.size()));
                running.add(step_data.get(step_data.size()-1).timestamp);
            }
            Log.i("running size",Float.toString(running.size()));
            boolean undone=true;
            idx=0;
            long sum=0;
            while (undone){
                sum=sum+(running.get(idx+1)-running.get(idx));
                idx=idx+2;
                if(idx>=running.size()){
                    undone=false;
                }
            }
            Log.i("The sumhere ",Float.toString(sum));
            //sum=sum/1000;
            Log.i("The sum now here ",Float.toString(sum));
            double somesum;
            somesum=(sum*1.667)/1000;
            acc_c.setText(Float.toString((float)somesum));
            double tot_dist = somesum * stridelength;
            dist_c.setText(Float.toString((float)tot_dist));
        }
        else{
            Log.i("Not long enough","enough");
            acc_c.setText("0");

        }
    }
    float[] gravity;
    float[] magnetic;
    public int enumerate(String bearing_s){
        switch(bearing_s){
            case "NORTH": return 0;
            case "EAST": return 1;
            case "WEST": return 2;
            case "SOUTH": return 3;
            case "NORTH_WEST": return 4;
            case "NORTH_EAST": return 6;
            case "SOUTH_WEST": return 5;
            case "SOUTH_EAST": return 7;
            case "SETTER":return 8;

        }
        return -1;

    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        String op= event.sensor.getName();

        if (op.equals(aName) && gcounter <internal_lim && starter ==0)
        {

            if(acounter==1)
            {
                stime=event.timestamp;
                nextupdateat = stime + updateinterval;
            }
            String a_x_y_z=Float.toString((event.values[0])) + ", ";
            a_x_y_z = a_x_y_z + Float.toString((event.values[1])) + ", ";
            a_x_y_z = a_x_y_z + Float.toString((event.values[2])) + ", ";
            aprev = a_x_y_z;
            gravity = event.values;
            if ((acounter > 0) && (gcounter > 0) &&(mcounter > 0)&&(lcounter > 0) && (wcounter>0)){
                Statistic astat = new Statistic();
                astat.accel_data = aprev;
                astat.gyro_data = gprev;
                astat.mag_data = mvalprev;
                astat.direction = mprev;
                astat.light=lprev;
                astat.rssi = rprev;
                astat.mic_db=mic_db;
                medium = event.timestamp - stime;
                medium = medium/1000000;
                astat.timestamp = Math.round(medium);
                data.add(astat);

                if(event.timestamp>=nextupdateat)
                {
                    nextupdateat = event.timestamp + updateinterval;
                    updateDisplay();
                }
                StepStat somestat = new StepStat();
                somestat.accel_y = event.values[1];
                somestat.timestamp = (event.timestamp-stime);
                somestat.timestamp = somestat.timestamp/1000000;
                somestat.timestamp = Math.round(somestat.timestamp);
                step_data.add(somestat);

            }
            acounter++;

        }

        else if(op.equals(gName) && gcounter <internal_lim && starter==0)
        {

            String g_x_y_z=Float.toString((event.values[0])) + ", ";
            g_x_y_z = g_x_y_z + Float.toString((event.values[1])) + ", ";
            g_x_y_z = g_x_y_z + Float.toString((event.values[2])) + ", ";
            gprev=g_x_y_z;

            if ((acounter > 0) && (gcounter > 0) && (mcounter > 0) && (lcounter>0) &&(wcounter>0)){
                Statistic astat = new Statistic();
                astat.accel_data = aprev;
                astat.gyro_data = gprev;
                astat.direction = mprev;
                astat.mag_data = mvalprev;
                astat.light=lprev;
                astat.rssi=rprev;
                astat.mic_db=mic_db;
                medium = event.timestamp - stime;
                medium = medium/1000000;
                astat.timestamp = Math.round(medium);
                data.add(astat);
                if(acounter==nextupdateat)
                {
                    nextupdateat = acounter + updateinterval;
                    updateDisplay();
                }
            }
            gcounter++;
            gyro_c.setText(Float.toString(gcounter));

        }
        else if(op.equals(lName) && gcounter <internal_lim && starter==0)
        {

            String l_x_y_z=Float.toString(Math.round(event.values[0]));
            lprev=l_x_y_z;
            if ((acounter > 0) && (gcounter > 0) && (mcounter > 0) && (lcounter>0)&&(wcounter>0)){
                Statistic astat = new Statistic();
                astat.accel_data = aprev;
                astat.gyro_data = gprev;
                astat.mag_data = mvalprev;
                astat.direction = mprev;
                astat.light=lprev;
                astat.mic_db=mic_db;
                medium = event.timestamp - stime;
                medium = medium/1000000;
                astat.timestamp = Math.round(medium);
                data.add(astat);
                if(acounter==nextupdateat)
                {
                    nextupdateat = acounter + updateinterval;
                    updateDisplay();
                }
            }
            lcounter++;

        }


        else if(op.equals(mName) && gcounter <internal_lim && starter==0)
        {
            String m_x_y_z=Float.toString((event.values[0]))+", ";
            m_x_y_z = m_x_y_z + Float.toString((event.values[1])) + ", ";
            m_x_y_z = m_x_y_z + Float.toString((event.values[2])) + ", ";
            mvalprev=m_x_y_z;
            magnetic = event.values;

            float R[] = new float[9];
            float I[] = new float[9];
            String bearing_s;
            if(acounter>1) {
                boolean success = SensorManager.getRotationMatrix(R, I, gravity, magnetic);
                if (success) {
                    float orientation[] = new float[3];

                    SensorManager.getOrientation(R, orientation);
                    azimuth = orientation[0];
                    bearing = azimuth * (360 / (2 * (float) Math.PI));
                    bearing = bearing + declination;

                    if (bearing < 0) {
                        bearing = bearing + 360;
                    }
                    //mag_field = Float.toString(orientation[0]); // orientation contains: azimut, pitch and roll
                }
                bearing_s = "SETTER, ";


                if(bearing >= 0 && bearing < 25){
                    bearing_s = "NORTH";
                }

                else if(bearing >= 25 && bearing < 118){   //140 , 150 , 160 , 90 , 100
                    bearing_s = "EAST";
                }

                else if(bearing >= 118 && bearing < 193){  //210 , 192 , 220 ,
                    bearing_s = "SOUTH";
                }


                else if(bearing >= 193 && bearing < 305){ //270  , 287  , 256
                    bearing_s =  "WEST";
                }
                else if(bearing >= 305 && bearing < 359){
                    bearing_s  ="NORTH";
                }

                /*
                 if(bearing >= 0 && bearing < 45){
                    bearing_s = "NORTH";
                }
               else if(bearing >= 45 && bearing < 135){   //140 , 150 , 160 , 90 , 100
                    bearing_s = "EAST";
                }

                else if(bearing >= 135 && bearing < 225){  //210 , 192 , 220 ,
                    bearing_s = "SOUTH";
                }
                else if(bearing >= 315 && bearing < 359){
                    bearing_s  ="NORTH";
                }

                else if(bearing >= 225 && bearing < 315){ //270  , 287  , 256
                    bearing_s =  "WEST";
                }
                //


                else if(bearing >= 228 && bearing < 245){
                    bearing_s =  "SOUTH_WEST";
                }
                else if(bearing >= 330 && bearing < 339){
                    bearing_s ="NORTH_WEST";
                }
                else if(bearing >= 20 && bearing < 34){// 73 , 76 , 80 ,
                    bearing_s =  "NORTH_EAST";
                else if(bearing >= 145 && bearing < 159){ //167  , 170 ,
                    bearing_s =  "SOUTH_EAST";
                }
                }*/

                mprev = Float.toString(enumerate(bearing_s))+", ";
                writer_c.setText(bearing_s+" "+Float.toString(bearing));

            }
            else
            {
                mprev ="Unset" + ", ";
                writer_c.setText(mprev);
                bearing_s="SETTER";
            }
            if ((acounter > 0) && (gcounter > 0) && (mcounter > 0) & (lcounter>0)&&(wcounter>0)){


                Statistic astat = new Statistic();
                astat.accel_data = aprev;
                astat.gyro_data = gprev;
                astat.mag_data = mvalprev;
                astat.direction= mprev;
                astat.light=lprev;
                medium = event.timestamp - stime;
                medium = medium/1000000;
                astat.timestamp = Math.round(medium);
                astat.rssi=rprev;
                astat.mic_db=mic_db;
                data.add(astat);
                Direction temp =  new Direction();
                temp.dir = enumerate(bearing_s);
                temp.timestamp=Math.round(medium);
                Directional.add(temp);
                if(acounter==nextupdateat)
                {
                    nextupdateat = acounter + updateinterval;
                    updateDisplay();
                }
            }
            mcounter++;

        }

        else if(starter==1 && limit == 0)
        {
            Log.i("Pasing","Passed to Parseit");
            mSensorManager.unregisterListener(this,aSensor);
            mSensorManager.unregisterListener(this,mSensor);
            mSensorManager.unregisterListener(this,gSensor);
            mSensorManager.unregisterListener(this,lSensor);
            //ParseIt();
            spawnit_new();
            ported_calc();
            directions();
            limit=1;
        }


    }


    public class Direction{
        public int dir;
        public long timestamp;
    }
    public void spawnit_new() {

        writer_c.setText("NOW WRITINGGGG!!!");
        String setupstring = "TS, ACCEL_X, ACCEL_Y, ACCEL_Z, GYRO_X, GYRO_Y, GYRO_Z, MAG_X, MAG_Y,";
        setupstring=setupstring+" MAG_Z, DIRECTION , RSSI, AMPLITUDE, LIGHT\n";
        File myFile = new File("/sdcard/" + fname+ext);

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            fOut.write(setupstring.getBytes());
            for (int a = 0; a < data.size(); a++) {
                if(data.get(a).rssi==null){
                    data.get(a).rssi=data.get(a-1).rssi;
                }
                String writing = Float.toString(data.get(a).timestamp) + ", ";
                writing = writing + data.get(a).accel_data + data.get(a).gyro_data;
                writing=writing+ data.get(a).mag_data + data.get(a).direction;
                writing = writing + data.get(a).rssi+ data.get(a).mic_db+data.get(a).light+"\n";
                fOut.write(writing.getBytes());

            }
            fOut.close();

            writer_c.setText("Done to " + myFile.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
        writestepdata();

    }
    public void writestepdata(){
        writer_c.setText("Now writing stepdata!!!");
        //String setupstring = "TS, ACCEL_Y;" ; //, ACCEL_Y, ACCEL_Z, GYRO_X, GYRO_Y, GYRO_Z, MAG_X, MAG_Y, MAG_Z, DIRECTION , LIGHT\n";
        File myFile = new File("/sdcard/" + fname + "_step"+ext);
        Log.i("Writer: Step data size",Float.toString(step_data.size()));
        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            //fOut.write(setupstring.getBytes());
            for (int gg = 0; gg < step_data.size(); gg++) {

                String writing = Float.toString(step_data.get(gg).timestamp)+", ";
                writing = writing + Float.toString(step_data.get(gg).accel_y)+"\n";

                /*
                String writing = Float.toString(step_data.get(gg).timestamp) + ", ";
                writing = writing + Float.toString(step_data.get(gg).accel_y)+"\n";*/

                fOut.write(writing.getBytes());
            }
            fOut.close();

            writer_c.setText("Done to " + myFile.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
        writerunning();

    }
    public void writerunning(){
        writer_c.setText("Now writing running!!!");
        //String setupstring = "TS, ACCEL_X, ACCEL_Y, ACCEL_Z, GYRO_X, GYRO_Y, GYRO_Z, MAG_X, MAG_Y, MAG_Z, DIRECTION , LIGHT\n";
        File myFile = new File("/sdcard/" + fname + "_running"+ext);
        Log.i("Writer: Running size is",Float.toString(running.size()));
        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            //fOut.write(setupstring.getBytes());
            for (int gg = 0; gg < running.size(); gg++) {

                String writing = Float.toString(running.get(gg));
                /*
                String writing = Float.toString(step_data.get(gg).timestamp) + ", ";
                writing = writing + Float.toString(step_data.get(gg).accel_y)+"\n";*/

                fOut.write(writing.getBytes());
            }
            fOut.close();

            writer_c.setText("Done to " + myFile.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }
}
