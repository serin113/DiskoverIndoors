package com.clemcab.diskoverindoors;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
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
            File file = new File(DATABASE_PATH+DATABASE_NAME);
            if(file.exists()) {
                file.delete();
                Log.d(TAG, "Database deleted.");
            }
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

    public void copyDBFromAsset() throws IOException {
        InputStream in  = context.getAssets().open(DATABASE_NAME);
        String outputFileName = DATABASE_PATH+DATABASE_NAME;
        File databaseFile = new File(DATABASE_PATH);
        // check if databases folder exists, if not create one and its subfolders
        Log.e("sample", "Starting copying");
        if (!databaseFile.exists()) {
            databaseFile.mkdir();
        }
        OutputStream out = new FileOutputStream(outputFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer))>0){
            out.write(buffer,0,length);
        }
        Log.e("sample", "Completed" );
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
