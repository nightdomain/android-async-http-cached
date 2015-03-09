package com.loopj.android.http.tools;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * RequestChacheManager for "GET" method if isCahce
 * 
 * @author savant-pan
 * 
 */
public class RequestCacheManager {
	private static RequestCacheManager INSTANCE = null;
	private RequestDBHelper requestDBHelper = null;
	private Context mContext;
	
	private RequestCacheManager(Context context) {
		this.mContext = context;
		this.requestDBHelper = new RequestDBHelper(context);
	}

	/**
	 * get instance of RequestChacheManager
	 * 
	 * @param context
	 *            Context value
	 * @return
	 */
	public static RequestCacheManager getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new RequestCacheManager(context);
		}
		return INSTANCE;
	}

	/**
	 * update record: add or update
	 * 
	 * @param item
	 */
	public void update(String url, long lastModified) {
		SQLiteDatabase db = requestDBHelper.getWritableDatabase();
		if (!find(url)) { // add if not exist
			db.execSQL("insert into request_cache(url, lastmodified) values(?,?)",
					new Object[] { url, String.valueOf(lastModified) });
		} else { // update is exist
			db.execSQL("update request_cache set lastmodified=? where url=?",
					new Object[] { String.valueOf(lastModified), url });
		}
	}

	/**
	 * get lastmotified value by url
	 * 
	 * @param filename
	 * @return
	 */
	public long getLastModified(String url) {
		SQLiteDatabase db = requestDBHelper.getReadableDatabase();
		Cursor cursor = null;
		try {
			long ret = 0l;
			cursor = db.rawQuery("select * from request_cache where url=?", new String[] { url });
			if (cursor.moveToFirst()) {
				final String last = cursor.getString(cursor.getColumnIndex("lastmodified"));
				ret = Long.valueOf(last);
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return 0l;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * check exists of url
	 * 
	 * @param url
	 * @return
	 */
	private boolean find(String url) {
		SQLiteDatabase db = requestDBHelper.getReadableDatabase();
		Cursor cursor = null;
		try {
			boolean flag = false;
			cursor = db.rawQuery("select * from request_cache where url=?", new String[] { url });
			if (cursor.moveToFirst()) {
				flag = true;
			}
			return flag;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * delete all records
	 */
	public void deleteAll() {
		List<String> all = getUrls();
		for (String url : all) {
			SQLiteDatabase database = requestDBHelper.getWritableDatabase();
			database.execSQL("delete from request_cache where url=?", new Object[] { url });
		}
	}

	/**
	 * get all urls in database
	 * 
	 * @return
	 */
	private List<String> getUrls() {
		List<String> ret = new ArrayList<String>();
		SQLiteDatabase db = requestDBHelper.getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("select * from request_cache", null);
			if (cursor.moveToFirst()) {
				do {
					final String url = cursor.getString(0);
					ret.add(url);
				} while (cursor.moveToNext());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret;
	}
	
	public FileInputStream getInputStream(String url) throws FileNotFoundException {
		return mContext.openFileInput(convertFilename(url));
	}

	public void saveCacheResource(String url,
			byte[] response, long lastModified) {
		ByteArrayInputStream ins = null;
		FileOutputStream os = null;
		try {
			ins = new ByteArrayInputStream(response);
			os = mContext.openFileOutput(convertFilename(url),
					Context.MODE_PRIVATE);
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = ins.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
			os.flush();

			RequestCacheManager.getInstance(mContext).update(url, lastModified);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (ins != null) {
				try {
					ins.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 检测缓存
	 */
	public boolean hasCache(String url) {
		FileInputStream ins = null;
		try {
			ins = mContext.openFileInput(convertFilename(url));
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (ins != null) {
				try {
					ins.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 对字符串进行MD5加密。
	 */
	private static String convertFilename(String strInput) {
		StringBuffer buf = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(strInput.getBytes("UTF-8"));
			byte b[] = md.digest();
			buf = new StringBuffer(b.length * 2);
			for (int i = 0; i < b.length; i++) {
				if (((int) b[i] & 0xff) < 0x10) { /* & 0xff转换无符号整型 */
					buf.append("0");
				}
				buf.append(Long.toHexString((int) b[i] & 0xff)); /* 转换16进制,下方法同 */
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return buf.toString().substring(8, 24);
	}
	
	/**
	 * RequestDBHelper
	 */
	private static class RequestDBHelper extends SQLiteOpenHelper {
		private static final String DB_NAME = "requestCache.db";
		private static final int DB_VER = 1;
		private static final String TABLE_CREATE = "create table request_cache(url varchar(32) primary key,  lastmodified varchar(16))";

		public RequestDBHelper(Context context) {
			super(context, DB_NAME, null, DB_VER);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

		}
	}
	
}