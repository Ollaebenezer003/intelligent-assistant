package com.example.olla.smartassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.example.olla.smartassistant.MainActivity;

public class MyService extends Service {

    // A vibrator variable to make the phone vibrate when re-activating the service..
    public static Vibrator serviceVibrator;

    // Speech Managers
    protected AudioManager serviceAudioManager;
    protected SpeechRecognizer serviceSpeechRecognizer;
    protected Intent serviceSpeechRecognizerIntent;

    // Messenger Handler for listening to messages...
    protected final Messenger serviceMessenger = new Messenger(new IncomingHandler(this));

    // Boolean variable to get current state of speech...
    protected boolean IsListening;

    // Variable to create a countdown timer before re-activating the service
    protected volatile boolean IsCountDownOn;

    // Variable for turning OFF Beep Sound while service is running in the background...
    private boolean IsStreamSolo;

    // Message variable to verify listening state of the Speech Recognizer...
    static final int MSG_RECOGNIZER_START_LISTENING = 1;

    // Message variable to verify that Speech Recognizer has been canceled...
    static final int MSG_RECOGNIZER_CANCEL = 2;

    // An instance of my notification Textfield...
    private TextView speechStatusFlag ;


    @Override
    public void onCreate() {
        super.onCreate();

        // instantiate the status flag...

        // Initializing the Audio Manager...
        serviceAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initializing Speech Recognizer and Speech Intent...
        serviceSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        serviceSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        serviceSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        serviceSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        serviceSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

    }

    // im not quite sure which handler class to import her. PS: i imported the OS_Handler...
    protected static class IncomingHandler extends Handler {

        // A target variable of the referenced service to trigger activities...
        private WeakReference<MyService> mtarget;

        IncomingHandler (MyService target){
            mtarget = new WeakReference<MyService>(target);
        }


        @Override
        public void handleMessage(Message msg) {
            final MyService target = mtarget.get();

            switch (msg.what){
                case MSG_RECOGNIZER_START_LISTENING:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        // turn of beep sound

                        // target.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
//                            if (!target.mIsStreamSolo){
//                                target.mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
//                                target.mIsStreamSolo = true;
//                            }
                    }
                    if (target.IsListening == false){
                        target.serviceSpeechRecognizer.startListening(target.serviceSpeechRecognizerIntent);
                        target.IsListening = true;
//                        target.speechStatusFlag.setHint("Listening...");
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
//                    if (target.mIsStreamSolo) {
//                            target.mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
//                            target.mIsStreamSolo = false;
//                        }

                    target.serviceSpeechRecognizer.cancel();
                    target.IsListening = false;

                    break;
            }

            // super.handleMessage(msg);
        }
    }

    // Count down timer for Jelly Bean work around...
    protected CountDownTimer NoSpeechCountDown = new CountDownTimer(5000, 5000) {
        @Override
        public void onTick(long milliUntilFinished) {
            // Auto-generated method stub
        }

        @Override
        public void onFinish() {
//            IsCountDownOn = false;

            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try{
                serviceMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                serviceMessenger.send(message);
            } catch (RemoteException e){
                // Do Nothing For Now...
            }

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (IsCountDownOn){
            NoSpeechCountDown.cancel();
        }
        if (serviceSpeechRecognizer != null){
            serviceSpeechRecognizer.destroy();
        }
    }


    protected class SpeechRecognitionListener implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                IsCountDownOn = true;
                NoSpeechCountDown.start();
            }
            Log.d(MyService.class.getName(), "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            // speech input will be processed, so there is no need for count down anymore...
            if (IsCountDownOn){
                IsCountDownOn = false;
                NoSpeechCountDown.cancel();
            }

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
            if (IsCountDownOn){
                IsCountDownOn = false;
                NoSpeechCountDown.cancel();
            }
            IsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);

