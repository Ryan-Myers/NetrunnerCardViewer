package ca.ryanmyers.netrunnercardviewer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Defines a set up methods for accessing and updating a SQLlite DB for the cards
 */
public final class CardDatabaseContract {
    public static final String TAG = "NetrunnerDBContract";
    private Context activity_context;

    public CardDatabaseContract(Context context) {
        this.activity_context = context;
    }

    public static abstract class CardEntry implements BaseColumns {
        public static final String TABLE_NAME = "NRDB_Cards";
        public static final String COLUMN_NAME_LAST_MODIFIED = "[last-modified]";
        public static final String SIMPLE_COLUMN_NAME_LAST_MODIFIED = "last-modified";
        public static final String COLUMN_NAME_CODE = "code";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_TYPE_CODE = "type_code";
        public static final String COLUMN_NAME_SUBTYPE = "subtype";
        public static final String COLUMN_NAME_SUBTYPE_CODE = "subtype_code";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_BASELINK = "baselink";
        public static final String COLUMN_NAME_FACTION = "faction";
        public static final String COLUMN_NAME_FACTION_CODE = "faction_code";
        public static final String COLUMN_NAME_FACTION_LETTER = "faction_letter";
        public static final String COLUMN_NAME_FLAVOR = "flavor";
        public static final String COLUMN_NAME_ILLUSTRATOR = "illustrator";
        public static final String COLUMN_NAME_INFLUENCELIMIT = "influencelimit";
        public static final String COLUMN_NAME_MINIMUMDECKSIZE = "minimumdecksize";
        public static final String COLUMN_NAME_NUMBER = "number";
        public static final String COLUMN_NAME_QUANTITY = "quantity";
        public static final String COLUMN_NAME_SETNAME = "setname";
        public static final String COLUMN_NAME_SET_CODE = "set_code";
        public static final String COLUMN_NAME_SIDE = "side";
        public static final String COLUMN_NAME_SIDE_CODE = "side_code";
        public static final String COLUMN_NAME_UNIQUENESS = "uniqueness";
        public static final String COLUMN_NAME_LIMITED = "limited";
        public static final String COLUMN_NAME_CYCLENUMBER = "cyclenumber";
        public static final String COLUMN_NAME_ANCURLINK = "ancurLink";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_IMAGESRC = "imagesrc";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BOOLEAN_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + CardEntry.TABLE_NAME + " (" +
                    CardEntry._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    CardEntry.COLUMN_NAME_LAST_MODIFIED + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_TYPE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_TYPE_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SUBTYPE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SUBTYPE_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_TEXT + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_BASELINK + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_FACTION + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_FACTION_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_FACTION_LETTER + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_FLAVOR + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_ILLUSTRATOR + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_INFLUENCELIMIT + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_MINIMUMDECKSIZE + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_NUMBER + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_QUANTITY + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SETNAME + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SET_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SIDE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_SIDE_CODE + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_UNIQUENESS + BOOLEAN_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_LIMITED + BOOLEAN_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_CYCLENUMBER + INTEGER_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_ANCURLINK + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                    CardEntry.COLUMN_NAME_IMAGESRC + TEXT_TYPE +
                    ")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + CardEntry.TABLE_NAME;

    public class CardDatabaseDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "CardDatabase.db";

