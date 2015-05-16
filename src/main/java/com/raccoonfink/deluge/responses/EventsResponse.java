package com.raccoonfink.deluge.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raccoonfink.deluge.DelugeEvent;
import com.raccoonfink.deluge.DelugeException;

public class EventsResponse extends DelugeResponse {
	private final List<DelugeEvent> m_events = new ArrayList<DelugeEvent>();

	public EventsResponse(final Integer httpResponseCode, final JSONObject result) throws DelugeException {
		super(httpResponseCode, result);

		if (!result.isNull("result")) {
			try {
				final JSONArray res = result.getJSONArray("result");
				for (int i=0; i < res.length(); i++) {
					m_events.add(new DelugeEvent(res.getJSONArray(i)));
				}
			} catch (final JSONException e) {
				throw new DelugeException(e);
			}
		}
	}
	
	public List<DelugeEvent> getEvents() {
		return Collections.unmodifiableList(m_events);
	}
}
