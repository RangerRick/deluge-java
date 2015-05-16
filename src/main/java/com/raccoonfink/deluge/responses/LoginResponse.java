package com.raccoonfink.deluge.responses;

import org.json.JSONObject;

import com.raccoonfink.deluge.DelugeException;

public class LoginResponse extends DelugeResponse {
	private final boolean m_loggedIn;

	public LoginResponse(final Integer httpResponseCode, final JSONObject response) throws DelugeException {
		super(httpResponseCode, response);

		if (response != null) {
			m_loggedIn = response.optBoolean("result");
		} else {
			m_loggedIn = false;
		}
	}

	public boolean isLoggedIn() throws DelugeException {
		return m_loggedIn;
	}

}
