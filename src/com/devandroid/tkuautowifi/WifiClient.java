package com.devandroid.tkuautowifi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EncodingUtils;
import org.apache.http.util.EntityUtils;

import android.content.Context;

public class WifiClient {
	static final String FORM_USERNAME = "user";
	static final String FORM_PASSWORD = "pass";
	static final String FORM_URL = "http://163.13.250.254/goform/eumLogin";

	static final int CONNECTION_TIMEOUT = 15000;
	static final int SOCKET_TIMEOUT = 15000;
	static final int RETRY_COUNT = 4;

	static final String TAG = "TKUWIFILOGIN";

	private String mUsername;
	private String mPassword;
	private DefaultHttpClient mHttpClient;

	private Context context;

	public WifiClient(String username, String password, Context context) {
		mUsername = username;
		mPassword = password;
		this.context = context;

		mHttpClient = new DefaultHttpClient();
		HttpParams params = mHttpClient.getParams();

		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

		mHttpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException exception,
					int executionCount, HttpContext context) {
				if (executionCount >= RETRY_COUNT) {
					return false;
				}
				if (exception instanceof UnknownHostException) {
					return false;
				}
				if (exception instanceof ConnectException) {
					return false;
				}
				if (exception instanceof SSLHandshakeException) {
					return false;
				}

				return true;
			}
		});
	}

	// public boolean loginRequired() throws IOException {
	// try {
	// HttpClient client = new DefaultHttpClient();
	// HttpGet request = new HttpGet("http://74.125.71.147/"); // google
	// HttpResponse response = client.execute(request);
	//
	// if (response.getStatusLine().getStatusCode() != 200) {
	// return true;
	// }
	//
	// } catch (Exception e) {
	// return true;
	// }
	// return false;
	// }

	public void login() throws IOException, LoginException,
			LoginRepeatException {
		try {
			UrlEncodedFormEntity mUrlEncodedFormEntity;
			HttpPost mHttpPost = new HttpPost(FORM_URL);
			mHttpPost.addHeader("Content-Type",
					"application/x-www-form-urlencoded");
			mHttpPost.addHeader("Connection", "keep-alive");
			mHttpPost.addHeader("User-Agent", "Mozilla/5.0");

			ArrayList<BasicNameValuePair> mArrayList = new ArrayList<BasicNameValuePair>();
			mArrayList.add(new BasicNameValuePair(FORM_USERNAME, mUsername));
			mArrayList.add(new BasicNameValuePair(FORM_PASSWORD, mPassword));
			mArrayList.add(new BasicNameValuePair("url",
					"163.13.250.254/eum/login_dialog_new_big.asp"));
			mArrayList.add(new BasicNameValuePair("authtype", "2"));
			mArrayList.add(new BasicNameValuePair("authid", "0"));

			mUrlEncodedFormEntity = new UrlEncodedFormEntity(mArrayList,
					HTTP.UTF_8);
			mHttpPost.setEntity(mUrlEncodedFormEntity);
			HttpResponse httpResponse = mHttpClient.execute(mHttpPost);

			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				byte[] byteresponse = EntityUtils.toByteArray(httpResponse
						.getEntity());
				String str = EncodingUtils.getString(byteresponse, "big5");

				// Log.i(TAG, "---Login page---\n" + str.toString());

				if (str.contains("Login fail")) {
					throw new LoginRepeatException("LOGIN_REPEAT");
				} else if (!str.contains("Login Success")) {
					throw new LoginException("ERROR");
				}
			}
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (ConnectTimeoutException e) {
			throw new ConnectTimeoutException();
		} catch (ProtocolException e) {
			throw new LoginException("ERROR");
		}
	}

	public void logout() throws IOException, LogoutRepeatException {
		String uri_logout = "http://163.13.250.254/goform/eumLogout?system_duration=20&hidden_herf=http%3A%2F%2F163.13.250.254%2Fgoform%2FeumLogout&hidden_host=http%3A%2F%2F163.13.250.254&logout_flag=1";

		HttpGet request = new HttpGet(uri_logout);

		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Connection", "keep-alive");
		request.addHeader("User-Agent", "Mozilla/5.0");

		try {
			HttpResponse response = mHttpClient.execute(request);

			if (response.getStatusLine().getStatusCode() == 200) {
				byte[] byteresponse = EntityUtils.toByteArray(response
						.getEntity());
				String str = EncodingUtils.getString(byteresponse, "big5");

				// Log.i(TAG, "---Logout page---\n" + str.toString());

				if (str.contains("window.parent.close()")) {
					throw new LogoutRepeatException("LOGOUT_REPEAT");
				}
			}
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		}
	}

	public String getChangelog() {
		String changelog = "";

		String uriAPI = "http://dl.dropbox.com/u/19246876/tku_wifi_changelog.txt";
		HttpGet request = new HttpGet(uriAPI);

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Connection", "keep-alive");
		request.addHeader("User-Agent", "Mozilla/5.0");

		try {
			HttpResponse response = new DefaultHttpClient(params)
					.execute(request);
			if (response.getStatusLine().getStatusCode() == 200) {
				InputStream in = response.getEntity().getContent();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String line = null;
				int latest_version = 0;

				while ((line = br.readLine()) != null) {
					if (line.startsWith("VersionCode:")) {
						latest_version = Integer.parseInt(line.substring(line
								.indexOf("VersionCode:") + 12));
					} else if (line.startsWith("ChangeLog:")) {
						changelog = line
								.substring(line.indexOf("ChangeLog:") + 10);
					}
				}

				// Log.i("David", "latest_version: " + latest_version
				// + " changelog: " + changelog);

				if (Utils.getVersionCode(context) >= latest_version) {
					changelog = "";
				}
			}

		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
			changelog = "";
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			changelog = "";
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return changelog;
	}

	@SuppressWarnings("unused")
	private StringBuilder inputStreamToString(InputStream is)
			throws IOException {
		String line = "";
		StringBuilder total = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		while ((line = rd.readLine()) != null) {
			total.append(line);
		}
		return total;
	}
}