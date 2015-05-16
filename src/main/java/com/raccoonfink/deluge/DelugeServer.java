package com.raccoonfink.deluge;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.SSLCertificateSocketFactory;

import com.raccoonfink.deluge.responses.CheckSessionResponse;
import com.raccoonfink.deluge.responses.ConnectedResponse;
import com.raccoonfink.deluge.responses.DeleteSessionResponse;
import com.raccoonfink.deluge.responses.DelugeResponse;
import com.raccoonfink.deluge.responses.EventsResponse;
import com.raccoonfink.deluge.responses.HostResponse;
import com.raccoonfink.deluge.responses.LoginResponse;
import com.raccoonfink.deluge.responses.UIResponse;

public class DelugeServer {
	private final URL m_url;
	private final String m_password;

	private int m_timeout;

	private final CookieManager m_cookieManager = new CookieManager();
	private int m_counter = 0;

	public DelugeServer(final String url, final String password) throws MalformedURLException {
		final StringBuilder sb = new StringBuilder(url);
		if (!url.endsWith("/")) {
			sb.append("/");
		}
		sb.append("json");
		m_url = new URL(sb.toString());
		m_password = password;

		HttpURLConnection.setFollowRedirects(true);
	}

	public void setTimeout(final int timeout) {
		m_timeout = timeout;
	}

