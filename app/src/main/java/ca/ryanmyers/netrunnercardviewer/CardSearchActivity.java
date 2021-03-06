package ca.ryanmyers.netrunnercardviewer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.mopub.common.MoPub;
import io.fabric.sdk.android.Fabric;
import com.mopub.mobileads.MoPubView;
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
    private static final String CARD_IMAGE_TAG = "CardImage";
    private static final String MOPUB_BANNER_AD_UNIT_ID = "98a9ce7f6a9b47f095747d9b85c00f6f";
    private MoPubView moPubView;

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mCurrentAnimator;


    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics(), new MoPub());
        //Show the layout.
        setContentView(R.layout.activity_card_search);
        moPubView = (MoPubView) findViewById(R.id.mopub_banner_ad);
        moPubView.setAdUnitId(MOPUB_BANNER_AD_UNIT_ID);
        moPubView.loadAd();
    }

    /**
     * Currently called by clicking search. It will start the download of a cards JSON
     * if there is a connection available, and add that card to the view.
     */
    protected void addCardsToView() {
        Log.d(TAG, "Clicked Search");
        downloadCardList();
    }

    protected void downloadCardList() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new downloadCardList().execute("cards", "");
        } else {
            //TODO: Properly handle scenarios where no connection is available.
            Log.d(TAG, "No Connection!");
        }
    }

    protected String[] getCardCodesFromJSON(JSONArray cardList) throws JSONException {
        String[] cardCodes = new String[cardList.length()];

        for (int i = 0; i <= cardList.length() - 1; i++) {
            JSONObject cardObject = cardList.getJSONObject(i);
            cardCodes[i] = cardObject.getString("code");
        }

        return cardCodes;
    }

    protected JSONArray readNetrunnerDB(String api, String code) throws IOException, JSONException {
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
            connection.setConnectTimeout(150000); //^
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

    /**
     * Downloads all cards, and adds them to the view
     */
    private class downloadCardList extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... params) {
            String apiCode = params[0];
            String cardCode = params[1];

            try {
                JSONArray cardList = readNetrunnerDB(apiCode, cardCode);

                CardDatabaseContract cardDb = new CardDatabaseContract(getApplicationContext());
                cardDb.addCards(cardList);

                return cardList;
            } catch (IOException e) {
                //TODO: Handle the error by telling the end user that we cannot download cards list right now.
                Log.d(TAG, "Unable to download cards");
                e.printStackTrace();
            } catch (JSONException e) {
                //TODO: Handle the error by telling the end user that we cannot parse cards list right now. (Problem with NRDB)
                Log.d(TAG, "Unable to parse cards");
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONArray cardList) {
            try {
                for (String cardCode : getCardCodesFromJSON(cardList)) {
                    new AddCardToView().execute(cardCode);
                }
            } catch (JSONException e) {
                //TODO: Handle the error by telling the end user that we cannot parse cards list right now. (Problem with NRDB)
                Log.d(TAG, "Unable to parse cards");
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a single card (passed with cardCode) to the view
     */
    private class AddCardToView extends AsyncTask<String, Void, TableRow> {
        @Override
        protected TableRow doInBackground(String... cardCodes) {
            return getCardRow(cardCodes[0]);
        }

        protected TableRow getCardRow(final String cardCode) {
            Context context = getApplicationContext();
            CardDatabaseContract cardDb = new CardDatabaseContract(context);

            TableRow.LayoutParams rowLayout =
                    new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,  TableRow.LayoutParams.WRAP_CONTENT);
            TableLayout.LayoutParams tableLayout =
                    new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT);

            TableRow tr = new TableRow(context);
            tr.setLayoutParams(tableLayout);

            /**
             * Add the small card image to the row
             */
            //Download the card if it hasn't already been done.
            cardDb.downloadCardImage(cardCode);
            ImageView cardImageView = new ImageView(context);
            cardImageView.setImageBitmap(cardDb.getSmallCardImage(cardCode));
            cardImageView.setLayoutParams(rowLayout);
            cardImageView.setAdjustViewBounds(true);
            cardImageView.setMaxHeight(100);
            cardImageView.setTag(CARD_IMAGE_TAG);
            tr.addView(cardImageView);

            /**
             * Add Card Title
             */
            TextView cardName = new TextView(context);
            cardName.setText(cardDb.getCardTitle(cardCode));
            cardName.setTextColor(Color.BLACK);
            cardName.setLayoutParams(rowLayout);
            tr.addView(cardName);

            /**
             * Set up the zoom for the card when clicked.
             */
            tr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageView cardImage = (ImageView) view.findViewWithTag(CARD_IMAGE_TAG);
                    zoomImageFromThumb(cardImage, cardCode);
                }
            });

            return tr;
        }

        @Override
        protected void onPostExecute(TableRow tr) {
            //Add row to table.
            //This is done on post execute like this because the async thread can't update the UI
            //that was created on another thread (in this case the UI thread).
            TableLayout tblCards = (TableLayout) findViewById(R.id.tblCards);
            tblCards.addView(tr);
        }
    }

    private void zoomImageFromThumb(final ImageView thumbView, String cardCode) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) findViewById(
                R.id.expanded_image);
        CardDatabaseContract cardDb = new CardDatabaseContract(getApplicationContext());
        expandedImageView.setImageBitmap(cardDb.getFullCardImage(cardCode));

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.main_view)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
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

    @Override
    protected void onDestroy() {
        moPubView.destroy();
        super.onDestroy();
    }
}
