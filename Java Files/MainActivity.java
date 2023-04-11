package com.example.olla.smartassistant;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.olla.smartassistant.MyService;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // A Tag for displaying Logs on the console
    private static final String TAG = MainActivity.class.getName();

    // REQUEST VARIABLE for verifying audio permissions, i.e. granted or not...
    private static final int REQUEST_AUDIO = 1;

    // Request for permission to READ contact list
    private static final int READ_CONTACTS = 1;

    // A flag variable for binding activities to service...
    private int mBindFlag;

    // A messenger instace for sending and messages to service activities...
    private Messenger mServiceMessenger;

    //text to speech object for responses
    private TextToSpeech mainTTS;

    // An instance of my notification Textfield...
    private TextView speechStatusFlag;

    // The service Intent that gets called to either Start or Stop the Service...
    Intent serviceIntent;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler mainDelayHandler = new Handler();

    // Variables for accessing views and clickable menus...
    ImageView ic_mic;
    ImageView ic_mute;
    ImageView ic_help;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hid the title bar of the app
        getSupportActionBar().hide();

        // Instantiating the variables for the views and the clickable menus...
        ic_mic = (ImageView) findViewById(R.id.ic_mic_speak);
        ic_mute = (ImageView) findViewById(R.id.ic_mic_mute);

        // Instantiating the help view to access the help section in the App...
        ic_help = (ImageView) findViewById(R.id.ic_help);
        ic_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenHelpView();
            }
        });

        // calling all functions and methods for checking permissions...
        checkAudioPermission();

        // check that read contacts permission has been granted
        checkContactsPermission();

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        // Create the serviceIntent action and pass data to it...
        serviceIntent = new Intent(MainActivity.this, MyService.class);
        //startService(serviceIntent);

        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;

        // Set listener for clicks =>> starts service when clicked...
        ic_mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(serviceIntent);
            }
        });

        // Set listener for clicks =>> stops service when clicked...
        ic_mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(serviceIntent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, MyService.class), mServiceConnection, mBindFlag);

        // start the service after 4secs of launch...
        mainDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(serviceIntent);
            }
        }, 3000);
    }

    @Override
    protected void onStop() {
        super.onStop();

//        if (mServiceMessenger != null){
//            unbindService(mServiceConnection);
//            mServiceMessenger = null;
//        }
        mainTTS.stop();
        mainTTS.shutdown();
        unbindService(mServiceConnection);
    }

    @Override
    protected void onPause() {
        mainTTS.stop();
        mainTTS.shutdown();
        super.onPause();
//        unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // if (DEBUG) {Log.d(TAG, "onServiceConnected");}

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = MyService.MSG_RECOGNIZER_START_LISTENING;
            try{
                mServiceMessenger.send(msg);
            } catch (RemoteException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // if (DEBUG) {Log.d(TAG, "onServiceDisconnected"); }
            mServiceMessenger = null;

        }
    };


    public void OpenHelpView(){
        Intent openView = new Intent(this, Help.class);
        startActivity(openView);
    }


    private void initializeTextToSpeech() {
        mainTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(mainTTS.getEngines().size() == 0){
                    Toast.makeText(MainActivity.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    mainTTS.setLanguage(Locale.ENGLISH);
                    mainTTS.setSpeechRate((float)0.9);
                    speak("What can I do for you?");
                    // speak("I am 08133125544");
                }
            }
        });
    }

    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            mainTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            mainTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    private boolean checkServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null){
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
                if (service.service.getClassName().equals("com.example.olla.olla.MyService")){
                    return true;
                }
            }
        }
        return false;
    }


    public void checkAudioPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else {

            // DO NOTHING FOR NOW
        }
    }


    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
        }
    }

    public String getServiceIntent(){
        return String.valueOf(serviceIntent);
    }

//    private void enableAutoStart() {
//        for (Intent intent: Constants.AUTO_START_INTENTS){
//            if(getPackageManager().resolveActivity(intent,
//                    PackageManager.MATCH_DEFAULT_ONLY) != null){
//                new Builder(this).title("Enable AutoStart")
//                        .content("Please allow Speech to always run in the background,else our services can\\'t be accessed when you are in distress")
//                        .positiveText("allow")
//                        .onPositive((dialog, which) -> {
//                            try{
//                                for (Intent intent1: Constants.AUTO_START_INTENTS)
//                                    if (getPackageManager().resolveActivity(intent1, PackageManager.MATCH_DEFAULT_ONLY) != null){
//                                        startActivity(intent1);
//                                        break;
//                                    }
//                            } catch (Exception e){
//                                e.printStackTrace();
//                            }
//                        })
//                        .show();
//                break;
//            }
//        }
//    }


//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//    }
}
