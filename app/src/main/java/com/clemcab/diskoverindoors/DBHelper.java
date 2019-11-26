package com.clemcab.diskoverindoors;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    // https://stackoverflow.com/a/37655109
    private static final String TAG = DBHelper.class.getSimpleName();
    private static final String DATABASE_PATH = "/data/data/com.clemcab.diskoverindoors/";
    private static final String DATABASE_NAME = "navdb.sqlite";
    private static final int DATABASE_VERSION = 1;
    private Context context;
    private SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        this.context = context;

        try {
            if (this.checkDB()) {
                Log.e(TAG,"SQLite DB already exists");
            } else {
                this.copyDBFromAsset();
            }
            this.openDB();
            Log.e(TAG,"Opened SQLite DB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int a, int b) {
        Log.d(TAG, "SQLite DB version changed from "
                +Integer.toString(a)+" to "+Integer.toString(b));
        File file = new File(DATABASE_PATH+DATABASE_NAME);
        try {
            if(file.exists()) {
                file.delete();
                Log.d(TAG, "SQLite DB deleted");
            }
            this.copyDBFromAsset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openDB() throws SQLException {
        String path = DATABASE_PATH+DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    public boolean codeExists(String code) {
        final String table = "QRTag";
        final String[] columns = {"url"};
        final String select = "url=?";
        String[] selectArgs = {code};
        String limit = "1";
        Cursor cursor = db.query(table, columns, select, selectArgs, null, null, null, limit);
        boolean exists = (cursor.getCount()) > 0;
        return exists;
    }

    public Float[] getCoordsFromCode(String code) {
        final String table = "QRTag";
        final String[] columns = {"xcoord", "ycoord"};
        final String select = "url=?";
        Float[] coords = new Float[2];
        coords[0] = null;
        coords[1] = null;
        String[] selectArgs = {code};
        String limit = "1";
        Cursor cursor = db.query(table, columns, select, selectArgs, null, null, null, limit);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            coords[0] = cursor.getFloat(cursor.getColumnIndex("xcoord"));
            coords[1] = cursor.getFloat(cursor.getColumnIndex("ycoord"));
        }
        return coords;
    }
//    private class IndoorLocation {
//        String building;
//        int level;
//        String title;
//        float x_coord;
//        float y_coord;
//    }
    public List<IndoorLocation> getRoomList(String qrCode) {
        final String table = "IndoorLocation";
        final String[] columns = {"bldg", "level", "title", "subtitle", "xcoord", "ycoord"};
        final String select =  "bldg=?";

        String[] args = qrCode.split("::", 0);
        String building_query = args[0];
        String[] selectArgs = {building_query};

        Cursor cursor = db.query(table, columns, select, selectArgs, null, null, null, null);

        List<IndoorLocation> list = new ArrayList<>();
        int row_count = cursor.getCount();

        cursor.moveToFirst();
        for (int i = 0; i < row_count; i++) {
            String building = cursor.getString(cursor.getColumnIndex("bldg"));
            int level = cursor.getInt(cursor.getColumnIndex("level"));
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String subtitle = cursor.getString(cursor.getColumnIndex("subtitle"));
            float x_coord = cursor.getFloat(cursor.getColumnIndex("xcoord"));
            float y_coord = cursor.getFloat(cursor.getColumnIndex("ycoord"));

            list.add(new IndoorLocation(building, level, title, subtitle, x_coord, y_coord));
            cursor.moveToNext();
        }

        return list;
    }

    public void copyDBFromAsset() throws IOException {
        InputStream in  = context.getAssets().open(DATABASE_NAME);
        String outputFileName = DATABASE_PATH+DATABASE_NAME;
        File databaseFile = new File(DATABASE_PATH);
        // check if databases folder exists, if not create one and its subfolders
        Log.e(TAG, "Copying SQLite DB from assets");
        if (!databaseFile.exists()) {
            databaseFile.mkdir();
        }
        OutputStream out = new FileOutputStream(outputFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer))>0){
            out.write(buffer,0,length);
        }
        Log.e(TAG, "Finished copying SQLite DB" );
        out.flush();
        out.close();
        in.close();
    }

    public boolean checkDB() {
        boolean checkDB = false;
        try {
            File file = new File(DATABASE_PATH+DATABASE_NAME);
            checkDB = file.exists();
        } catch(SQLiteException e) {
            Log.d(TAG, e.getMessage());
        }
        return checkDB;
    }
}