        public CardDatabaseDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    public void addCard(JSONArray cards) {
        CardDatabaseDbHelper mDbHelper = new CardDatabaseDbHelper(activity_context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (int i = 0; i <= cards.length() - 1; i++) {
            ContentValues cardValues = new ContentValues();
            String cardImageUrl = null;
            String cardImageFilePath = null;

            //Attempt to read the card JSON, and prepare it for inserting into the DB.
            try {
                JSONObject cardObject = cards.getJSONObject(i);
                cardValues.put(CardEntry.COLUMN_NAME_LAST_MODIFIED, cardObject.get(CardEntry.SIMPLE_COLUMN_NAME_LAST_MODIFIED).toString());
                cardValues.put(CardEntry.COLUMN_NAME_CODE, cardObject.get(CardEntry.COLUMN_NAME_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_TITLE, cardObject.get(CardEntry.COLUMN_NAME_TITLE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_TYPE, cardObject.get(CardEntry.COLUMN_NAME_TYPE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_TYPE_CODE, cardObject.get(CardEntry.COLUMN_NAME_TYPE_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SUBTYPE, cardObject.get(CardEntry.COLUMN_NAME_SUBTYPE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SUBTYPE_CODE, cardObject.get(CardEntry.COLUMN_NAME_SUBTYPE_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_TEXT, cardObject.get(CardEntry.COLUMN_NAME_TEXT).toString());
                cardValues.put(CardEntry.COLUMN_NAME_BASELINK, cardObject.get(CardEntry.COLUMN_NAME_BASELINK).toString());
                cardValues.put(CardEntry.COLUMN_NAME_FACTION, cardObject.get(CardEntry.COLUMN_NAME_FACTION).toString());
                cardValues.put(CardEntry.COLUMN_NAME_FACTION_CODE, cardObject.get(CardEntry.COLUMN_NAME_FACTION_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_FACTION_LETTER, cardObject.get(CardEntry.COLUMN_NAME_FACTION_LETTER).toString());
                cardValues.put(CardEntry.COLUMN_NAME_FLAVOR, cardObject.get(CardEntry.COLUMN_NAME_FLAVOR).toString());
                cardValues.put(CardEntry.COLUMN_NAME_ILLUSTRATOR, cardObject.get(CardEntry.COLUMN_NAME_ILLUSTRATOR).toString());
                cardValues.put(CardEntry.COLUMN_NAME_INFLUENCELIMIT, cardObject.get(CardEntry.COLUMN_NAME_INFLUENCELIMIT).toString());
                cardValues.put(CardEntry.COLUMN_NAME_MINIMUMDECKSIZE, cardObject.get(CardEntry.COLUMN_NAME_MINIMUMDECKSIZE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_NUMBER, cardObject.get(CardEntry.COLUMN_NAME_NUMBER).toString());
                cardValues.put(CardEntry.COLUMN_NAME_QUANTITY, cardObject.get(CardEntry.COLUMN_NAME_QUANTITY).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SETNAME, cardObject.get(CardEntry.COLUMN_NAME_SETNAME).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SET_CODE, cardObject.get(CardEntry.COLUMN_NAME_SET_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SIDE, cardObject.get(CardEntry.COLUMN_NAME_SIDE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_SIDE_CODE, cardObject.get(CardEntry.COLUMN_NAME_SIDE_CODE).toString());
                cardValues.put(CardEntry.COLUMN_NAME_UNIQUENESS, cardObject.get(CardEntry.COLUMN_NAME_UNIQUENESS).toString());
                cardValues.put(CardEntry.COLUMN_NAME_LIMITED, cardObject.get(CardEntry.COLUMN_NAME_LIMITED).toString());
                cardValues.put(CardEntry.COLUMN_NAME_CYCLENUMBER, cardObject.get(CardEntry.COLUMN_NAME_CYCLENUMBER).toString());
                cardValues.put(CardEntry.COLUMN_NAME_ANCURLINK, cardObject.get(CardEntry.COLUMN_NAME_ANCURLINK).toString());
                cardValues.put(CardEntry.COLUMN_NAME_URL, cardObject.get(CardEntry.COLUMN_NAME_URL).toString());
                cardValues.put(CardEntry.COLUMN_NAME_IMAGESRC, cardObject.get(CardEntry.COLUMN_NAME_IMAGESRC).toString());

                //Get the fully qualified URL for the card image.
                cardImageUrl = this.activity_context.getResources().getString(R.string.netrunner_db_url) +
                        cardObject.get(CardEntry.COLUMN_NAME_IMAGESRC).toString();
                //Get the full path and filename of the card image using the card code as the filename.
                cardImageFilePath = this.activity_context.getResources().getString(R.string.card_image_file_path) +
                        cardObject.get(CardEntry.COLUMN_NAME_CODE).toString() + ".png";
            } catch (JSONException e) {
                Log.d(TAG, "Card JSONException: " + e.getMessage());
            }

            //Attempt to insert the card data into the database.
            try {
                long newRowId;
                //NULL in the second argument ensures that if there is no data in cardValues, it doesn't insert a row.
                newRowId = db.insert(CardEntry.TABLE_NAME, "null", cardValues);
                Log.d(TAG, "Inserted a new row with Id: " + newRowId);
            } catch (SQLiteDatabaseLockedException e) {
                Log.d(TAG, "SQLiteDatabaseLockedException - " + e.getMessage());
            }

            //Add Card Image. This will download the image, so it can only be done on the async thread.
            FileOutputStream out = null;
            try {
                if (cardImageUrl != null && cardImageFilePath != null) {
                    Bitmap bmp = BitmapFactory.decodeStream((InputStream) new URL(cardImageUrl).getContent());
                    out = new FileOutputStream(cardImageFilePath);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                } else {
                    Log.d(TAG, "cardImageUrl or cardImageFilePath is null! CIU: " +
                            cardImageUrl + " - CIFP: " + cardImageFilePath);
                }
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        Log.d(TAG, "Failed to close file - " + ex.getMessage());
                    }
                }
            }
        }
    }
}
