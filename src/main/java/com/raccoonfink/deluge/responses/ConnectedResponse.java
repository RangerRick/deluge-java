package com.raccoonfink.deluge.responses;

import org.json.JSONException;
import org.json.JSONObject;

import com.raccoonfink.deluge.DelugeException;

public class ConnectedResponse extends DelugeResponse {
	private final boolean m_connected;

	public ConnectedResponse(final Integer httpResponseCode, final JSONObject response) throws DelugeException {
		super(httpResponseCode, response);
		try {
			m_connected = response.getBoolean("result");
		} catch (final JSONException e) {
			throw new DelugeException(e);
		}
	}

	public ConnectedResponse(final Integer httpResponseCode, final JSONObject response, final boolean isConnected) throws DelugeException {
		super(httpResponseCode, response);
		m_connected = isConnected;
	}

	public boolean isConnected() throws DelugeException {
		return m_connected;
	}

}