            try{
                serviceMessenger.send(message);
            } catch (RemoteException e){
                // Do Nothing For Now...
            }

        }

        @Override
        public void onResults(Bundle results) {
            List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            processResults(matches.get(0));

        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }

    private void processResults(String command) {
        command.toLowerCase();
        Log.i("the currentcommand is ", command);
        Toast.makeText(this, command, Toast.LENGTH_SHORT).show();

        // Regular expressions to correct some speech faulty spellings
        command.toLowerCase();

        if (command.contains("hi") || command.contains("hey") || command.contains("hi assistant") || command.contains("hey assistant")){
            //serviceSpeechRecognizer.stopListening();
            Intent launchApp = getPackageManager().getLaunchIntentForPackage("com.example.olla.smartassistant");
            startActivity(launchApp);

        }
        if (command.contains("what")){
            if (command.contains("your name")){
                Toast.makeText(this, "I am your smart assistant", Toast.LENGTH_SHORT).show();
            }
            if (command.contains("time")){
                Date newTime = new Date();
                String actualTime = DateUtils.formatDateTime(this, newTime.getTime(), DateUtils.FORMAT_SHOW_TIME);
                Toast.makeText(this, actualTime, Toast.LENGTH_SHORT).show();
            }
        }
        if (command.contains("stop service")){

        }
        if (command.contains("call") || command.contains("call adama")){
            Intent openCall = new Intent(this, Dialer.class);
            openCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(openCall);
        }
        if (command.contains("send") || command.contains("compose")){
            if (command.contains("message")){
                Intent openMessage = new Intent(this, SendMessage.class);
                openMessage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(openMessage);
            }
        }
        if (command.contains("alarm") || command.contains("set an alarm for") || command.contains("set alarm")){
            Intent setAlarm = new Intent(this, Alarm.class);
            setAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(setAlarm);
        }
        if (command.contains("contacts") || command.contains("contact list")){
            Intent openContact = new Intent(this, Contacts.class);
            openContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(openContact);
        }
        if (command.contains("send a mail") || command.contains("send email") || command.contains("send mail")){
            Intent sendMail = new Intent(this, MailClass.class);
            sendMail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(sendMail);
        }
        if (command.contains("show my inbox")){
            Intent openInbox = new Intent(this, MessageInbox.class);
            openInbox.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(openInbox);
        }
        if (command.contains("launch") || command.contains("open")){
            if (command.contains("adobe") || command.contains("Adobe")){
                Intent launchAdobe = getPackageManager().getLaunchIntentForPackage("com.adobe.reader");
                startActivity(launchAdobe);
            }
            if (command.contains("chrome") || command.contains("Chrome")){
                Intent launchChrome = getPackageManager().getLaunchIntentForPackage("com.android.chrome");
                startActivity(launchChrome);
            }
            if (command.contains("facebook") || command.contains("Facebook")){
                Intent launchFacebook = getPackageManager().getLaunchIntentForPackage("com.facebook.katana");
                startActivity(launchFacebook);
            }
            if (command.contains("gallery") || command.contains("Gallery")){
                Intent launchGallery = getPackageManager().getLaunchIntentForPackage("com.gionee.gallery");
                startActivity(launchGallery);
            }
            if (command.contains("instagram") || command.contains("Instagram")){
                Intent launchInstagram = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
                startActivity(launchInstagram);
            }
            if (command.contains("opera mini") || command.contains("Opera Mini")){
                Intent launchOpera = getPackageManager().getLaunchIntentForPackage("com.opera.mini.native");
                startActivity(launchOpera);
            }
            if (command.contains("twitter") || command.contains("Twitter")){
                Intent launchTwitter = getPackageManager().getLaunchIntentForPackage("com.twitter.android");
                startActivity(launchTwitter);
            }
            if (command.contains("what up") || command.contains("whatsapp")
                    || command.contains("Whatsapp") || command.contains("WhatsApp")){
                Intent launchWhatsapp = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                startActivity(launchWhatsapp);
            }
            if (command.contains("snapchat") || command.contains("Snapchat")){
                Intent launchSnapchat = getPackageManager().getLaunchIntentForPackage("com.snapchat.android");
                startActivity(launchSnapchat);
            }
            if (command.contains("youtube") || command.contains("Youtube")){
                Intent launchYoutube = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                startActivity(launchYoutube);
            }
        }
//        if (command.contains("launch facebook") || command.contains("launch Facebook")){
//            Intent launchFacebook = getPackageManager().getLaunchIntentForPackage("com.facebook.katana");
//            startActivity(launchFacebook);
//        }
//        if (command.contains("launch what up") || command.contains("launch whatsapp")){
////            Intent launchWhatsapp = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
////            startActivity(launchWhatsapp);
//        }

        //=============================================================================
        //ADD THE START LISTENING METHOD HERE TO ENSURE CONTINUOUS LISTENING EVEN AFTER
        //THE CURRENT COMMANDS HAVE BEEN PROCESSED
        //========================================

        serviceSpeechRecognizer.startListening(serviceSpeechRecognizerIntent);
        IsListening = true;
//        speechStatusFlag.setHint("Listening...");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service is running!", Toast.LENGTH_SHORT).show();
        if (!IsListening){
            serviceSpeechRecognizer.startListening(serviceSpeechRecognizerIntent);
            IsListening = true;
//            speechStatusFlag.setHint("Listening...");
            Log.d(MyService.class.getName(), "Listening");
            Toast.makeText(this, "Listening", Toast.LENGTH_SHORT).show();
        }

        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//                ((AudioManager) Objects.requireNonNull(
//                        getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {

        // Restarting the service if ot gets removed by the android system...
        // ==================================================================

        // Creating a Pending Intent that gets fired when it is time to wake up the service...
        PendingIntent servicePendingIntent = PendingIntent.getService(getApplicationContext(), new Random().nextInt(),
                new Intent(getApplicationContext(), MyService.class), PendingIntent.FLAG_ONE_SHOT);

        // An alarm instance that wakes the phone and activates the service when fired!!!
        AlarmManager serviceWakeUp = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        assert serviceWakeUp != null;
        serviceWakeUp.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, servicePendingIntent);

        // Vibrator for notification in case the phone is on silence or in a pocket...
        serviceVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        serviceVibrator.vibrate(1000);


        super.onTaskRemoved(rootIntent);
    }

}
