package ca.ryanmyers.netrunnercardviewer;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.JsonReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
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
        Log.d(TAG, "Clicked Search");
    }

    protected JSONObject readNetrunnerDB(String api, String code) {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        return new JSONObject();
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
