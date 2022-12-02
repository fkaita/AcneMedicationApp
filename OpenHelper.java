package com.example.acnedetection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class OpenHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "DB.db";
    private static final String TABLE_NAME = "acne_db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    "_id" + " INTEGER PRIMARY KEY," +
                    "date" + " TEXT," +
                    "class" + " INTEGER," +
                    "count" + " INTEGER)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    OpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(
                SQL_DELETE_ENTRIES
        );
        onCreate(db);
    }

    public float countByDate(String start, String end){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Integer> array_list = new ArrayList<Integer>();
//        String SQL_MATCH_ENTRIES = "SELECT * FROM " + TABLE_NAME +
//                " WHERE date >= " + start + " AND date <= " + end;
        String SQL_MATCH_ENTRIES = "SELECT * FROM " + TABLE_NAME +
                " WHERE date LIKE '" + start + "%'";
        Cursor res = db.rawQuery(SQL_MATCH_ENTRIES, null);
        res.moveToFirst();
        for (int i=0; i < res.getCount() ;i++) {
            array_list.add(res.getInt(3));
            System.out.println("3rd col " + res.getInt(2) + " 4th col " + res.getInt(3) );
            res.moveToNext();
        }
        res.close();
        db.close();
        int sum = 0;
        int cnt = 0;
        for (int i=0; i < array_list.size(); i++){
            sum += array_list.get(i);
            cnt += 1;
        }
        if (cnt!=0){
            return sum/cnt;
        }else{
            return -1; // This means no measurement was done.
        }




    }

}
