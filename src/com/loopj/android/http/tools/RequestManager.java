package com.loopj.android.http.tools;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

/**
 * Request Manager based on AsyncHttpClient
 * 
 * @author savant-pan
 * 
 */
public class RequestManager {
	private final AsyncHttpClient mAsyncHttpClient;
	private final RequestCacheManager mCacheManager;
	private Context mContext;

	private static volatile RequestManager INSTANCE = null;

	protected RequestManager(Context context) {
		this.mContext = context;
		this.mCacheManager = RequestCacheManager.getInstance(context);
		this.mAsyncHttpClient = new AsyncHttpClient();
	}

	public static RequestManager getInstance(Context context) {
		if (INSTANCE == null) {
			synchronized (RequestManager.class) {
				if (INSTANCE == null) {
					INSTANCE = new RequestManager(context);
				}
			}
		}
		return INSTANCE;
	}

	/**
	 * 清空缓存文件
	 * 
	 * @param context
	 */
	public static void clearHttpCache(Context context) {
		final String fl[] = context.fileList();
		try {
			for (String f : fl) {
				context.deleteFile(f);
			}
			RequestCacheManager.getInstance(context).deleteAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cancelRequests() {
		mAsyncHttpClient.cancelRequests(this.mContext, true);
	}

	/**
	 * 参数列表请求
	 * 
	 * @param url
	 * @param params
	 * @param requestListener
	 * @param actionId
	 */
	public void post(String url, RequestParams params,
			RequestListener requestListener, int actionId) {
		mAsyncHttpClient.post(this.mContext, url, params,
				new ResponseHandler(this.mCacheManager, url, false,
						requestListener, actionId));
	}

	/**
	 * JSON　参数请求
	 * 
	 * @param url
	 * @param params
	 * @param requestListener
	 * @param actionId
	 */
	public void post(String url, JSONObject params,
			RequestListener requestListener, int actionId) {
		mAsyncHttpClient.post(this.mContext, url,
				rpcToEntity(params.toString(), "application/json"),
				"application/json", new ResponseHandler(this.mCacheManager,
						url, false, requestListener, actionId));
	}

	/**
	 * JSON　参数请求
	 * 
	 * @param url
	 * @param headers
	 * @param params
	 * @param requestListener
	 * @param actionId
	 */
	public void post(String url, Header[] headers, JSONObject params,
			RequestListener requestListener, int actionId) {
		mAsyncHttpClient.post(this.mContext, url, headers,
				rpcToEntity(params.toString(), "application/json"),
				"application/json", new ResponseHandler(this.mCacheManager,
						url, false, requestListener, actionId));
	}

	/**
	 * XML　参数请求
	 * 
	 * @param url
	 * @param params
	 * @param requestListener
	 * @param actionId
	 */
	public void post(String url, String params,
			RequestListener requestListener, int actionId) {
		mAsyncHttpClient.post(this.mContext, url,
				rpcToEntity(params, "application/xml"), "application/xml",
				new ResponseHandler(this.mCacheManager, url, false,
						requestListener, actionId));
	}

	/**
	 * XML　参数请求
	 * 
	 * @param url
	 * @param headers
	 * @param params
	 * @param requestListener
	 * @param actionId
	 */
	public void post(String url, Header[] headers, String params,
			RequestListener requestListener, int actionId) {
		mAsyncHttpClient.post(this.mContext, url, headers,
				rpcToEntity(params, "application/xml"), "application/xml",
				new ResponseHandler(this.mCacheManager, url, false,
						requestListener, actionId));
	}

	/**
	 * 将JSON/XML字符串转为HttpEntity(StringEntity)
	 * 
	 * @param params
	 * @param contentType
	 * @return
	 */
	private static HttpEntity rpcToEntity(String params, String contentType) {
		StringEntity entity = null;
		if (!TextUtils.isEmpty(params)) {
			try {
				entity = new StringEntity(params, HTTP.UTF_8);
				entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
						contentType));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return entity;
	}

	/**
	 * get数据
	 * 
	 * @param url
	 * @param requestListener
	 * @param actionId
	 */
	public void get(String url, RequestListener requestListener, int actionId) {
		get(urlEncode(url), null, requestListener, false, actionId);
	}

	/**
	 * get数据
	 * 
	 * @param context
	 * @param url
	 * @param requestListener
	 * @param isCache
	 * @param actionId
	 */
	public synchronized void get(final String url, final RequestParams params,
			final RequestListener requestListener, final boolean isCache,
			final int actionId) {
		if (!hasNetwork(this.mContext)) {
			new CacheLoadTask(this.mCacheManager, url, requestListener,
					actionId).execute();
		} else {
			mAsyncHttpClient.get(this.mContext, url, params,
					new ResponseHandler(this.mCacheManager, url, isCache,
							requestListener, actionId));
		}
	}

	/**
	 * cache load task
	 */
	private class CacheLoadTask extends AsyncTask<Void, Integer, byte[]> {
		private RequestListener mRequestListener;
		private int mRequestId;
		private RequestCacheManager mCacheManager;
		private String mUrl;

		public CacheLoadTask(RequestCacheManager cacheManager, String url,
				RequestListener requestListener, int requestId) {
			this.mCacheManager = cacheManager;
			this.mUrl = url;

			this.mRequestListener = requestListener;
			this.mRequestId = requestId;
		}

		protected void onPreExecute() {
			mRequestListener.onStart();
		}

		@Override
		protected byte[] doInBackground(Void... params) {
			if (!mCacheManager.hasCache(mUrl)) {
				return null;
			}
			
			return loadCacheResource();
		}

		/**
		 * 读缓存
		 * 
		 * @param context
		 * @param url
		 */
		public byte[] loadCacheResource() {
			FileInputStream ins = null;
			try {
				ins = mCacheManager.getInputStream(mUrl);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] bytes = new byte[4096];
				int len = 0;
				int count = 0;
				int contentLength = ins.available();

				while ((len = ins.read(bytes)) > 0) {
					bos.write(bytes, 0, len);
					count += len;
					publishProgress(count, contentLength);
				}
				bos.flush();
				return bos.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
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

		protected void onProgressUpdate(Integer... values) {
			mRequestListener.onProgress(values[0], values[1], mRequestId);
		}

		protected void onPostExecute(byte[] result) {
			boolean flag = (result != null);
			mRequestListener.onCompleted((flag ? RequestListener.OK
					: RequestListener.ERR), result, flag ? "load cache ok"
					: "load cache error", mRequestId);
		}
	}

	/**
	 * 检验网络是否有连接，有则true，无则false
	 * 
	 * @param context
	 * @return
	 */
	private static boolean hasNetwork(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected()) {
			return true;
		}
		return false;
	}

	/**
	 * 网址汉字编码
	 */
	private static String urlEncode(String str) {
		StringBuffer buf = new StringBuffer();
		byte c;
		byte[] utfBuf;
		try {
			utfBuf = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out
					.println("URLEncode: Failed to get UTF-8 bytes from string.");
			utfBuf = str.getBytes();
		}
		for (int i = 0; i < utfBuf.length; i++) {
			c = utfBuf[i];
			if ((c >= '0' && c <= '9')
					|| (c >= 'A' && c <= 'Z')
					|| (c >= 'a' && c <= 'z')
					|| (c == '.' || c == '-' || c == '*' || c == '_')
					|| (c == ':' || c == '/' || c == '=' || c == '?'
							|| c == '&' || c == '%')) {
				buf.append((char) c);
			} else {
				buf.append("%").append(Integer.toHexString((0x000000FF & c)));
			}
		}
		return buf.toString();
	}

}
