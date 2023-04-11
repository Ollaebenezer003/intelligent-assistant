package com.example.olla.smartassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.olla.smartassistant.MainActivity.*;

import java.util.List;
import java.util.Locale;

public class Dialer extends AppCompatActivity implements View.OnClickListener {

    // Flag for displaying logs
    private static final String TAG = Dialer.class.getName();

    // request for RECORD AUDIO permission
    public static final int REQUEST_AUDIO = 1;

    // Request for CALL permission
    private static final int REQUEST_CALL = 1;

    // Request for permission to READ contact list
    private static final int READ_CONTACTS = 1;

    // Speech Recognizer for callbacks
    public SpeechRecognizer dialerSpeechRecogizer;

    // Intent for speech recognition
    Intent dialerSpeechIntent;

    // Boolean variable to keep track of Speech State
    boolean dialerIsListening;

    //text to speech object for responses
    private TextToSpeech dialerTTS;

    // Input manager variable for hiding the keypad
    InputMethodManager dialerKeypadMgr;

    // Cursor instance for accessing contacts for instant Phone Calls
    Cursor dialerContactsCursor;

    // Personal information from contact list
    String getNameOnly;
    String getNumberOnly;

    // View variables for onScreen elements/objects
    EditText screen;
    TextView getDialerNumber;
    ImageView delBtn;
    ImageView callBtn;
    Button plusPress;
    ImageView ic_contacts;

    // Instances of other imported classes
    MainActivity inst;
    MyService obj;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler dialerDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);

        // Hide the title bar on the app top nav...
        getSupportActionBar().hide();

        // check that read contacts permission has been granted
        checkContactsPermission();

