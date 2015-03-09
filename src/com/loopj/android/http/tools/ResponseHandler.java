package com.loopj.android.http.tools;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;

import com.loopj.android.http.AsyncHttpResponseHandler;

import android.os.AsyncTask;
import android.util.Log;

public class ResponseHandler extends AsyncHttpResponseHandler {

	private static final String TAG = ResponseHandler.class.getSimpleName();
	private RequestCacheManager mCacheManager;
	private String mUrl;
	private boolean mCache;

	private RequestListener mRequestListener;
	private int mRequestId;
	
	private long mLastModified = -1L;
	
	public ResponseHandler(RequestCacheManager cacheManager, String url,
			boolean cache, RequestListener requestListener, int requestId) {
		this.mCacheManager = cacheManager;
		this.mUrl = url;
		this.mCache = cache;
		
		this.mRequestListener = requestListener;
		this.mRequestId = requestId;
	}

	@Override
	public void sendResponseMessage(HttpResponse response) throws IOException {
		// do not process if request has been cancelled
		if (!Thread.currentThread().isInterrupted()) {
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() >= 300) {
				sendFailureMessage(status.getStatusCode(),
						response.getAllHeaders(), null,
						new HttpResponseException(status.getStatusCode(),
								status.getReasonPhrase()));
			} else {
				byte[] responseBody = null;
				if (mCache) {
					if (!isLastModified(response)) {
						responseBody = loadCacheResource();
					}
				}
				if (responseBody != null) {
					sendSuccessMessage(status.getStatusCode(),
							response.getAllHeaders(), responseBody);
				} else {
					// additional cancellation check as getResponseData() can take non-zero time to process
					if (!Thread.currentThread().isInterrupted()) {
						responseBody = getResponseData(response.getEntity());
						System.out.println(response.getAllHeaders());
						sendSuccessMessage(status.getStatusCode(),
								response.getAllHeaders(), responseBody);
						setCache(responseBody);
					}
				}
			}
		}
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
				count+=len;
				sendProgressMessage(count, (int) (contentLength <= 0 ? 1 : contentLength));
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
	
	@Override
	public void onProgress(int bytesWritten, int totalSize) {
		this.mRequestListener.onProgress(bytesWritten, totalSize, mRequestId);
	}

	@Override
	public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
		this.mRequestListener.onCompleted(RequestListener.OK, responseBody,
				"server response ok", mRequestId);
	}

	@Override
	public void onFailure(int statusCode, Header[] headers,
			byte[] responseBody, Throwable error) {
		this.mRequestListener.onCompleted(RequestListener.ERR, null,
				error.toString(), mRequestId);
	}
	
	private void setCache(final byte[] responseBody) {
		if (mCache) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					mCacheManager.saveCacheResource(mUrl, responseBody,
							mLastModified);
					return null;
				}
			}.execute();
		}
	}

	private boolean isLastModified(HttpResponse response) {
		if (!this.mCacheManager.hasCache(mUrl)) {
			return true;
		} else {
			mLastModified = -1l;
			try {
				final String last = response.getLastHeader("last-modified")
						.getValue();
				if (null != last && !"".equals(last)) {
					java.util.Date d = new java.util.Date(last);
					mLastModified = d.getTime();
				}
			} catch (Exception e) {
			}

			final long ret = this.mCacheManager.getLastModified(mUrl);
			return ret != -1 && ret != mLastModified;
		}
	}

	   /**
     * Attempts to encode response bytes as string of set encoding
     *
     * @param charset     charset to create string with
     * @param stringBytes response bytes
     * @return String of set encoding or null
     */
    public static String getResponseString(byte[] stringBytes, String charset) {
        try {
            String toReturn = (stringBytes == null) ? null : new String(stringBytes, charset);
            if (toReturn != null && toReturn.startsWith(UTF8_BOM)) {
                return toReturn.substring(1);
            }
            return toReturn;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Encoding response into string failed", e);
            return null;
        }
    }
    
}
