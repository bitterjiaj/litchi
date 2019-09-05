//-------------------------------------------------
// Litchi Game Server Framework
// Copyright(c) 2019 phantaci <phantacix@qq.com>
// MIT Licensed
//-------------------------------------------------
package litchi.core.net.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Http {
	private static final Logger LOGGER = LoggerFactory.getLogger(Http.class);

	private int connectTimeout = 5000; // 5s
	private int readTimeout = 5000; // 5s
	private int writeTimeout = 5000; // 5s

	private OkHttpClient client;
	private OkHttpClient httpsClient;

	private static Http instance;

	private Http() {
		initHttp();
	}

	private void initHttp() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
		builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
		builder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);

		SSLUtils.SSLParams ssl = SSLUtils.getSslSocketFactory(null, null, null);
		builder.sslSocketFactory(ssl.sSLSocketFactory, ssl.trustManager);

		// Dispatcher dispatcher = new Dispatcher();
		// dispatcher.setMaxRequests(128);
		// dispatcher.setMaxRequestsPerHost(12);
		// builder.dispatcher(dispatcher);

		this.client = builder.build();
	}

	private void initHttps() {
		SSLUtils.SSLParams ssl = SSLUtils.getSslSocketFactory(null, null, null);

		this.httpsClient = new OkHttpClient.Builder()
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.hostnameVerifier(new HostnameVerifier() {

					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				})
				.sslSocketFactory(ssl.sSLSocketFactory, ssl.trustManager)
				.build();
	}

	public static Http instance() {
		if (instance == null) {
			instance = new Http();
		}
		return instance;
	}

	public static String get(String url) {
		return get(url, null);
	}

	public static String get(String url, Map<String, String> params) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(concatUrl(url, params));

		return instance().execute(reqBuilder.build());
	}

	public static void get(String url, Map<String, String> params, StringCallback callback) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(concatUrl(url, params));

		instance().execute(reqBuilder.build(), callback);
	}

	public static String post(String url, Map<String, String> params) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(url);

		FormBody.Builder formBuilder = new FormBody.Builder();
		for (Entry<String, String> entry : params.entrySet()) {
			formBuilder.add(entry.getKey(), entry.getValue());
		}
		reqBuilder.post(formBuilder.build());

		return instance().execute(reqBuilder.build());
	}

	public static String postBody(String url, String body) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(url);
		reqBuilder.post(RequestBody.create(null, body));
		return instance().execute(reqBuilder.build());
	}

	public static String postBody(String url, FormBody formBody) {
		Request request = new Request.Builder()
				.url(url)
				.post(formBody)
				.build();
		try {
			String text = instance().execute(request);
			return text;
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		return null;
	}
	
	public static InputStream postBodyAndGetStream(String url, String body) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(url);
		reqBuilder.post(RequestBody.create(null, body));
		return instance().executeAndGetStream(reqBuilder.build());
	}

	public static void post(String url, Map<String, String> params, StringCallback callback) {
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(url);

		FormBody.Builder formBuilder = new FormBody.Builder();
		for (Entry<String, String> entry : params.entrySet()) {
			formBuilder.add(entry.getKey(), entry.getValue());
		}
		reqBuilder.post(formBuilder.build());

		instance().execute(reqBuilder.build(), callback);
	}

	public static String concatUrl(String url, Map<String, String> data) {
		if (data == null) {
			return url;
		}

		try {
			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> entry : data.entrySet()) {
				sb.append("&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "utf-8"));
			}

			if (sb.length() < 1) {
				return url;
			}

			String prefix = sb.substring(1).toString();
			if (url.indexOf("?") < 1) {
				url += "?";
			}

			return url + prefix;
		} catch (Exception ex) {
			LOGGER.warn("{}", ex);
		}
		return url;
	}

	private String execute(Request request) {
		final Call call = instance().client.newCall(request);
		Response response = null;
		try {
			response = call.execute();
			ResponseBody body = response.body();
			return body.string();
		} catch (IOException e) {
			LOGGER.error("{}", e);
		} finally {
			if (response != null) {
				response.close();
			}
		}
		return "";
	}
	
	private InputStream executeAndGetStream(Request request) {
		final Call call = instance().client.newCall(request);
		
		Response response = null;
		try {
			response = call.execute();
			ResponseBody body = response.body();
			return body.byteStream();
		} catch (IOException e) {
			LOGGER.error("{}", e);
		}
		return null;
	}

	private void execute(Request request, StringCallback callback) {
		final Call call = instance().client.newCall(request);
		call.enqueue(new Callback() {

			@Override
			public void onResponse(Call paramCall, Response paramResponse) throws IOException {
				callback.completed(paramResponse.body().string());
			}

			@Override
			public void onFailure(Call paramCall, IOException ex) {
				callback.failed(ex);
			}
		});
	}

}