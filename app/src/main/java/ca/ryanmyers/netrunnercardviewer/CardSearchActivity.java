package ca.ryanmyers.netrunnercardviewer;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.JsonReader;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;


public class CardSearchActivity extends ActionBarActivity {
    private static final String TAG = "NetrunnerCardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_card_search);
    }

    protected void addCardsToView() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            //String[] test = new String[]{"card", "01001"};
            //new String[]{"card", "01001"}
            new DownloadCardTask().execute(new String[]{"card", "01001"});
        } else {
            Log.d(TAG, "No Connection!");
        }

        Log.d(TAG, "Clicked Search");
    }

    private class DownloadCardTask extends AsyncTask<String[], Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String[]... params) {
            try {
                for (String[] param : params) {
                    //param[0] is the API to use
                    //param[1] is the code to use
                    return readNetrunnerDB(param[0], param[1]);
                }
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: " + e.getMessage());
            } catch (IOException ioEx) {
                Log.d(TAG, "IOException: " + ioEx.getMessage());
            }

            return new JSONArray();
        }

        @Override
        protected void onPostExecute(JSONArray card) {
            Log.d(TAG, "Card Finished: " + card.toString());
            super.onPostExecute(card);
        }

        private JSONArray readNetrunnerDB(String api, String code) throws IOException, JSONException {
            String apiUrl = getResources().getString(R.string.netrunner_db_url) + api + "/" + code;
            HttpURLConnection connection  = null;
            InputStream content = null;
            BufferedReader reader = null;
            StringBuilder builder = new StringBuilder();

            try {
                URL url = new URL(apiUrl);

                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000); //milliseconds
                connection.setConnectTimeout(15000); //^
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                int response = connection.getResponseCode();
                Log.d(TAG, "The response is: " + response);

                content = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(content));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                return new JSONArray(builder.toString());

            } catch (MalformedURLException ex) {
                Log.d(TAG, "Malformed URL: " + apiUrl + " Message: " + ex.getMessage());
            } finally {
                if (connection != null) {
                    connection .disconnect();
                }
                if (content != null) {
                    content.close();
                }
                if (reader != null) {
                    reader.close();
                }
            }

            return new JSONArray();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_card_search, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_bar_search:
                addCardsToView();
                return true;
            case R.id.action_bar_settings :
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
