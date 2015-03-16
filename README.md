android-async-http with response cached.
-------------------

基于Android Asynchronous HTTP Library (https://github.com/loopj/android-async-http)实现。  
1.增加响应数据本地缓存功能。  
2.便于JSON-RPC,XML-RPC方式调用。  

用法：
--------------------

	public class TestActivity extends Activity {

		private static final int REQUEST_GET_ID = 0;
		private static final int REQUEST_POST_ID = 1;
		private static final int REQUEST_POST_JSON_ID = 2;
		private static final int REQUEST_POST_XML_ID = 3;

		private RequestManager mRequestManager;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.mRequestManager = RequestManager
					.getInstance(getApplicationContext());
			
			get();
			postParams();
			postJSONObject();
			postXML();
		}

		/**
		 * get by url
		 */
		private void get() {
			this.mRequestManager
					.get("http://app.shafa.com/api/push/download/52a093cf3bf55d361e000477?response-content-type=application%2fvnd.android.package-archive",
							null, requestListener, true, REQUEST_GET_ID);
		}

		/**
		 * post by RequestParams
		 */
		private void postParams() {
			final RequestParams params = new RequestParams();
			params.put("key1", "value1");
			params.put("key2", "value2");
			this.mRequestManager.post("http://server.winfirm.net/memoServer",
					params, requestListener, REQUEST_POST_ID);
		}

		/**
		 * post by JSONObject
		 */
		private void postJSONObject() {
			final JSONObject json = new JSONObject();
			try {
				json.put("key1", "value1");
				json.put("key2", "value2");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			this.mRequestManager.post("http://server.winfirm.net/memoServer", json,
					requestListener, REQUEST_POST_JSON_ID);
		}

		/**
		 * post by xml
		 */
		private void postXML() {
			final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><key1>value1</key1><key2>value2</key2>";
			this.mRequestManager.post("http://server.winfirm.net/memoServer", xml,
					requestListener, REQUEST_POST_XML_ID);
		}

		/**
		 * request listener
		 */
		private RequestListener requestListener = new RequestListener() {
			@Override
			public void onStart() {

			}

			@Override
			public void onCompleted(int statusCode, byte[] data,
					String description, int actionId) {
				try {
					System.out.println(new String(data, "UTF-8"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (REQUEST_GET_ID == actionId) {
					if (RequestListener.OK == statusCode) {
						// sucess
					} else {
						// handler error case
					}
				} else if (REQUEST_POST_ID == actionId) {

				} else if (REQUEST_POST_JSON_ID == actionId) {

				} else if (REQUEST_POST_XML_ID == actionId) {

				}
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize, int actionId) {
				System.out.println(bytesWritten + "/" + totalSize);
			}
		};

	}


