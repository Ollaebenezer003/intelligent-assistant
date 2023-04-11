package com.example.olla.smartassistant;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class Contacts extends AppCompatActivity {

    // ArrayList for storing the list of contact names
    ArrayList<String> nameListView = new ArrayList<String>();

    // ArrayList for storing the list of contact numbers
    ArrayList<String> phoneListView = new ArrayList<String>();

    // List view instance
    ListView contactListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // set a custom title for the Title/Action Bar...
        getSupportActionBar().setTitle("Select from contact list");

        // list view instance + id
        contactListView = (ListView) findViewById(R.id.listview);

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");

        // move cursor to the first name on the list
        cursor.moveToFirst();

        // iterate through the contact list and obtain contact details
        while (cursor.moveToNext()){

            // get contact names
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            // get contact numbers
            String phoneNum = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            // add the contact details to the list
            nameListView.add(contactName + "\n" + phoneNum);
        }


        ArrayAdapter<String> adapterName = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, nameListView);

        contactListView.setAdapter(adapterName);

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // get an item when clicked at a particular position
                String tempListView = (String) parent.getItemAtPosition(position);

                // set clicked item to an intent that is returned to the Message Page
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", tempListView);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();

            }
        });

    }
}
