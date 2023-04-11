package com.example.olla.smartassistant;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class Help extends AppCompatActivity {

    ListView listv;
    String[] fruitname = {"Alarm", "Call", "Calculator", "Contacts", "Gallery", "Mail", "Inbox", "Message", "Reminder", "Facebook"};
    String[] desc = {"'Set an alarm for 10am'", "'Call Adama'", "'What is 40 divide by 2'", "'Show my contact list'", "'Bring up my images from yesterday'", "'I want to send a mail to James'", "'Show messages in my inbox'", "'Tell Jane I'll be there in 5 minutes'", "'When is my wedding anniversary'", "'Open facebook app'"};
    Integer[] imgId = {R.drawable.clock, R.drawable.call, R.drawable.calculator, R.drawable.contacts, R.drawable.gallery, R.drawable.mail, R.drawable.message, R.drawable.message2, R.drawable.calendar, R.drawable.facebook};

    Help TAG = Help.this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        getSupportActionBar().setTitle("Some things you can ask me");

        listv = findViewById(R.id.listView1);

        HelpClassListview customListView = new HelpClassListview(this, fruitname, desc, imgId);
        listv.setAdapter(customListView);

        listv.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
                String message = (String) parent.getItemAtPosition(position);
                message.toLowerCase();

//                Toast.makeText(TAG, message, Toast.LENGTH_SHORT).show();

                if (message.contains("Alarm")){
                    Intent openAlarm = new Intent(TAG, Alarm.class);
                    startActivity(openAlarm);
                }
                if (message.contains("Call")){
                    Intent openCall = new Intent(TAG, Dialer.class);
                    startActivity(openCall);
                }
                if (message.contains("Calculator")){
//                    Intent openCalculator = new Intent(Help.this, Calculator.class);
//                    startActivity(openCalculator);
                    Toast.makeText(TAG, "Calculator class still in pogress...", Toast.LENGTH_SHORT).show();
                }
                if (message.contains("Contacts")){
                    Intent openContacts = new Intent(TAG, Contacts.class);
                    startActivity(openContacts);
                }
                if (message.contains("Gallery")){
//                    Intent openGallery = new Intent(TAG, Pictures.class);
//                    startActivity(openGallery);
                    Toast.makeText(TAG, "Gallery still in progress...", Toast.LENGTH_SHORT).show();
                }
                if (message.contains("Mail")){
                    Intent openMail = new Intent(TAG, MailClass.class);
                    startActivity(openMail);
//                    Toast.makeText(TAG, "Email not ready yet...", Toast.LENGTH_SHORT).show();
                }
                if (message.contains("Inbox")){
                    Intent openInbox = new Intent(TAG, MessageInbox.class);
                    startActivity(openInbox);
                }
                if (message.contains("Message")){
                    Intent openSendMessage = new Intent(TAG, SendMessage.class);
                    startActivity(openSendMessage);
                }
                if (message.contains("Reminders")){
//                    Intent openReminders = new Intent(TAG, CalendarReminder.class);
//                    startActivity(openReminders);
                    Toast.makeText(TAG, "Calendar reminders still in process...", Toast.LENGTH_SHORT).show();
                }
                if (message.contains("Facebook")){
                    Intent launchFacebook = getPackageManager().getLaunchIntentForPackage("com.facebook.katana");
                    startActivity(launchFacebook);
                }
            }
        });
    }
}
