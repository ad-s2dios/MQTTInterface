package com.ads2dios.adrieladtan.mqttinterface;

import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // Layout stuff
    TextView textView;
//    Button ledButt;
//    Button pumpButt;
//    Button reconnectButt;

    TextView tempTV;
    TextView lightTV;
    TextView waterTV;
    ProgressBar pb;
    SeekBar seekbar;

    ImageButton fwdButt;
    ImageButton backButt;
    ImageButton leftButt;
    ImageButton rightButt;

    GraphView graphView;
    ArrayList<Float> tempData;
    ArrayList<Float> lightData;
    ArrayList<Float> waterData;
    LineGraphSeries<DataPoint> tempSeries;
    LineGraphSeries<DataPoint> waterSeries;
    LineGraphSeries<DataPoint> lightSeries;
    double graphLastXValue;

    public int MOISTURE_THRESHOLD;
    public int LIGHT_THRESHOLD;

    // MQTT stuff
    MqttAndroidClient androidClient;
//    final String SERVER_URI = "tcp://iot.eclipse.org:1883";
    final String SERVER_URI = "tcp://mqtt.fluux.io:1883";
    String CLIENT_ID = "MQTTInterfaceClient";
    final String SESSION = "AdrielAndrew/esp32/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Layout setup
        textView = findViewById(R.id.text_view);
