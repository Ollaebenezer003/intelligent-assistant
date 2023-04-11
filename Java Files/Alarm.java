package com.example.olla.smartassistant;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Alarm extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    // Flag variable for display logs
    private static final String TAG = Alarm.class.getName();

    // Variable to request audio permission
    public static final int REQUEST_AUDIO = 1;

    // Speech recognizer for callbacks
    public SpeechRecognizer alarmSpeechRecognizer;

    // Intent for recognizer callbacks
    Intent alarmSpeechIntent;

    // boolean variable to keep track of Speech state
    boolean alarmIsListening;

    //text to speech object for responses
    private TextToSpeech alarmTTS;

    // Instance variable for menu items and clickables
    private TextView alarmTextStatus;
    private TextView alarmTextStatusHint;
    private TextView hintBelowFlag;
    private ImageView alarmImageView;
    private ImageView alarmCancel;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler alarmDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // hide the title bar on the top nav bar
        getSupportActionBar().hide();

        // check audio permission for voice recognition
        checkAudioPermission();

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        // getting id of menu items
        alarmTextStatus = (TextView) findViewById(R.id.activeAlarmFlag);
        alarmTextStatusHint = (TextView) findViewById(R.id.alarmStatus);
        hintBelowFlag = (TextView) findViewById(R.id.textHints);

        // getting id of clickables
        alarmImageView = (ImageView) findViewById(R.id.ac_Alarm);
        alarmImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activateAlarm();
            }
        });

        alarmCancel = (ImageView) findViewById(R.id.de_Alarm);
        alarmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlarm();
            }
        });

    }

    @Override
    protected void onStart() {

        // stop the background running service.
        stopService(new Intent(this, MyService.class));
        Log.i("feedback!", "Service has been stopped");

        alarmSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(Alarm.this);
        SpeechListener alarmSpeechListener = new SpeechListener();
        alarmSpeechRecognizer.setRecognitionListener(alarmSpeechListener);
        alarmSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        alarmSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Alarm.this.getPackageName());

        alarmSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        alarmSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        alarmSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);



        alarmDelayHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                alarmSpeechRecognizer.startListening(alarmSpeechIntent);
                //hintBelowFlag.setText("Listening");
                alarmIsListening = true;
            }
        }, 5000);

        super.onStart();
    }

    class SpeechListener extends Alarm implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d(TAG, " ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "speech beginning");
        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            Log.d(TAG, "buffer received");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "speech ended");
        }

        @Override
        public void onError(int error) {
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
                Log.d(TAG, "client error");
            } else{
                if (alarmIsListening){
                    alarmSpeechRecognizer.stopListening();
                    alarmIsListening = false;
                }
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (results != null){
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                processCommand(matches.get(0));
//                if (alarmIsListening){
//                    // do nothing for now
//                }
            }
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            Log.d(TAG, "partial results");
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            Log.d(TAG, "on events");
        }
    }

    private void processCommand(String command) {
        command.toLowerCase();

        alarmTextStatusHint.setText(command);

        Toast.makeText(this, command, Toast.LENGTH_SHORT).show();

        boolean containNumber = command.contains("0") || command.contains("1") || command.contains("2")
                || command.contains("3") || command.contains("4") || command.contains("5")
                || command.contains("6") || command.contains("7") || command.contains("8")
                || command.contains("9");

        if (command.contains("thank you")){
            alarmSpeechRecognizer.stopListening();
            alarmSpeechRecognizer.cancel();
            alarmSpeechRecognizer.destroy();
        }

        if (command.contains("alarm")){
            if (command.contains("for")){

                // if the command contains "Set Alarm For" Keywords but does not contain ":"
                if (!command.contains(":")){
                    String[] splitCommand = command.split("for ");
                    String getTime = splitCommand[1];

                    // separating the Hour and the Minute
                    String[] actualTime = getTime.split(" ");
                    String hourDay = actualTime[0];
                    String minuteDay = actualTime[1];

                    // convert String Hour and Minute to Integers
                    int convertedHour = Integer.parseInt(hourDay);
                    int convertedMinute = Integer.parseInt(minuteDay);

                    // set the converted Hour and Minute to the Calendar object
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, convertedHour);
                    c.set(Calendar.MINUTE, convertedMinute);
                    c.set(Calendar.SECOND, 0);

                    // start alarm and update time to the desired alarm time
                    updateTimeText(c);
                    startAlarm(c);
                }

                // if the alarm contains "Set Alarm For" keywords and ":"
                else if (command.contains(":")){
                    String[] splitCommand = command.split("for ");
                    String getTime = splitCommand[1];

                    // separating the Hour and the Minute
                    String[] actualTime = getTime.split(":");
                    String hourDay = actualTime[0];
                    String minuteDay = actualTime[1];

                    // convert String Hour and Minute to Integers
                    int convertedHour = Integer.parseInt(hourDay);
                    int convertedMinute = Integer.parseInt(minuteDay);

                    // set the converted Hour and Minute to the Calendar object
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, convertedHour);
                    c.set(Calendar.MINUTE, convertedMinute);
                    c.set(Calendar.SECOND, 0);

                    // start alarm and update time to the desired alarm time
                    updateTimeText(c);
                    startAlarm(c);
                }
                else {
                    speak("Invalid time format for alarm!");
                }
            }
        }

        // if the command does not contain ==> "Start Alarm For" <== Keywords
        // and also DOES NOT contain ":" as in cases of "16 23", "15 30", etc...
        if (!command.contains("alarm for") && !command.contains("wake")){
            if (containNumber){
                if (command.contains(" ")){
                    String[] getTime = command.split(" ");

                    // separating the Hour and Minute
                    String hourDay = getTime[0];
                    String minuteDay = getTime[1];

                    if (minuteDay.contains("a.m.")){
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
//                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, 00);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateTimeText(c);
                        startAlarm(c);
                    }
                    else if (minuteDay.contains("p.m.")){
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
//                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, 12+convertedHour);
                        c.set(Calendar.MINUTE, 00);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateTimeText(c);
                        startAlarm(c);
                    }
                    else{
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, convertedMinute);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateTimeText(c);
                        startAlarm(c);
                    }

                }
                else if (!command.contains(" ")){
                    char [] getTime = command.toCharArray();
                    if (getTime.length == 3 && !command.contains(":")){
                        String hourDay = String.valueOf(getTime[0]);
                        String minuteDay = String.valueOf(getTime[1]) + String.valueOf(getTime[2]);

                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, convertedMinute);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateTimeText(c);
                        startAlarm(c);
                    }
                    else if (getTime.length == 4 && !command.contains(":")){
                        String hourDay = String.valueOf(getTime[0]) + String.valueOf(getTime[1]);
                        String minuteDay = String.valueOf(getTime[2]) + String.valueOf(getTime[3]);

                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, convertedMinute);
                        c.set(Calendar.SECOND, 0);

                        // start the alarm and update time to the desired alarm time
                        updateTimeText(c);
                        startAlarm(c);

                    }
                    else if (getTime.length > 4){
                        speak("Invalid time format!");
                        Toast.makeText(this, "Invalid Time Format", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        speak("Invalid time format for alarm");
                    }
                }
            }
        }

        if (!command.contains("alarm for") && !command.contains("thank you")){
            if (!containNumber){
                speak("No time format found!");
                Toast.makeText(this, "No time format found", Toast.LENGTH_SHORT).show();
            }
        }

        // if command does not contain ==> "Start Alarm For" <== Keywords
        // BUT contains ":" as in the cases of "9:45", "10:30", etc...
        if (!command.contains("alarm for") && command.contains(":")){
            if (containNumber){
                String[] getTime = command.split(":");

                // separate the Hour and the Minute
                String hourDay = getTime[0];
                String minuteDay = getTime[1];

                // convert String Hour and Minute to Integers
                int convertedHour = Integer.parseInt(hourDay);
                int convertedMinute = Integer.parseInt(minuteDay);

                // set the converted Hour and Minute to the Calendar object
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, convertedHour);
                c.set(Calendar.MINUTE, convertedMinute);
                c.set(Calendar.SECOND, 0);

                // start the alarm and update time to the desired alarm time
                updateTimeText(c);
                startAlarm(c);
            }
        }

        // if command does not contain ==> "Start Alarm For && Wake Me Up" <== Keywords
        // BUT contains "time" as in the cases of "9 a.m.", "10 p.m.", etc...
        if (!command.contains("alarm for") && !command.contains(":")) {
            if (containNumber && command.contains(" ")){
                // separating the Hour and the Minute
                String[] actualTime = command.split(" ");
                String hourDay = actualTime[0];
                String minuteDay = actualTime[1];

                if (minuteDay.contains("a.m.")){
                    // convert String Hour and Minute to Integers
                    int convertedHour = Integer.parseInt(hourDay);
//                        int convertedMinute = Integer.parseInt(minuteDay);

                    // set the converted Hour and Minute to the Calendar object
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, convertedHour);
                    c.set(Calendar.MINUTE, 00);
                    c.set(Calendar.SECOND, 0);

                    // start alarm and update time to the desired alarm time
                    updateTimeText(c);
                    startAlarm(c);
                }
                else if (minuteDay.contains("p.m.")){
                    // convert String Hour and Minute to Integers
                    int convertedHour = Integer.parseInt(hourDay);
                    //int convertedMinute = Integer.parseInt(minuteDay);

                    // set the converted Hour and Minute to the Calendar object
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, 12+convertedHour);
                    c.set(Calendar.MINUTE, 00);
                    c.set(Calendar.SECOND, 0);

                    // start alarm and update time to the desired alarm time
                    updateTimeText(c);
                    startAlarm(c);
                }
                else {
                    speak("Invalid time format for alarm!");
                }
            }
            else{
                //speak("Invalid time format for alarm!");
            }
        }


        // if command contain ==> "Wake Me Up Tomorrow" <== Keywords
        // BUT contains "time" as in the cases of "9 a.m.", "10 p.m.", etc...
        if (command.contains("wake me")){
            if (command.contains("tomorrow at")){

                // if the command contains "Wake Me Up Tomorrow" Keywords but does not contain ":"
                if (!command.contains(":")){
                    String[] splitCommand = command.split("at ");
                    String getTime = splitCommand[1];

                    // separating the Hour and the Minute
                    String[] actualTime = getTime.split(" ");
                    String hourDay = actualTime[0];
                    String minuteDay = actualTime[1];

                    if (minuteDay.contains("a.m.")){
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
//                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, 00);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateNextDayTimeText(c);
                        startNextDayAlarm(c);
                    }
                    else if (minuteDay.contains("p.m.")){
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
                        //int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, 12+convertedHour);
                        c.set(Calendar.MINUTE, 00);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateNextDayTimeText(c);
                        startNextDayAlarm(c);
                    }
                    else {
                        // convert String Hour and Minute to Integers
                        int convertedHour = Integer.parseInt(hourDay);
                        int convertedMinute = Integer.parseInt(minuteDay);

                        // set the converted Hour and Minute to the Calendar object
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, convertedHour);
                        c.set(Calendar.MINUTE, convertedMinute);
                        c.set(Calendar.SECOND, 0);

                        // start alarm and update time to the desired alarm time
                        updateNextDayTimeText(c);
                        startNextDayAlarm(c);
//                        speak("Invalid time format for alarm!");
                    }
                }

                // if the alarm contains "Wake me up tomorrow" keywords and ":"
                else if (command.contains(":")){
                    String[] splitCommand = command.split("at ");
                    String getTime = splitCommand[1];

                    // separating the Hour and the Minute
                    String[] actualTime = getTime.split(":");
                    String hourDay = actualTime[0];
                    String minuteDay = actualTime[1];

                    // convert String Hour and Minute to Integers
                    int convertedHour = Integer.parseInt(hourDay);
                    int convertedMinute = Integer.parseInt(minuteDay);

                    // set the converted Hour and Minute to the Calendar object
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, convertedHour);
                    c.set(Calendar.MINUTE, convertedMinute);
                    c.set(Calendar.SECOND, 0);

                    // start alarm and update time to the desired alarm time
                    updateTimeText(c);
                    startAlarm(c);
                }
                else {
                    speak("Invalid time format for alarm!");
                }
            }
        }



        if (command.contains("cancel") || command.contains("stop")){
            if (command.contains("alarm")){
                cancelAlarm();
            }
        }


        if (command.contains("back")){
            this.onBackPressed();
        }

        alarmDelayHandler.postDelayed(new Runnable(){

            @Override
            public void run() {
                alarmSpeechRecognizer.startListening(alarmSpeechIntent);
                alarmIsListening = true;
                //hintBelowFlag.setText("Listening...");
            }
        }, 3000);


    }

    public void activateAlarm(){
        DialogFragment timepicker = new TimePickerFragment();
        timepicker.show(getSupportFragmentManager(), "time picker");
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        updateTimeText(c);
        startAlarm(c);
    }

    private void updateTimeText(Calendar c){
        String timeText;
        timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());

        alarmTextStatus.setText(timeText);
        alarmTextStatusHint.setText("");
        hintBelowFlag.setText("Active alarm");
        alarmImageView.setImageResource(R.mipmap.ic_alarmsuccess);
        speak("Alarm has been set for " + timeText);
    }

    private void startAlarm(Calendar c){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, AlarmAlertReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, 0);

        if (c.before(Calendar.getInstance())){
            c.add(Calendar.DATE, 1);
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), alarmPendingIntent);

    }

    private void updateNextDayTimeText(Calendar c){
        String timeText;
        timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());

        alarmTextStatus.setText("Tomorrow at " + timeText);
        alarmTextStatusHint.setText("");
        hintBelowFlag.setText("Active alarm");
        alarmImageView.setImageResource(R.mipmap.ic_alarmsuccess);
        speak("Alarm has been set for tomorrow at " + timeText);
    }

    private void startNextDayAlarm(Calendar c){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, AlarmAlertReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, 0);
        c.add(Calendar.DATE, 1);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), alarmPendingIntent);

    }

    public void cancelAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, AlarmAlertReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, 0);

        alarmManager.cancel(alarmPendingIntent);
        alarmTextStatus.setText("No Active Alarm!");
        alarmTextStatusHint.setText("Touch to activate alarm");
        hintBelowFlag.setText("set alarm");

        alarmImageView.setImageResource(R.mipmap.ic_activatealarm);

        speak("Alarm has been canceled!");
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
    }

    private void checkAudioPermission(){
        if (ContextCompat.checkSelfPermission(Alarm.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Alarm.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else {
            // Do nothing for now
        }
    }

    private void initializeTextToSpeech() {
        alarmTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(alarmTTS.getEngines().size() == 0){
                    Toast.makeText(Alarm.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    alarmTTS.setLanguage(Locale.ENGLISH);
                    alarmTTS.setSpeechRate((float)0.9);
                    speak("What time should I set the Alarm?");
                    // speak("I am 08133125544");
                }
            }
        });
    }



    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            alarmTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            alarmTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    protected void onPause() {
        alarmTTS.stop();
        alarmTTS.shutdown();
        alarmDelayHandler.removeCallbacksAndMessages(null);
        super.onPause();
        alarmSpeechRecognizer.stopListening();
    }

    @Override
    protected void onDestroy() {
        alarmTTS.stop();
        alarmTTS.shutdown();
        alarmDelayHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
        alarmSpeechRecognizer.stopListening();
    }
}
