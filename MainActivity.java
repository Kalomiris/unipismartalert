package com.kalom.unipismartalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private Button abordButton, sosButton, speekerButton;
    private TextView countdownText;
    private float lastZ;
    private float deltaZ = 0;
    private float plungeThreshold = 0;
    private float quakeThreshold = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Speaker speaker;
    private LocationManager locationManager;
    private SmsManager smsManager;
    private SharedPreferences preferences;
    private Double latitude, longitude;
    private boolean isActiveCountDownTimer = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String SEND_SMS = Manifest.permission.SEND_SMS;
    private static final String READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;
    private static final int PERMGRANTED = PackageManager.PERMISSION_GRANTED;

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
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sentSMS()) {
                    Collection<String> phoneList;
                    phoneList = read().values();
                    for (String element : phoneList) {
                        Double[] location = getLocationData();
                        latitude = location[0];
                        longitude = location[1];
                        sendSMSMessage(element);
                    }
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

    private Double[] getLocationData() {
        final Double[] result = new Double[2];
        new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                result[0] = location.getLatitude();
                result[1] = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        return result;
    }

    private boolean sentSMS() {
        if (ActivityCompat.checkSelfPermission(this, SEND_SMS) != PERMGRANTED
                || ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) != PERMGRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{SEND_SMS, READ_PHONE_STATE}, 123);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        deltaZ = Math.abs(lastZ - event.values[2]);
        if (deltaZ < 2)
            deltaZ = 0;
        lastZ = event.values[2];
        if (deltaZ > plungeThreshold && !isActiveCountDownTimer) {
            isActiveCountDownTimer = true;
            new CountDownTimer(5000, 1000) {
                ToneGenerator toneGen1;

                @Override
                public void onTick(long millisUntilFinished) {
                    countdownText.setText(java.lang.String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
                    toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1500);
                }

                @Override
                public void onFinish() {
                    countdownText.setText(java.lang.String.valueOf(0));
                    isActiveCountDownTimer = false;
                    toneGen1.stopTone();
                }
            }.start();
        }
        if (deltaZ < quakeThreshold) {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void message(String messageKey) {
        Toast.makeText(this, messageKey, Toast.LENGTH_SHORT).show();
    }

    public HashMap<String, String> read() {
        Map<String, ?> result;
        result = preferences.getAll();
        return (HashMap<String, String>) result;
    }

    public void write() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("anna", "6976860669");
//        editor.putString("giorgos", "6972849576");
//        editor.putString("giannis", "6938648259");
        editor.apply();
    }

    private void initializeView() {
        abordButton = findViewById(R.id.abordButton);
        countdownText = findViewById(R.id.countdowntextView);
        sosButton = findViewById(R.id.sosButton);
        speekerButton = findViewById(R.id.speakButton);
        smsManager = SmsManager.getDefault();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

    }

    private void initializeService() {
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        write();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            plungeThreshold = accelerometer.getMaximumRange() / 4;
            quakeThreshold = accelerometer.getMaximumRange();
        }
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION) != PERMGRANTED &&
                ActivityCompat.checkSelfPermission(this, COURSE_LOCATION) != PERMGRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
    }

    protected void sendSMSMessage(String phoneNo) {
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
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    Collection<String> phoneList;
                    phoneList = read().values();
                    SMSmodel sms = new SMSmodel();
                    sms.setPhoneNum((ArrayList<String>) phoneList);
                    sms.setMessage("Βρίσκομαι στην τοποθεσία με γεωγραφικό μήκος " + longitude + " και γεωγραφικό πλάτος : " + latitude + " και χρειάζομαι βοήθεια");
                    for (String phoneNo : sms.getPhoneNum()) {
                        smsManager.sendTextMessage(phoneNo, null, sms.getMessage(), null, null);
                    }
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

}