	public DelugeResponse makeRequest(final DelugeRequest delugeRequest) throws DelugeException {
		System.err.println("URL: " + m_url);

		Integer connectionResponseCode = null;
		JSONObject jsonResponse = null;
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) m_url.openConnection();
		} catch (final IOException e) {
			throw new DelugeException(e);
		}

		try {
			if (connection instanceof HttpsURLConnection) {
				final HttpsURLConnection sslConnection = (HttpsURLConnection)connection;
				sslConnection.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(1000, null));
			}
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Accept-Encoding", "compress;q=0.5, gzip;q=1.0");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setConnectTimeout(m_timeout);
			connection.setReadTimeout(m_timeout);
			connection.setUseCaches(false);
			connection.setDoInput(true);

			final String cookieHeader = getCookieHeader();
			//System.err.println("Setting cookie: " + cookieHeader);
			connection.setRequestProperty("Cookie", cookieHeader);

			final String postData = delugeRequest.toPostData(m_counter++);
			if (postData != null) {
				System.err.println("POST: " + postData);
				connection.setRequestProperty("Content-Length", String.valueOf(postData.getBytes("UTF-8").length));
				connection.setDoOutput(true);

				final DataOutputStream os = new DataOutputStream(connection.getOutputStream());
				os.writeBytes(postData);
				os.flush();
				os.close();
			}

			InputStream is = null;
			InputStreamReader isr = null;
			BufferedReader reader = null;

			try {
				is = connection.getInputStream();
				if ("gzip".equals(connection.getContentEncoding()) || "compress".equals(connection.getContentEncoding())) {
					isr = new InputStreamReader(new GZIPInputStream(is), "UTF-8");
				} else if ("".equals(connection.getContentEncoding()) || connection.getContentEncoding() == null) {
					isr = new InputStreamReader(is, "UTF-8");
				} else {
					System.err.println("WARNING: Unknown Content-Encoding: " + connection.getContentEncoding());
					isr = new InputStreamReader(is, "UTF-8");
				}

				reader = new BufferedReader(isr);
				String line;

				final StringBuilder response = new StringBuilder(is.available());
				while ((line = reader.readLine()) != null) {
					//System.err.println("line = " + line);
					response.append(line).append("\n");
				}
				System.err.println("Response: " + response.toString());
				connectionResponseCode = connection.getResponseCode();
				jsonResponse = new JSONObject(response.toString());

				final Map<String, List<String>> headerFields = connection.getHeaderFields();
				addCookies(headerFields.get("Set-Cookie"));
			} finally {
				closeQuietly(reader);
				closeQuietly(is);
			}
		} catch (final IOException e) {
			throw new DelugeException(e);
		} catch (final JSONException e) {
			throw new DelugeException(e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		try {
			if (jsonResponse.has("error") && !jsonResponse.isNull("error")) {
				final JSONObject error = jsonResponse.getJSONObject("error");
				final String message = error.optString("message");
				final int code = error.optInt("code");
				final StringBuilder builder = new StringBuilder("Error");
				if (code >= 0) {
					builder.append(" ").append(code);
				}
				if (message != null) {
					builder.append(": ").append(message);
				}
				throw new DelugeException(builder.toString());
			}
		} catch (final JSONException e) {
			throw new DelugeException(e);
		}
		return new DelugeResponse(connectionResponseCode, jsonResponse);
	}

	private static final void closeQuietly(final Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (final IOException e) {
		}
	}

	private String getCookieHeader() {
		final List<HttpCookie> cookies = m_cookieManager.getCookieStore().getCookies();
		if (cookies == null) {
			return null;
		}
		final StringBuilder builder = new StringBuilder();
		String separator = "";
		for (int i=0; i < cookies.size(); i++) {
			builder.append(separator).append(cookies.get(i));
			separator = ",";
		}
		return builder.toString();
	}

	private void addCookies(final List<String> cookies) {
		if (cookies == null) {
			return;
		}
		for (final String cookie : cookies) {
			//System.err.println("Response contained cookie: " + cookie);
			m_cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
		}
	}

	public DelugeResponse listMethods() throws DelugeException {
		return makeRequest(new DelugeRequest("system.listMethods"));
	}

	public CheckSessionResponse checkSession() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("auth.check_session"));
		return new CheckSessionResponse(response.getResponseCode(), response.getResponseData());
	}

	public LoginResponse login() throws DelugeException {
		final DelugeRequest request = new DelugeRequest("auth.login", m_password);
		final DelugeResponse response = makeRequest(request);
		return new LoginResponse(response.getResponseCode(), response.getResponseData());
	}

	public DeleteSessionResponse deleteSession() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("auth.delete_session"));
		return new DeleteSessionResponse(response.getResponseCode(), response.getResponseData());
	}

	public void registerEventListeners() throws DelugeException {
		final List<String> events = Arrays.asList("ConfigValueChangedEvent",
				"NewVersionAvailableEvent",
				"PluginDisabledEvent",
				"PluginEnabledEvent",
				"PreTorrentRemovedEvent",
				"SessionPausedEvent",
				"SessionResumedEvent",
				"SessionStartedEvent",
				"TorrentAddedEvent",
				"TorrentFileRenamedEvent",
				"TorrentFinishedEvent",
				"TorrentFolderRenamedEvent",
				"TorrentQueueChangedEvent",
				"TorrentRemovedEvent",
				"TorrentResumedEvent",
				"TorrentStateChangedEvent");

		for (final String event : events) {
			makeRequest(new DelugeRequest("web.register_event_listener", event));
		}
	}

	public ConnectedResponse isConnected() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.connected"));
		return new ConnectedResponse(response.getResponseCode(), response.getResponseData());
	}

	public HostResponse getHosts() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.get_hosts"));
		return new HostResponse(response.getResponseCode(), response.getResponseData(), false);
	}

	public HostResponse getHostStatus(final String id) throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.get_host_status", id));
		return new HostResponse(response.getResponseCode(), response.getResponseData(), true);
	}

	public ConnectedResponse connect(final Host host) throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.connect", host.getId()));
		if (response.getResponseData().isNull("result")) {
			return new ConnectedResponse(response.getResponseCode(), response.getResponseData(), true);
		} else {
			return new ConnectedResponse(response.getResponseCode(), response.getResponseData());
		}
	}

	public ConnectedResponse disconnect() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.disconnect"));
		if (response.getResponseData().isNull("result")) {
			return new ConnectedResponse(response.getResponseCode(), response.getResponseData(), false);
		} else {
			return new ConnectedResponse(response.getResponseCode(), response.getResponseData(), true);
		}
	}

	public EventsResponse getEvents() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.get_events"));
		return new EventsResponse(response.getResponseCode(), response.getResponseData());
	}

	public UIResponse updateUI() throws DelugeException {
		final DelugeResponse response = makeRequest(new DelugeRequest("web.update_ui", new JSONArray(Arrays.asList(
				"queue",
				"name",
				"total_size",
				"state",
				"progress",
				"num_seeds",
				"total_seeds",
				"num_peers",
				"total_peers",
				"download_payload_rate",
				"upload_payload_rate",
				"eta",
				"ratio",
				"distributed_copies",
				"is_auto_managed",
				"time_added",
				"tracker_host",
				"save_path",
				"total_done",
				"total_uploaded",
				"max_download_speed",
				"max_upload_speed",
				"seeds_peers_ratio"
				)), new JSONObject()));
		return new UIResponse(response.getResponseCode(), response.getResponseData());
	}
}
