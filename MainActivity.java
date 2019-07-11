package com.kalom.unipismartalert;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private Button abordButton, sosButton, speekerButton;
    private TextView countdownText;
    private float lastZ;
    private float deltaZ = 0;
    private float plungeThreshold = 0;
    private float quakeThreshold = 0;
    private long currentTime = 0;
    private long quekeId;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Speaker speaker;
    private SharedPreferences preferences;
    private String latitude, longitude;
    private boolean isActiveCountDownTimer = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int PERMGRANTED = PackageManager.PERMISSION_GRANTED;
    private static final int REQUEST_CODE = 1234;
    private List<QuakeDataModel> quakeDataList = new ArrayList<>();
    private boolean runOnlyOnce;

    private DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("earthquakes");
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speaker = new Speaker(this);
        initializeView();
        initializeService();

        /*
            START BUTTONS
         */
        abordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isActiveCountDownTimer = false;
                sendSMS("ΑΚΥΡΟ");
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gps = new GPSTracker(MainActivity.this);

                // check if GPS enabled
                if (gps.canGetLocation()) {
                    latitude = String.valueOf(gps.getLatitude());
                    longitude = String.valueOf(gps.getLongitude());
                    smsPerm();
                    sendSMS("");
                    message("Messages Sent!");
                } else {
                    message("Message Failed...");
                }

            }
        });
        speekerButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                speaker.speak("help, help, help");
            }
        });
        /*
            END BUTTONS
         */
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        deltaZ = Math.abs(lastZ - event.values[2]);
        if (deltaZ < 2)
            deltaZ = 0;
        lastZ = event.values[2];
        if (deltaZ > plungeThreshold && !isActiveCountDownTimer && !isPhonePluggedIn(this)) {
            isActiveCountDownTimer = true;
            new CountDownTimer(5000, 1000) {
                ToneGenerator toneGen1;

                @Override
                public void onTick(long millisUntilFinished) {
                    countdownText.setText(java.lang.String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
                    try {
                        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFinish() {
                    countdownText.setText(java.lang.String.valueOf(0));
                    isActiveCountDownTimer = false;
                    toneGen1.stopTone();
                }
            }.start();
        }

        double quake = (Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2)) - SensorManager.GRAVITY_EARTH);
        if (isPhonePluggedIn(this) && quake > 2 && !runOnlyOnce) {
            runOnlyOnce = true;
            FirebaseApp.initializeApp(this);
            currentTime = new Date().getTime();
        } else if (quake < 0.5 && runOnlyOnce) {
            runOnlyOnce = false;
            QuakeDataModel quakeData = new QuakeDataModel(longitude, latitude);
            quakeData.setCurrentTime(currentTime);
            ref.child(Long.toString(quekeId++)).setValue(quakeData);
            getTimeStamp();
            if (quakeDataList.size() >= 3) {
                message("WARNING: EARTHQUAKE!!!");
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public HashMap<String, String> read() {
        Map<String, ?> result;
        result = preferences.getAll();
        return (HashMap<String, String>) result;
    }

    public void write() {
        SharedPreferences.Editor editor = preferences.edit();
//        editor.putString("mhtsos", "+306958619314");
        editor.apply();
    }

    private void message(String messageKey) {
        Toast.makeText(this, messageKey, Toast.LENGTH_SHORT).show();
    }

    private void initializeView() {
        abordButton = findViewById(R.id.abordButton);
        countdownText = findViewById(R.id.countdowntextView);
        sosButton = findViewById(R.id.sosButton);
        speekerButton = findViewById(R.id.speakButton);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

    }

    private void initializeService() {
        getLocationPermission();
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        write();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            plungeThreshold = accelerometer.getMaximumRange() / 4;
            quakeThreshold = accelerometer.getMaximumRange();
        }

    }


    protected void smsPerm() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

    }

    public void sendSMS(String msg) {
        final String longi = longitude;
        final String lati = latitude;
        final String message = (msg != null) ? msg :
                "Γεωγραφικό μήκος: " + longi + "\nΓεωγραφικό πλάτος : " + lati + "\nΒΟΗΘΕΙΑ!";
//        for (String phoneNo : read().values()) {
//            smsManager.sendTextMessage(phoneNo, null, message, null, null);
//        }


        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
                SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        // ---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        ContentValues values = new ContentValues();
                        for (String phoneNo : read().values()) {
                            values.put("address", phoneNo);
                            values.put("body", message);
                        }
                        getContentResolver().insert(
                                Uri.parse("content://sms/sent"), values);
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        // ---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage("+306958619314", null, message, sentPI, deliveredPI);
    }


    public static boolean isPhonePluggedIn(Context context) {
        boolean charging = false;

        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean batteryCharge = (status == BatteryManager.BATTERY_STATUS_CHARGING);

        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_USB);

        if (batteryCharge) {
            charging = true;
        }
        if (usbCharge) {
            charging = true;
        }


        return charging;
    }

    private void getTimeStamp() {
        FirebaseApp.initializeApp(this);
        ref = FirebaseDatabase.getInstance().getReference("earthquakes");
        ref.addChildEventListener(new ChildEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                QuakeDataModel data = dataSnapshot.getValue(QuakeDataModel.class);
                quakeDataList.add(data);
            }

            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void getLocationPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION)
                    != PERMGRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{FINE_LOCATION},
                        REQUEST_CODE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}