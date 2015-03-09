package com.loopj.android.http.tools;

/**
 * Request Listener for AsyncHttpResponseHandler
 * 
 * @author savant-pan
 * 
 */
public interface RequestListener {
	/**
	 * Status OK
	 */
	public final static int OK = 0;
	/**
	 * Status ERR
	 */
	public final static int ERR = 1;

	/**
	 * callback when Request start
	 */
	void onStart();
	
    /**
     * Fired when the request progress, override to handle in your own code
     *
     * @param bytesWritten offset from start of file
     * @param totalSize    total size of file
     * @param actionId    request id
     */
	public void onProgress(int bytesWritten, int totalSize, int actionId);

	/**
	 * callback when Request end
	 * 
	 * @param data
	 *            byte array if any
	 * @param statusCode
	 *            Request Status
	 * @param description
	 *            description
	 * @param actionId
	 *            request identifier
	 */
	void onCompleted(int statusCode, byte[] data, String description, int actionId);
}
