package com.example.olla.smartassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class SendMessage extends AppCompatActivity {

    // Flag variable to display in logs...
    private static final String TAG = SendMessage.class.getName();

    // Variable to request permission for RECORD AUDIO
    private static final int REQUEST_AUDIO = 1;

    // Variable to request permission to READ CONTACTS
    private static final int READ_CONTACTS = 1;

    // Variable to verify the SMS code
    private final static int SEND_SMS_PERMISSION_REQUEST_CODE = 111;

    // Speech recognizer for callbacks
    private SpeechRecognizer messageSpeechRecognizer;

    // Intent for message speech recognition
    Intent messageSpeechIntent;

    //text to speech object for responses
    private TextToSpeech messageTTS;

    // Boolean var for keeping track of speech state
    boolean mIsListening;

    // Message Button
    private Button sendMessage;

    // Phone Number
    private EditText phone;

    // Message composed
    private EditText message;

    // Hidden textView for getting Numbers for instant Messaging
    private TextView getNumber;

    // Imageview that opens contact list when clicked
    ImageView ic_contact;

    // Input manager instance var for hiding keypad!
    InputMethodManager msgKeypadMgr;

    // Cursor instance for accessing contacts for instant Messaging
    Cursor messageContactCursor;

    // Keywords for activating message box;
    String activeMessage1 = "take message";
    String activeMessage2 = "enter message";
    String activeMessage3 = "send message to";

    // Personal information from contact list
    String getNameOnly;
    String getNumberOnly;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler messageDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        // hide the title bar on the top nav
        getSupportActionBar().hide();

        // verify audio permission when app is created, if Granted or Not
        checkAudioPermission();

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        // verify READ contacts permission, if Granted or Not
        checkContactsPermission();

        // getting view id of clickable item and menus
        sendMessage = (Button) findViewById(R.id.send_message);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTheMessage();
            }
        });

        phone = (EditText) findViewById(R.id.phone_no);
        phone.setEnabled(true);
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activatePhoneByTouch();
            }
        });

        // creating scroller for the phone entry text field to ensure text wrapping
        phone.setScroller(new Scroller(getApplicationContext()));
        phone.setVerticalScrollBarEnabled(true);
        phone.setMinLines(0);

        message = (EditText) findViewById(R.id.message);
        message.setEnabled(true);
        message.requestFocus();
        message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activateMessageByTouch();
            }
        });

        // creating scroller for the message text field to ensure text wrapping
        message.setScroller(new Scroller(getApplicationContext()));
        message.setVerticalScrollBarEnabled(true);
        message.setMinLines(0);

        // image instance for showing contact list
        ic_contact = (ImageView) findViewById(R.id.ic_contacts);
        ic_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContactList();
            }
        });

        getNumber = (TextView) findViewById(R.id.getNum);

        sendMessage.setEnabled(false);
        if (checkPermission(Manifest.permission.SEND_SMS)){
            sendMessage.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                String[] getContactInfo = result.split("\n");
                getNameOnly = getContactInfo[0];
                getNumberOnly = getContactInfo[1];

                phone.setEnabled(true);
                phone.setText(getNameOnly);
                getNumber.setText(getNumberOnly);
            }
            if (resultCode == Activity.RESULT_CANCELED){
                // Do nothing for now
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void showContactList(){
        Intent getContact = new Intent(this, Contacts.class);
        startActivityForResult(getContact, 1);
    }

    public void activatePhoneByTouch(){
        if (!phone.isEnabled()){
            phone.setEnabled(true);
        } else {
            phone.setEnabled(true);
        }
    }

    public void activateMessageByTouch(){
        if (!message.isEnabled()){
            message.setEnabled(true);
        } else {
            message.setEnabled(true);
        }
    }

    @Override
    protected void onStart() {

        // stop the running service when SendMessage is called
        stopService(new Intent(this, MyService.class));
        Log.i("Feedback", "Service has been stopped");

        messageSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(SendMessage.this);
        SpeechListener messageRecognitionListener = new SpeechListener();
        messageSpeechRecognizer.setRecognitionListener(messageRecognitionListener);
        messageSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        messageSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, SendMessage.this.getPackageName());

        messageSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        messageSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        messageSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

//        messageSpeechRecognizer.startListening(messageSpeechIntent);
//        mIsListening = true;

        messageDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                messageSpeechRecognizer.startListening(messageSpeechIntent);
                mIsListening = true;
            }
        }, 4000);

        super.onStart();
    }

    class SpeechListener extends SendMessage implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d(TAG, "speech ready");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "beginning of speech");
        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            Log.d(TAG, "buffer recieved");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "speech ending");
        }

        @Override
        public void onError(int error) {
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
                Log.d(TAG, "client error");
            } else {
                messageSpeechRecognizer.stopListening();
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (results != null){
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                processCommand(matches.get(0));
            }
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            Log.d(TAG, "partial results");
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            Log.d(TAG, "on event");
        }
    }

    private void processCommand(String command){
        command.toLowerCase();

        Log.d("the current command is ", command);

        if (command.contains("enter")){
            if (command.contains("number")){
                phone.setEnabled(true);
            }
            if (command.contains("contact")){
                phone.setEnabled(true);
            }
        }
        if (command.equals("back")){
            this.onBackPressed();
        }
        if (command.contains("enter")){
            if (command.contains("message")){
                message.setEnabled(true);
            }
        }
        if (command.contains("done") && phone.getText() != null){
            message.setEnabled(true);
        }
        if (command.contains("0") && phone.isEnabled()){
            phone.setText(command);
        }
        if (command.contains(activeMessage1) || command.contains(activeMessage2)){
            message.setEnabled(true);
        }
        if (message.isEnabled() && !command.contains(activeMessage1) && !command.contains(activeMessage2) && !command.contains(activeMessage3)){
            if (message.getText().toString().equals(command + " ")){
                message.append("");
            } else {
                message.append(command + " ");
            }
        }
        if (command.contains("finish typing")){

        }
        if (command.contains("send") && command.toString().length() > 15 && !message.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), command, Toast.LENGTH_SHORT).show();

            // Regular expressions to correct some speech faulty spellings
            if (command.contains("ray")){
                command = command.replaceAll("ray", "Ray");
            }
            if (command.contains("rate") || command.contains("Rate")){
                command = command.replaceAll("rate", "Ray");
            }
            if (command.contains("read") || command.contains("Read")){
                command = command.replaceAll("read", "Ray");
            }
            if (command.contains("Drake")){
                command = command.replaceAll("drake", "Ray");
            }
            if (command.contains("adamo")){
                command = command.replaceAll("adamo", "adama");
            }
            if (command.contains("Adamo")){
                command = command.replaceAll("Adamo", "Adama");
            }
            if(command.contains("zero") || command.contains("Zero")){
                command = command.replaceAll("zero", "0");
            }
            if (command.contains("ola")){
                command = command.replaceAll("ola", "Olla");
            }
            if (command.contains("Ola")){
                command = command.replaceAll("Ola", "Olla");
            }
            if (command.contains("Allah")){
                command = command.replaceAll("Allah", "Olla");
            }
            if (command.contains("allah")){
                command = command.replaceAll("allah", "Olla");
            }
            if (command.contains("hola")) {
                command = command.replaceAll("hola", "olla");
            }

            messageContactCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
            messageContactCursor.moveToFirst();

            while (messageContactCursor.moveToNext()) {
                String contactName = messageContactCursor.getString(messageContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNum = messageContactCursor.getString(messageContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                command.toString().toLowerCase();
                contactName.toString().toLowerCase();
                if (command.contains(contactName)) {
                    phone.setText(contactName);
                    getNumber.setText(phoneNum);
                    sendTheMessage();
                }

                //Toast.makeText(this, "Name or Number Doesn't Exist in Contacts", Toast.LENGTH_SHORT).show();
            }
            messageContactCursor.close();

            if (phone.getText().toString().isEmpty()){
                String[] getInstantNumber = command.split("to ");
                String callInstantNumber = getInstantNumber[1];
                //Toast.makeText(this, getInstantNumber[1]+" Is not a valid Name in Contacts", Toast.LENGTH_SHORT).show();


                if (callInstantNumber.contains("1") || callInstantNumber.contains("2") ||
                        callInstantNumber.contains("3") || callInstantNumber.contains("4") ||
                        callInstantNumber.contains("5") || callInstantNumber.contains("6") ||
                        callInstantNumber.contains("7") || callInstantNumber.contains("8") ||
                        callInstantNumber.contains("9") || callInstantNumber.contains("0")) {
                    phone.setText(callInstantNumber);
                    getNumber.setText(callInstantNumber);
                    sendTheMessage();
                } else {
                    speak(getInstantNumber[1] + " is not a valid name in contact list");
                }
            }
            }
        if (command.contains("send") && command.toString().length() > 15 && message.getText().toString().isEmpty()){
            speak("please enter a message");
            message.requestFocus();
        }


        messageDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                messageSpeechRecognizer.startListening(messageSpeechIntent);
                mIsListening = true;
            }
        }, 3000);
    }

    public void sendTheMessage(){
        String msg = message.getText().toString();
        String NumberOrName = phone.getText().toString();
        String phoneNumber = getNumber.getText().toString();

        // Send the message using getNumber i.e. user picked contact from Contact List
        if (!getNumber.getText().toString().isEmpty()) {

            if (checkPermission(Manifest.permission.SEND_SMS)) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber, null, msg, null, null);
                    speak("Message sent successfully to " + NumberOrName);
//                    Toast.makeText(SendMessage.this, "Message Sent Sucessfully.", Toast.LENGTH_SHORT).show();
//                    Log.d("Number is", phoneNumber);
//                    Log.d("Message is", msg);
//                    Log.d("Sms sent success ", smsManager.toString());
                } else {
                Toast.makeText(SendMessage.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }

        // Send the message using the default number user entered manually
        if (getNumber.getText().toString().isEmpty()){
            if (!phone.toString().isEmpty() && (phone.toString().contains("0") || phone.toString().contains("+"))) {
                String newNumber = phone.getText().toString();

                if (checkPermission(Manifest.permission.SEND_SMS)) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(newNumber, null, msg, null, null);
                    speak("message sent successfully to " + newNumber);
//                        Toast.makeText(SendMessage.this, "Message Sent Sucessfully.", Toast.LENGTH_SHORT).show();
//                        Log.d("Number is: ", newNumber);
//                        Log.d("Message is: ", msg);
//                        Log.d("Sms sent success ", smsManager.toString());
                } else {
                    Toast.makeText(SendMessage.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
//                  Toast.makeText(SendMessage.this, "Enter a message and a phone number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAudioPermission(){
        if (ContextCompat.checkSelfPermission(SendMessage.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(SendMessage.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else{

        }
    }

    private void checkContactsPermission(){
        if (ContextCompat.checkSelfPermission(SendMessage.this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(SendMessage.this,
                    new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
        }
    }

    private boolean checkPermission(String permission) {
        int checkSubPermission = ContextCompat.checkSelfPermission(this, permission);
        return checkSubPermission == PackageManager.PERMISSION_GRANTED;
    }


    private void initializeTextToSpeech() {
        messageTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(messageTTS.getEngines().size() == 0){
                    Toast.makeText(SendMessage.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    messageTTS.setLanguage(Locale.ENGLISH);
                    messageTTS.setSpeechRate((float)0.9);
                    speak("What should I send?");
                    // speak("I am 08133125544");
                }
            }
        });
    }



    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            messageTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            messageTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case SEND_SMS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    sendMessage.setEnabled(true);
                    Toast.makeText(SendMessage.this, "Permission Granted.", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }
}
