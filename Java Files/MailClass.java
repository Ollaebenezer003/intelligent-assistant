package com.example.olla.smartassistant;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailClass extends AppCompatActivity {

    // Flag for displaying logs
    private static final String TAG = MailClass.class.getName();

    // request for RECORD AUDIO permission
    public static final int REQUEST_AUDIO = 1;

    // Speech Recognizer for callbacks
    public SpeechRecognizer mailSpeechRecognizer;

    // Intent for speech recognition
    Intent mailSpeechIntent;

    //text to speech object for responses
    private TextToSpeech mailTTS;

    // Boolean variable to keep track of Speech State
    boolean mIsListening;

    // Input manager variable for hiding the keypad
    InputMethodManager mailKeypadMgr;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler mailDelayHandler = new Handler();

    // variable instaces for the mail properties. Some instances required to send the mail...
    // ======================================================================================
    Session session = null;
    ProgressDialog pDialog = null;
    Context context = null;

    // Edit text objects to get input values from entries on the for...
    // ================================================================
    private EditText mEditTextTo;
    private EditText mEditTextSubject;
    private EditText mEditTextMessage;

    // String variables to Obtain values....
    String recepient;
    String subject;
    String textMessage;

    boolean flag1;
    boolean flag2;
    boolean flag3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_class);
        getSupportActionBar().setTitle("Omail");

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.ic_sendmail);

        context = this;

        flag1 = false;
        flag2 = false;
        flag3 = false;

        // Initialize the receiver's address and set the focus of keypad to text input...
        mEditTextTo = findViewById(R.id.edit_text_to);
        mEditTextTo.requestFocus();

        mEditTextSubject = findViewById(R.id.edit_text_subject);
        mEditTextMessage = findViewById(R.id.edit_text_message);

        Button buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMail();
            }
        });
    }

    @Override
    protected void onStart() {

        // stop the running service when SendMessage is called
        stopService(new Intent(this, MyService.class));
        Log.i("Feedback", "Service has been stopped");

        mailSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MailClass.this);
        SpeechListener mailRecognitionListener = new SpeechListener();
        mailSpeechRecognizer.setRecognitionListener(mailRecognitionListener);
        mailSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        mailSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, MailClass.this.getPackageName());

        mailSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        mailSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        mailSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        mailDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mailSpeechRecognizer.startListening(mailSpeechIntent);
                mIsListening = true;
            }
        }, 4000);

        super.onStart();
    }

    class SpeechListener extends MailClass implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d(TAG, "ready for speech");
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
                Toast.makeText(getApplicationContext(), "Client Error", Toast.LENGTH_SHORT).show();
            } else {
                if (mIsListening){
                    mailSpeechRecognizer.stopListening();
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

        if (mEditTextTo.isEnabled() && !flag1) {

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


            if (command.contains("at gmail.com")) {
                command = command.replaceAll("at gmail.com", "@ gmail.com");
                command = command.replaceAll("\\s+", "").trim();
                String receiver = command.toLowerCase();
                mEditTextTo.setText(receiver);
                mEditTextTo.setEnabled(false);
                mEditTextSubject.setEnabled(true);
                speak("what is the subject?");

                flag1 = true;
            } else if (command.contains("at yahoo.com")) {
                command = command.replaceAll("at yahoo.com", "@ yahoo.com");
                command = command.replaceAll("\\s+", "").trim();
                String receiver = command.toLowerCase();
                mEditTextTo.setText(receiver);
                mEditTextSubject.requestFocus();
                speak("what is the subject?");

                flag1 = true;
            } else if (!command.contains("at gmail.com") && !command.contains("at yahoo.com") && mEditTextTo.requestFocus()) {
                speak("Incorrect email address");
                mEditTextTo.requestFocus();
            }

            Toast.makeText(getApplicationContext(), command, Toast.LENGTH_SHORT).show();
        }
        // enter a subject for the Mail if an address has been entered...
        if (mEditTextSubject.isEnabled() && !flag2){

            if (!mEditTextTo.getText().toString().isEmpty() && !command.contains("@")) {

                mEditTextSubject.setText(command);
                flag2 = true;
                mEditTextSubject.setEnabled(false);
                mEditTextMessage.setEnabled(true);
                speak("compose the mail to be sent");
            } else {
                mEditTextMessage.setText("");
                mEditTextSubject.requestFocus();
                flag2 = false;
            }
        }
        // compose the Mail message if an address and a subject has been provided
        if (mEditTextMessage.isEnabled() && !flag3){
            String subjectValue = mEditTextSubject.getText().toString();
            if (!command.contains("@") && flag2 && !command.contains(subjectValue)) {
                mEditTextMessage.append(command + " ");
            } else {
                mEditTextMessage.append("");
                mEditTextMessage.requestFocus();
            }
//            if (!command.contains("correct subject") && !command.contains("@")){
//                mEditTextMessage.append(command + " ");
//            } else {
//
//            }
        }
        if (command.equals("send mail") || command.equals("send email") || command.equals("send")){
            if (mEditTextMessage.isEnabled()){
                mEditTextMessage.append("");
            }
            String valAddress = mEditTextTo.getText().toString();
            String valSubject = mEditTextSubject.getText().toString();
            String valMessage = mEditTextMessage.getText().toString();
            if (!valAddress.isEmpty() && !valMessage.isEmpty()){
                sendMail();
            } else {
                speak("No mail address and/or message");
            }

        }
        if (command.equals("cancel mail") || command.equals("cancel email")){
            this.onBackPressed();
        }
        mailDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mailSpeechRecognizer.startListening(mailSpeechIntent);
                mIsListening = true;
            }
        }, 3000);
    }

    private void sendMail() {
        recepient = mEditTextTo.getText().toString();
        subject = mEditTextSubject.getText().toString();
        textMessage = mEditTextMessage.getText().toString();

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("ollaebenezer@gmail.com", "Olla6243");
            }
            });

        pDialog = ProgressDialog.show(context, "", "Sending Mail...", true);

        RetreiveFeedtask task = new RetreiveFeedtask();
        task.execute();
    }

    private void initializeTextToSpeech() {
        mailTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(mailTTS.getEngines().size() == 0){
                    Toast.makeText(MailClass.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    mailTTS.setLanguage(Locale.ENGLISH);
                    mailTTS.setSpeechRate((float)0.8);
                    speak("Who should I mail?");
                    // speak("I am 08133125544");
                }
            }
        });
    }



    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            mailTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            mailTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    class RetreiveFeedtask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try{
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("ollaebenezer@gmail.com"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recepient));
                message.setSubject(subject);
                message.setContent(textMessage, "text/html; charset=utf_8");

                Transport.send(message);
            } catch (MessagingException e){
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            pDialog.dismiss();
            mEditTextTo.setText("");
            mEditTextSubject.setText("");
            mEditTextMessage.setText("");
            Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();

//            super.onPostExecute(s);
        }
    }
}
