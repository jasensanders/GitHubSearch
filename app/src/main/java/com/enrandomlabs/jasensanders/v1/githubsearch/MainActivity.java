package com.enrandomlabs.jasensanders.v1.githubsearch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_EVENT_REQUEST = "com.enrandomlabs.jasensanders.v1.githubsearch.SERVICE_EVENT_REQUEST";
    public static final String SERVICE_EXTRA_GITHUB_DATA = "com.enrandomlabs.jasensanders.v1.githubsearch.SERVICE_EVENT_GITHUB_DATA";
    private static final String CACHE = "com.enrandomlabs.jasensanders.v1.githubsearch.CACHE";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private BroadcastReceiver mMessageReceiver;

    private RecyclerView itemList;
    private ListItemAdapter listItemAdapter;
    private EditText queryText;
    private Button submit;
    private ArrayList<String> cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ToDO
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queryText = findViewById(R.id.query);
        submit = findViewById(R.id.submit);
        itemList = findViewById(R.id.content_list);
        listItemAdapter = new ListItemAdapter(this);
        itemList.setAdapter(listItemAdapter);
        LinearLayoutManager list = new LinearLayoutManager(this);
        list.setOrientation(LinearLayoutManager.VERTICAL);
        itemList.setLayoutManager(list);
        itemList.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, R.drawable.line_divider));

        if(savedInstanceState != null){
            cache = savedInstanceState.getStringArrayList(CACHE);
            if(cache != null) {
                ArrayList<String[]> expand = expandString(cache);
                //Push Data to List.
                listItemAdapter.swapData(expand);
            }
        }

    }

    private class ServiceMessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringArrayListExtra(SERVICE_EXTRA_GITHUB_DATA) != null ) {

                //Report to user with results
                ArrayList<String> received = intent.getStringArrayListExtra(SERVICE_EXTRA_GITHUB_DATA);
                cache = received;

                ArrayList<String[]> expand = expandString(received);

                //Push Data to List.
                listItemAdapter.swapData(expand);

            }

        }
    }

    private void registerMessageReceiver(){

        //If there isn't a messageReceiver yet make one.
        if(mMessageReceiver == null) {
            mMessageReceiver = new ServiceMessageReceiver();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_EVENT_REQUEST);
        filter.addAction(FetchRepositoriesService.SERVER_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

    }
    private void unRegisterMessageReceiver(){

        if(mMessageReceiver != null){
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            }catch(Exception e){
                Log.e(LOG_TAG, "messageReciever unregisterd already", e);
            }
        }
    }

    //Checks for internet connectivity
    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    private ArrayList<String[]> expandString(ArrayList<String> repos){
        ArrayList<String[]> expanded = new ArrayList<>();

        for(String Repo: repos){
            String[] item = Repo.split(",");
            expanded.add(item);
        }
        return expanded;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(CACHE, cache);

    }

    public void queryGitHub(View view){

        String input = queryText.getText().toString();

        FetchRepositoriesService.startActionQuery(this, input);

    }

    @Override
    public void onPause() {
        unRegisterMessageReceiver();
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
        super.onResume();
        registerMessageReceiver();
    }
}