//        ledButt = findViewById(R.id.led_button);
//        pumpButt = findViewById(R.id.pump_button);
//        reconnectButt = findViewById(R.id.reconnect_button);
        tempTV = findViewById(R.id.temp_tv);
        lightTV = findViewById(R.id.light_tv);
        waterTV = findViewById(R.id.water_tv);
        pb = findViewById(R.id.pb);
        graphView = findViewById(R.id.graph);
        fwdButt = findViewById(R.id.fwd_button);
        backButt = findViewById(R.id.back_button);
        leftButt = findViewById(R.id.left_button);
        rightButt = findViewById(R.id.right_button);
        seekbar = findViewById(R.id.seek_bar);

        CLIENT_ID = CLIENT_ID + System.currentTimeMillis();

        setup();
        clearData();

        fwdButt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println(" pressed ");
                        publishMessage("move", "fwd_start");
                        return true;
                    case MotionEvent.ACTION_UP:
                        System.out.println(" released ");
                        publishMessage("move", "fwd_end");
                        return true;
                }
                return false;
            }
        });

        backButt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println(" pressed ");
                        publishMessage("move", "back_start");
                        return true;
                    case MotionEvent.ACTION_UP:
                        System.out.println(" released ");
                        publishMessage("move", "back_end");
                        return true;
                }
                return false;
            }
        });

        leftButt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println(" pressed ");
                        publishMessage("move", "left_start");
                        return true;
                    case MotionEvent.ACTION_UP:
                        System.out.println(" released ");
                        publishMessage("move", "left_end");
                        return true;
                }
                return false;
            }
        });

        rightButt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println(" pressed ");
                        publishMessage("move", "right_start");
                        return true;
                    case MotionEvent.ACTION_UP:
                        System.out.println(" released ");
                        publishMessage("move", "right_end");
                        return true;
                }
                return false;
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String payload = "speed," + String.valueOf(seekBar.getProgress());
                publishMessage("actuate", payload);
            }
        });
    }

    private void clearData(){
        graphLastXValue = 0d;

        tempData = new ArrayList<>();
        lightData = new ArrayList<>();
        waterData = new ArrayList<>();

        tempSeries = new LineGraphSeries<>();
        lightSeries = new LineGraphSeries<>();
        waterSeries = new LineGraphSeries<>();

        tempSeries.setColor(Color.rgb(244,79,72));
        lightSeries.setColor(Color.rgb(248,195,0));
        waterSeries.setColor(Color.rgb(100,221,255));

        DataPoint[] origin = new DataPoint[1];
        origin[0] = new DataPoint(0,0);
        tempSeries.resetData(origin);
        lightSeries.resetData(origin);
        waterSeries.resetData(origin);

        graphView.addSeries(tempSeries);
        graphView.addSeries(lightSeries);
        graphView.addSeries(waterSeries);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(40);
    }

    private void setup(){
        textView.setText("");
        MOISTURE_THRESHOLD = 100;
        LIGHT_THRESHOLD = 0;
        try {
            // create client
            MemoryPersistence persistence = new MemoryPersistence();
            androidClient = new MqttAndroidClient(getApplicationContext(), SERVER_URI, CLIENT_ID, persistence);
            androidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {

                    if (reconnect) {
                        print("Reconnected to : " + serverURI);
                        // Because Clean Session is true, we need to re-subscribe
                        subscribeToTopic("data");
                    } else {
                        print("Connected to: " + serverURI);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    print("The Connection was lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (topic.equals(SESSION + "data")){
                        String dataArray[] = message.toString().split(",");
                        String temp = dataArray[0].substring(0, 6);
                        String water = dataArray[1];
                        Float light = Float.valueOf(dataArray[2])/4096f*100f;

                        tempTV.setText(temp);
                        waterTV.setText(water);
                        pb.setProgress(Math.round(light));
                        lightTV.setText(String.valueOf(light));

                        tempData.add(Float.valueOf(temp));
                        waterData.add(Float.valueOf(water));
                        lightData.add(light);

                        graphLastXValue += 1d;
                        tempSeries.appendData(new DataPoint(graphLastXValue, Double.valueOf(temp)), true, 50);
                        waterSeries.appendData(new DataPoint(graphLastXValue, Double.valueOf(water)), true, 50);
                        lightSeries.appendData(new DataPoint(graphLastXValue, light), true, 30);
                    }
                    else{
                        print("Incoming message: " + new String(message.getPayload()));
                    }
                    //TODO: handle received messages
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Toast.makeText(MainActivity.this, "message delivered", Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception e) {
            print("Error 1: " + e.getCause());
        }

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            // connect to server
            print("Connecting to " + SERVER_URI);
            androidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    print("Connected!");
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    androidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic("data");
                    seekbar.setProgress(30);
                    String payload = "speed," + String.valueOf(30);
                    publishMessage("actuate", payload);
                    publishMessage("actuate", "moisture_threshold," + String.valueOf(MOISTURE_THRESHOLD));
                    publishMessage("actuate", "light_threshold," + String.valueOf(LIGHT_THRESHOLD));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    print("Failed to connect to: " + SERVER_URI);
                    MqttException e = (MqttException) exception;
                    print("Error: " + e.getReasonCode());
                }
            });
//            client.connect();
        } catch (MqttException ex){
            print("Failed");
            ex.printStackTrace();
        }
        catch (Exception e){
            print("Error2: " + e.getMessage());
        }
    }

    private void print(String mainText){
        System.out.println("LOG: " + mainText);
        String display = textView.getText().toString() + "\n" + mainText;
        textView.setText(display);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_refresh:
                setup();
                break;
            case R.id.action_led:
                publishMessage("actuate", "led");
                break;
            case R.id.action_pump:
                publishMessage("actuate", "pump");
                break;
            case R.id.action_settings:
                settings();
                break;
        }


        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    public void subscribeToTopic(String topic){
        try {
            androidClient.subscribe(SESSION + topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    print("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    print("Failed to subscribe");
                    print("Error: " + exception.getMessage());
                }
            });

//            client.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
//                @Override
//                public void messageArrived(String topic, MqttMessage message) throws Exception {
//                    // message Arrived!
//                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
//                }
//            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            androidClient.publish(SESSION + topic, message);
            //print("Message Published");
            if (!androidClient.isConnected()) {
                print(androidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (Exception e) {
            System.err.println("Error Publishing: " + e.getMessage());
            print("Error3: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void settings(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Thresholds");
        alert.setMessage("Set the thresholds for the moisture and light sensors to be activated");

        LinearLayout outerLL = new LinearLayout(this);
        outerLL.setOrientation(LinearLayout.VERTICAL);

        LinearLayout moistureLL = new LinearLayout(this);
        moistureLL.setOrientation(LinearLayout.HORIZONTAL);
        moistureLL.setPadding(50, 20, 50, 20);
        final TextView moistureTV = new TextView(this);
        moistureTV.setText("Moisture: " + String.valueOf(MOISTURE_THRESHOLD));
        final SeekBar moistureSeek = new SeekBar(this);
        moistureSeek.setMax(4096);
        moistureSeek.setProgress(MOISTURE_THRESHOLD);
        moistureSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                moistureTV.setText("Moisture: " + String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        moistureLL.addView(moistureTV, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        moistureLL.addView(moistureSeek, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout lightLL = new LinearLayout(this);
        lightLL.setOrientation(LinearLayout.HORIZONTAL);
        lightLL.setPadding(50, 20, 50, 20);
        final TextView lightTV = new TextView(this);
        lightTV.setText("Brightness: " + String.valueOf(LIGHT_THRESHOLD));
        final SeekBar lightSeek = new SeekBar(this);
        lightSeek.setMax(4096);
        lightSeek.setProgress(LIGHT_THRESHOLD);
        lightSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lightTV.setText("Brightness: " + String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lightLL.addView(lightTV, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lightLL.addView(lightSeek, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        outerLL.addView(moistureLL);
        outerLL.addView(lightLL);
        alert.setView(outerLL);

        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {

            }
        });

        alert.setPositiveButton("Apply",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id)
            {
                MOISTURE_THRESHOLD = moistureSeek.getProgress();
                LIGHT_THRESHOLD = lightSeek.getProgress();
                publishMessage("actuate", "moisture_threshold," + String.valueOf(moistureSeek.getProgress()));
                publishMessage("actuate", "light_threshold," + String.valueOf(lightSeek.getProgress()));
                Toast.makeText(getApplicationContext(), "Thresholds updated",Toast.LENGTH_LONG).show();
            }
        });

        alert.show();
    }
}
