package com.example.olla.smartassistant;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MessageInbox extends AppCompatActivity {

    private static MessageInbox inst;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;

    //List view  instance for accessing the View display
    ListView smsListView;

    // Array Adapter for update method
    ArrayAdapter arrayAdapter;

    // contact icon to be displayed for the messages.
    Integer imgId = R.drawable.contact_icon;

    // Array for storing and traversing the sender's ID
    String[] senderName;

    // An array for storing and traversing the message contents...
    String[] messageBody;

    // An array for storing and traversing the full message contents...
    String[] fullMessageBody;

    //text to speech object for responses
    private TextToSpeech inboxTTS;

    // Handler object to delay the Speech recognizer so that TTS completes
    // before StartListening method is called....
    Handler inboxDelayHandler = new Handler();

    public static MessageInbox instance(){return inst;}

    @Override
    protected void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_inbox);
        getSupportActionBar().setTitle("Messages from your inbox");

        //initialize the method for activating the TextToSpeech
        initializeTextToSpeech();

        smsListView = (ListView) findViewById(R.id.smsList);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED){
            showContacts();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS},
                    PERMISSION_REQUEST_READ_CONTACTS);
        }

        CustomListView customListView = new CustomListView(this, senderName, messageBody, imgId);
        smsListView.setAdapter(customListView);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_READ_CONTACTS){
            showContacts();
        } else{
            Toast.makeText(this, "until you grant the permission, we cannot display the name",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showContacts() {

        // Use URI instance to obtain access to the inbox.
        Uri inboxUri = Uri.parse("content://sms/inbox");

        // Content Resolver object for accessing phone's content
        ContentResolver contentResolver = getContentResolver();

        Cursor cursor = contentResolver.query(inboxUri, null, null, null, null);

        // Get the total count or number of messages in the Inbox....
        int count = cursor.getCount();

        // Setting and initializing the array sizes.
        // ========================================
        senderName = new String[count];
        messageBody = new String[count];
        fullMessageBody = new String[count];

        // An array counter to access each index of the array...
        int arrayCounter = 0;

        // Loop through the message list in the inbox of the mobile phone.
        // While looping we retreive the sender's ID, and the message sent.
        // If the message's length is longer than 50 characters, we cut it short so as to fit into the
        // Listview
        // ===========================================================================================
        while(cursor.moveToNext()){

            // String var for storing the sender's actual name if it exists
            String senderIDName = null;

            // cursor instances used to access the sender's ID number
            // and Message content respectively...
            String number = cursor.getString(cursor.getColumnIndexOrThrow("address")).toString();
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body")).toString();

            // check if the ID number is am actual number or bulk SMS ID
            // either from individuals or network operators
            if (number.contains("+")){
                senderIDName = getContactName(number);
            } else {
                senderIDName = number;
            }

            // Trim the message length if it is greater than 50 characters
            String newBody;
            if (body.toString().length() >= 50){
                newBody = body.toString().substring(0, Math.min(101, body.toString().length() -1)) +"...";
            } else {

            // Retain message length if it is less than 50 characters
                newBody = body;
            }

            // Initialize array elements with the Sender's ID and the Message Sent
            // This array values are then passed to the list view menu item
            senderName[arrayCounter] = senderIDName;
            messageBody[arrayCounter] = newBody;

            fullMessageBody[arrayCounter] = body;

            arrayCounter++;

        }

        // Close the cursor after looping through the list of messages...
        cursor.close();

    }

    // Method to receive the sender ID number and try to check if it exists
    // in the user's contact list
    private String getContactName(String senderNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(senderNumber));

        String projection[] = new String[]{ContactsContract.Data.DISPLAY_NAME};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        // check if number matches a name and return the name for the array
        String senderName = null;
        if (cursor.moveToFirst()){
            senderName = cursor.getString(0);
            cursor.close();
            return senderName;
        }
        // return initial number if the sender ID does not exist in contact list
        else {
            cursor.close();
            return senderNumber;
        }
    }


    // ==============================================================
    // A method to refresh the message inbox and check for new messages.

    private void refreshSmsInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
        arrayAdapter.clear();
        do {
            String str = "SMS From: " + smsInboxCursor.getString(indexAddress) +
                    "\n" + smsInboxCursor.getString(indexBody) + "\n";
            arrayAdapter.add(str);
        } while (smsInboxCursor.moveToNext());
    }

    public void updateList(final String smsMessage){
        arrayAdapter.insert(smsMessage, 0);
        arrayAdapter.notifyDataSetChanged();
    }

    private void initializeTextToSpeech() {
        inboxTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(inboxTTS.getEngines().size() == 0){
                    Toast.makeText(MessageInbox.this, "there are no TTS Engines installed on this device", Toast.LENGTH_SHORT).show();

                }else{
                    inboxTTS.setLanguage(Locale.ENGLISH);
                    inboxTTS.setSpeechRate((float)0.7);
                    speak("Last message was from" + senderName[0]);
                    inboxDelayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            inboxTTS.playSilentUtterance(2000, TextToSpeech.QUEUE_ADD, null);
                            speak("Message was " + fullMessageBody[0]);
                        }
                    }, 3000);
                    // speak("I am 08133125544");
                }
            }
        });
    }



    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            inboxTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            inboxTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    // Method to select an item clicked and open up a new View for reading full message...
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id){
//        try {
//            String[] smsMessages = smsMessageList.get(pos).split("\n");
//            String address = smsMessages[0];
//            String smsMessage = "";
//            for (int i = 1; i < smsMessages.length; ++i){
//                smsMessage += smsMessages[i];
//            }
//
//            String smsMessageStr = address + "\n";
//            smsMessageStr += smsMessage;
//            Toast.makeText(this, smsMessageStr, Toast.LENGTH_SHORT).show();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        inboxTTS.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inboxTTS.stop();
        inboxTTS.shutdown();
    }
}
