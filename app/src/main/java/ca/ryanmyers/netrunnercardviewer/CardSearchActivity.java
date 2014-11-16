package ca.ryanmyers.netrunnercardviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CardSearchActivity extends ActionBarActivity {
    private static final String TAG = "NetrunnerCardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Show the layout.
        setContentView(R.layout.activity_card_search);
    }

    /**
     * Currently called by clicking search. It will start the download of a cards JSON
     * if there is a connection available, and add that card to the view.
     */
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

    /**
     * Downloads a single card's JSON async.
     */
    private class DownloadCardTask extends AsyncTask<String[], Void, Void> {
        @Override
        protected Void doInBackground(String[]... params) {
            try {
                for (String[] param : params) {
                    //param[0] is the API to use
                    //param[1] is the code to use
                    updateTableView(readNetrunnerDB(param[0], param[1]));
                }
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: " + e.getMessage());
            } catch (IOException ioEx) {
                Log.d(TAG, "IOException: " + ioEx.getMessage());
            }

            return null;
        }

        protected void updateTableView(JSONArray card) {
            Log.d(TAG, "Card Finished: " + card.toString());
            Context context = getApplicationContext();

            //Declare this as final so that the thread at the end can access it.
            final TableLayout tblCards = (TableLayout) findViewById(R.id.tblCards);

            TableRow.LayoutParams rowLayout =
                    new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,  TableRow.LayoutParams.WRAP_CONTENT);
            TableLayout.LayoutParams tableLayout =
                    new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT);

            for (int i = 0; i <= card.length() - 1; i++) {
                //Declare this as final so that the thread at the end can access it.
                final TableRow tr = new TableRow(context);
                tr.setLayoutParams(tableLayout);
                String cardTitle = null;
                String cardImageUrl = null;

                try {
                    JSONObject cardObject = card.getJSONObject(i);
                    cardTitle = cardObject.get("title").toString();
                    cardImageUrl = getResources().getString(R.string.netrunner_db_url) + cardObject.get("imagesrc").toString();
                    Log.d(TAG, cardImageUrl);
                } catch (JSONException e) {
                    Log.d(TAG, "Card JSONException: " + e.getMessage());
                }

                //Add Card Image. This will download the image, so it can only be done on the async thread.
                ImageView cardImage = new ImageView(context);
                try {
                    Bitmap bmp = BitmapFactory.decodeStream((InputStream) new URL(cardImageUrl).getContent());
                    cardImage.setImageBitmap(bmp);
                } catch (MalformedURLException e) {
                    Log.d(TAG, e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                cardImage.setLayoutParams(rowLayout);
                tr.addView(cardImage);

                //Add Card Title
                TextView cardName = new TextView(context);
                cardName.setText(cardTitle);
                cardName.setTextColor(Color.BLACK);
                cardName.setLayoutParams(rowLayout);
                tr.addView(cardName);

                //Add row to table.
                //This is done with a thread like this because the async thread can't update the UI
                //that was created on another thread (in this case the UI thread).
                new Thread() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tblCards.addView(tr);
                                }
                            });
                        }
                    }
                }.start();
            }
        }

        private JSONArray readNetrunnerDB(String api, String code) throws IOException, JSONException {
            //URL for Netrunner DB for the card.
            String apiUrl = getResources().getString(R.string.netrunner_db_url) + "api/" + api + "/" + code;

            //Set up the major variables so they can be properly disposed of later.
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

                //TODO: Remove this after debugging is done, or handle 404's.
                int response = connection.getResponseCode();
                Log.d(TAG, "The response is: " + response);

                //All of the below to get the content read to a string with no length restrictions.
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
                //Make sure all connections are properly closed, regardless of the outcome of the request.
                if (connection != null) connection.disconnect();
                if (content != null)    content.close();
                if (reader != null)     reader.close();
            }

            //In the event that this was unable to get a result, return a blank array.
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
