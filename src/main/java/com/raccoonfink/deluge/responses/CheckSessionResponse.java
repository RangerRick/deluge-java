package com.raccoonfink.deluge.responses;

import org.json.JSONObject;

import com.raccoonfink.deluge.DelugeException;

public class CheckSessionResponse extends DelugeResponse {
	private final boolean m_sessionActive;

	public CheckSessionResponse(final Integer httpResponseCode, final JSONObject result) throws DelugeException {
		super(httpResponseCode, result);

		if (result != null) {
			m_sessionActive = result.optBoolean("result");
		} else {
			m_sessionActive = false;
		}
	}

	public boolean isSessionActive() {
		return m_sessionActive;
	}
}