//        stopService(new Intent(this, MyService.class));
//        Log.i("Feedback", "Service has been stopped");

        // Accessing MainActivity variables.
        //inst = ((MainActivity)getApplicationContext());

        // using the MainActivity instance to check for AUDIO permission
        checkAudioPermission();

        // initialize views for clickable buttons
        initializeView();

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        // get View id of objects and onScreen elements
        screen = (EditText) findViewById(R.id.Display);
        screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideInputManager();
            }
        });

        delBtn = (ImageView) findViewById(R.id.DelEntry);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteEntry();
            }
        });
        delBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                screen.setText("");
                getDialerNumber.setText("");
                return true;
            }
        });

        callBtn = (ImageView) findViewById(R.id.ic_call);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeCall();
            }
        });

        getDialerNumber = (TextView) findViewById(R.id.getActualNum);

        plusPress = (Button) findViewById(R.id.btn0);
        plusPress.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                screen.append("+");
                return true;
            }
        });

        ic_contacts = (ImageView) findViewById(R.id.ic_contacts);
        ic_contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContactList();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                String[] getContactInfo = result.split("\n");
                getNameOnly = getContactInfo[0];
                getNumberOnly = getContactInfo[1];

                //screen.setEnabled(true);
                screen.setText(getNameOnly);
                getDialerNumber.setText(getNumberOnly);
                screen.setSelection(screen.getText().length());
            }
            if (resultCode == Activity.RESULT_CANCELED){
                // Do nothing for now
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showContactList(){
        Intent getContact = new Intent(this, Contacts.class);
        startActivityForResult(getContact, 1);
    }

    public void hideInputManager(){
        dialerKeypadMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        dialerKeypadMgr.hideSoftInputFromWindow(screen.getWindowToken(), 0);

//        if (mgr.hideSoftInputFromWindow(screen.getWindowToken(), 0)){
//
//        }
    }

    private void deleteEntry() {
        String newText;
        if (screen.getText().toString().length() >= 1){

            int cursorPosition = screen.getSelectionEnd();
            if(cursorPosition > 0){
                newText = screen.getText().delete(cursorPosition -1, cursorPosition).toString();
                getDialerNumber.setText("");
                screen.setText(newText);
                screen.setSelection(cursorPosition -1);
            }
        } else{

        }
    }

    private void initializeView() {
        int idList[] = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnAsterick, R.id.btnHash};


        for(int idCounter : idList){
            View v = (View) findViewById(idCounter);
            v.setOnClickListener(Dialer.this);
        }
    }

    public void setDisplay(String value){
        screen.append(value);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn0:
                setDisplay("0");
                break;
            case R.id.btn1:
                setDisplay("1");
                break;
            case R.id.btn2:
                setDisplay("2");
                break;
            case R.id.btn3:
                setDisplay("3");
                break;
            case R.id.btn4:
                setDisplay("4");
                break;
            case R.id.btn5:
                setDisplay("5");
                break;
            case R.id.btn6:
                setDisplay("6");
                break;
            case R.id.btn7:
                setDisplay("7");
                break;
            case R.id.btn8:
                setDisplay("8");
                break;
            case R.id.btn9:
                setDisplay("9");
                break;
            case R.id.btnAsterick:
                setDisplay("*");
                break;
            case R.id.btnHash:
                setDisplay("#");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStart() {

//        stopService(new Intent(this, MyService.class));
        stopService(new Intent(this, MyService.class));
        Log.i("Feedback", "Service has been stopped");

        dialerSpeechRecogizer = SpeechRecognizer.createSpeechRecognizer(this);
        SpeechListener dialerRecognitionListener = new SpeechListener();
        dialerSpeechRecogizer.setRecognitionListener(dialerRecognitionListener);
        dialerSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        dialerSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Dialer.this.getPackageName());

        // giving hint to the recognizer about what the user might say.
        dialerSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // specification of number of results to be processed
        dialerSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        dialerSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

//        dialerSpeechRecogizer.startListening(dialerSpeechIntent);
//        dialerIsListening = true;

        dialerDelayHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                dialerSpeechRecogizer.startListening(dialerSpeechIntent);
                dialerIsListening = true;
            }
        }, 5000);

        super.onStart();
    }

    class SpeechListener extends Dialer implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d(TAG, "Ready for speech");
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
            Log.d(TAG, "buffer received");
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
                if (dialerIsListening != false){
                    dialerSpeechRecogizer.stopListening();
                    dialerIsListening = false;
                }
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

    private void processCommand(String command) {
        command.toLowerCase();
        Log.d("The current command is ", command);

        Toast.makeText(this, command, Toast.LENGTH_SHORT).show();

        if (command.contains("back")){
            this.onBackPressed();
        }
        if (command.equals("0") || command.equals("zero")){
            screen.append("0");
        }
        if (command.equals("1") || command.equals("one")){
            screen.append("1");
        }
        if (command.equals("2") || command.equals("two")){
            screen.append("2");
        }
        if (command.equals("3") || command.equals("three")){
            screen.append("3");
        }
        if (command.equals("4") || command.equals("four")){
            screen.append("4");
        }
        if (command.equals("5") || command.equals("five")){
            screen.append("5");
        }
        if (command.equals("6") || command.equals("six")){
            screen.append("6");
        }
        if (command.equals("7") || command.equals("seven")){
            screen.append("7");
        }
        if (command.equals("8") || command.equals("eight")){
            screen.append("8");
        }
        if (command.equals("9") || command.equals("nine")){
            screen.append("9");
        }
        if (command.equals("+") || command.equals("plus")){
            screen.append("+");
        }
        if (command.indexOf("0") != -1){
//        || command.indexOf("zero") != -1){
            screen.setText(command);
        }
        if ((command.equals("call") || command.contains("dial")) && command.toString().length() < 5){
            makeCall();
        }
        if (command.contains("call") && command.toString().length() > 5){

            if (command.contains("ray")){
                command = command.replaceAll("ray", "Ray");
            }
            if (command.contains("adamo")){
                command = command.replaceAll("adamo", "Adama");
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

            dialerContactsCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
            dialerContactsCursor.moveToFirst();

            while(dialerContactsCursor.moveToNext()) {
                String contactName = dialerContactsCursor.getString(dialerContactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNum = dialerContactsCursor.getString(dialerContactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                command.toString().toLowerCase();
                contactName.toString().toLowerCase();
                if (command.contains(contactName)) {
                    screen.setText(contactName);
                    getDialerNumber.setText(phoneNum);
                    makeInstantCall();
                }

                //Toast.makeText(this, "Name or Number Doesn't Exist in Contacts", Toast.LENGTH_SHORT).show();
            }

            dialerContactsCursor.close();

            if (screen.getText().toString().isEmpty()){
                String[] getInstantNumber = command.split("call ");
                String callInstantNumber = getInstantNumber[1];
                //Toast.makeText(this, getInstantNumber[1]+" Is not a valid Name in Contacts", Toast.LENGTH_SHORT).show();


                if (callInstantNumber.contains("1") || callInstantNumber.contains("2") ||
                        callInstantNumber.contains("3") || callInstantNumber.contains("4") ||
                        callInstantNumber.contains("5") || callInstantNumber.contains("6") ||
                        callInstantNumber.contains("7") || callInstantNumber.contains("8") ||
                        callInstantNumber.contains("9") || callInstantNumber.contains("0")) {
                    screen.setText(callInstantNumber);
                    getDialerNumber.setText(callInstantNumber);
                    makeInstantCall();
                } else {
                    speak(getInstantNumber[1] + " is not a valid name in contact list");
                }
            }



        }
        if (!command.contains("call") && command.toString().length() > 1){

            // Regular expressions to correct some speech faulty spellings
            if (command.contains("ray")){
                command = command.replaceAll("ray", "Ray");
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

            dialerContactsCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
            dialerContactsCursor.moveToFirst();

            while(dialerContactsCursor.moveToNext()) {
                String contactName = dialerContactsCursor.getString(dialerContactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNum = dialerContactsCursor.getString(dialerContactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                command.toString().toLowerCase();
                contactName.toString().toLowerCase();
                if (command.contains(contactName)) {
                    screen.setText(contactName);
                    getDialerNumber.setText(phoneNum);
                    makeInstantCall();
                }

                //Toast.makeText(this, "Name or Number Doesn't Exist in Contacts", Toast.LENGTH_SHORT).show();
            }

            dialerContactsCursor.close();

            if (screen.getText().toString().isEmpty()){
//                String[] getInstantNumber = command.split("call ");
                String callInstantNumber = command;
                //Toast.makeText(this, getInstantNumber[1]+" Is not a valid Name in Contacts", Toast.LENGTH_SHORT).show();


                if (callInstantNumber.contains("1") || callInstantNumber.contains("2") ||
                        callInstantNumber.contains("3") || callInstantNumber.contains("4") ||
                        callInstantNumber.contains("5") || callInstantNumber.contains("6") ||
                        callInstantNumber.contains("7") || callInstantNumber.contains("8") ||
                        callInstantNumber.contains("9") || callInstantNumber.contains("0")) {
                    screen.setText(callInstantNumber);
                    getDialerNumber.setText(callInstantNumber);
                    makeInstantCall();
                } else {
                    speak(callInstantNumber + " is not a valid name in contact list");
                }
            }



        }

//        dialerSpeechRecogizer.startListening(dialerSpeechIntent);
//        dialerIsListening = true;
        dialerDelayHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                dialerSpeechRecogizer.startListening(dialerSpeechIntent);
                dialerIsListening = true;
            }
        }, 5000);
    }

    private void makeCall() {

        // Make call using contact i.e. when the user picks from the contact list
        if (!getDialerNumber.getText().toString().isEmpty()){

            String number = getDialerNumber.getText().toString();
            String encodeHash = Uri.encode(number);

            if (number.trim().length() > 0 && number.contains("#")) {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                } else {

//                    String dial = "tel:" + number+ encodeHash;
//                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                    speak("USSD running ");
                    startActivityForResult(new Intent("android.intent.action.CALL",
                            Uri.parse("tel:" + encodeHash)), 1);
                }


            } else if (number.trim().length() > 0 && !number.contains("#")) {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                } else {
                    speak("calling " + screen.getText().toString());
                    String dial = "tel:" + number;
                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                }


            } else {
                speak("please enter a phone number!");
                Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
            }
        }

        // Make calls using the default entered phone i.e user entered number manually
        if(getDialerNumber.getText().toString().isEmpty()) {
            if (!screen.getText().toString().isEmpty()) {

                String number = screen.getText().toString();
                String encodeHash = Uri.encode(number);

                if (number.trim().length() > 0 && number.contains("#")) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                    } else {

//                    String dial = "tel:" + number+ encodeHash;
//                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                        speak("USSD running");
                        startActivityForResult(new Intent("android.intent.action.CALL",
                                Uri.parse("tel:" + encodeHash)), 1);
                    }


                } else if (number.trim().length() > 0 && !number.contains("#")) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                    } else {
                        speak("calling " + number);
                        String dial = "tel:" + number;
                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                    }


                } else {
                    speak("please enter a phone number!");
                    Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
                }
            }
        }

        else {
            Toast.makeText(getApplicationContext(), "Enter Phone Number", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeInstantCall() {

        // Make instant calls using the ==> Call *ContactName* <== command
        if(!screen.getText().toString().isEmpty() && !getDialerNumber.getText().toString().isEmpty()) {

            String number = getDialerNumber.getText().toString();
            String encodeHash = Uri.encode(number);

            if (number.trim().length() > 0) {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                } else {
                    speak("calling " + screen.getText().toString());
                    String dial = "tel:" + number;
                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                }

            } else {
                Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void checkAudioPermission(){
        if (ContextCompat.checkSelfPermission(Dialer.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Dialer.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else {

            // DO NOTHING FOR NOW
        }
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(Dialer.this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Dialer.this,
                    new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
        }
    }

    private void initializeTextToSpeech() {
        dialerTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(dialerTTS.getEngines().size() == 0){
                    Toast.makeText(Dialer.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    dialerTTS.setLanguage(Locale.ENGLISH);
                    dialerTTS.setSpeechRate((float)0.9);
                    speak("Whom should I call?");
                    // speak("I am 08133125544");
                }
            }
        });
    }



    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            dialerTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            dialerTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialerDelayHandler.removeCallbacksAndMessages(null);
        dialerSpeechRecogizer.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dialerDelayHandler.removeCallbacksAndMessages(null);
        dialerSpeechRecogizer.stopListening();
    }
}