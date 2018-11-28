package com.enrandomlabs.jasensanders.v1.githubsearch;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *
 * helper methods.
 */
public class FetchRepositoriesService extends IntentService {

    private static final String LOG_TAG = FetchRepositoriesService.class.getSimpleName();

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_SEARCH = "com.enrandomlabs.jasensanders.v1.githubsearch.action.SEARCH";

    public static final String EXTRA_QUERY = "com.enrandomlabs.jasensanders.v1.githubsearch.extra.QUERY";

    // Error 000 is connection/server, Error 001 is parsing.
    public static final String SERVER_ERROR = "com.enrandomlabs.jasensanders.v1.githubsearch.extra.SERVER_ERROR";


    public FetchRepositoriesService() {
        super("FetchRepositoriesService");
    }

    /**
     * Starts this service to perform query action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */

    public static void startActionQuery(Context context, String param1) {
        Intent intent = new Intent(context, FetchRepositoriesService.class);
        intent.setAction(ACTION_SEARCH);
        intent.putExtra(EXTRA_QUERY, param1);
        context.startService(intent);
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEARCH.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_QUERY);
                handleActionQuery(param1);
            }
        }
    }

    /**
     * Handle action Query in the provided background thread with the provided
     * parameters.
     */
    private void handleActionQuery(String param1) {
        // Fetch Repositories
        String results = fetchRepositoriesJson(param1);

        // Parse Json
        ArrayList<String> data = parseJson(results);

        // Send data back to MainActivity
        sendDataBack(data);
    }

    private String fetchRepositoriesJson(String query){
        if(query == null)return null;
        // https://api.github.com/search/repositories?q=queryTerm&sort=stars&order=desc
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String upcJsonStr;

        try{
            final String BASE_URL = "https://api.github.com/search/repositories?";
            final String QUERY = "q";
            final String SORT = "sort";
            final String SORT_TYPE = "stars";
            final String ORDER = "order";
            final String SORT_ORDER = "desc";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY, query)
                    .appendQueryParameter(SORT, SORT_TYPE)
                    .appendQueryParameter(ORDER, SORT_ORDER)
                    .build();


            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //get result code
            int responseCode = urlConnection.getResponseCode();
            if(responseCode == 200) {

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                upcJsonStr = buffer.toString();
            }else{
                return SERVER_ERROR + ", " + String.valueOf(responseCode);
            }
        }catch (IOException e){
            Log.e(LOG_TAG, "IO Error UPC Request", e);

            return SERVER_ERROR + ", " + "000";
        }finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(reader != null){
                try{
                    reader.close();
                }catch (final IOException f){
                    Log.e(LOG_TAG, "Error Closing Stream UPC Request", f);

                }
            }
        }
        return upcJsonStr;
    }

    private ArrayList<String> parseJson(String results){

        // String Array Return Format: {0-RepoURL, 1-AvatarURL, 2-RepoName, 3-Description, 4-StarsNum}
        ArrayList<String> repos =new ArrayList<>();
        if(results != null) {
            if(results.startsWith(SERVER_ERROR) ){
                sendError(results.split(", "));
                return null;
            }

            final String ITEMS = "items";
            final String OWNER = "owner";
            final String REPONAME = "name";
            final String AVATARURL = "avatar_url";
            final String REPOURL = "html_url";
            final String DESCRIPTION = "description";
            final String STARSCOUNT = "stargazers_count";

            try {
                JSONObject upcJson = new JSONObject(results);
                JSONArray items = upcJson.getJSONArray(ITEMS);

                for(int i=0; i< items.length(); i++){
                    JSONObject item = items.getJSONObject(i);
                    JSONObject owner = item.getJSONObject(OWNER);
                    String avatarUrl = owner.getString(AVATARURL);
                    String repoURL = item.getString(REPOURL);
                    String repoName = item.getString(REPONAME);
                    String desc = item.getString(DESCRIPTION);
                    String starsCount = String.valueOf(item.getInt(STARSCOUNT));
                    String repo = repoURL +","+ avatarUrl + ","+ repoName+ "," + desc + "," + starsCount;
                    repos.add(repo);
                }

                return repos;


            } catch (JSONException j) {
                Log.e(LOG_TAG, "Error Parsing UPC JSON", j);
                sendError(new String[]{SERVER_ERROR, "001"});
                return null;
            }
        }else{
            sendError(new String[]{SERVER_ERROR, "001"});
            return null;
        }


    }

    private void sendDataBack(ArrayList<String> data){
        Intent messageIntent = new Intent(MainActivity.SERVICE_EVENT_REQUEST);
        messageIntent.putExtra(MainActivity.SERVICE_EXTRA_GITHUB_DATA, data);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }

    private void sendError(String[] errorMess){
        Intent messageIntent = new Intent(SERVER_ERROR);
        messageIntent.putExtra(SERVER_ERROR, errorMess);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }


}
