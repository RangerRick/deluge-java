package com.raccoonfink.deluge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.raccoonfink.deluge.responses.CheckSessionResponse;
import com.raccoonfink.deluge.responses.ConnectedResponse;
import com.raccoonfink.deluge.responses.DeleteSessionResponse;
import com.raccoonfink.deluge.responses.DelugeResponse;
import com.raccoonfink.deluge.responses.HostResponse;
import com.raccoonfink.deluge.responses.LoginResponse;
import com.raccoonfink.deluge.responses.UIResponse;

public class DelugeServerTest {
	private static final String DELUGE_JSON_URL = System.getProperty("delugeJsonUrl", "http://defiance.dyndns.org:8112/");
	private static final String DELUGE_PASSWORD = System.getProperty("delugePassword");
	private static final String DELUGE_HOST_HASH = System.getProperty("delugeHostHash", "1cc7ee2e2259ad6c29430b2f5ae75919009ec4da");

	/* Attempt to reset the connection/environment state before any tests run. */
	@Before
	public void setUp() {
		try {
			final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
			server.login();
			server.disconnect();
			server.deleteSession();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Test(expected=DelugeException.class)
	public void testBadRequest() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.makeRequest(new DelugeRequest("foo.bar"));
	}

	@Test
	public void testListMethods() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		DelugeResponse response = server.listMethods();
		assertNotNull(response);
		assertEquals(Integer.valueOf(0), Integer.valueOf(response.getId()));

		response = server.listMethods();
		assertEquals(Integer.valueOf(1), Integer.valueOf(response.getId()));
	}

	@Test
	public void testCheckSession() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		CheckSessionResponse response = server.checkSession();
		assertNotNull(response);
		assertFalse(response.isSessionActive());
	}

	@Test
	public void testLogin() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		LoginResponse response = server.login();
		assertNotNull(response);
		assertTrue(response.isLoggedIn());
	}

	@Test
	public void testDeleteSession() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		LoginResponse loginResponse = server.login();
		assertNotNull(loginResponse);
		assertTrue(loginResponse.isLoggedIn());

		CheckSessionResponse checkResponse = server.checkSession();
		assertNotNull(checkResponse);
		assertTrue(checkResponse.isSessionActive());

		DeleteSessionResponse response = server.deleteSession();
		assertNotNull(response);
		assertTrue(response.isSessionDeleted());
	}

	@Test
	public void testRegisterEventListeners() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		server.registerEventListeners();
	}

	@Test
	public void testDisconnected() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final ConnectedResponse response = server.isConnected();
		assertNotNull(response);
		assertFalse(response.isConnected());
	}

	@Test
	public void testGetHosts() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final HostResponse response = server.getHosts();
		assertNotNull(response);
		assertEquals(Integer.valueOf(1), Integer.valueOf(response.getHosts().size()));
		assertEquals("localhost", response.getHosts().get(0).getHostname());
	}

	@Test
	public void testGetHostStatus() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final HostResponse response = server.getHostStatus(DELUGE_HOST_HASH);
		assertNotNull(response);
		assertEquals(Integer.valueOf(1), Integer.valueOf(response.getHosts().size()));
		assertEquals("localhost", response.getHosts().get(0).getHostname());
		assertNotNull(response.getHosts().get(0).getVersion());
	}

	@Test
	public void testConnect() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final HostResponse response = server.getHostStatus(DELUGE_HOST_HASH);
		final ConnectedResponse connResponse = server.connect(response.getHosts().get(0).getId());
		assertTrue(connResponse.isConnected());
	}

	@Test
	public void testGetEvents() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final HostResponse response = server.getHostStatus(DELUGE_HOST_HASH);
		final ConnectedResponse connResponse = server.connect(response.getHosts().get(0).getId());
		assertTrue(connResponse.isConnected());
		server.getEvents();
	}

	@Test
	public void testUpdateUI() throws Exception {
		final DelugeServer server = new DelugeServer(DELUGE_JSON_URL, DELUGE_PASSWORD);
		server.login();
		final HostResponse response = server.getHostStatus(DELUGE_HOST_HASH);
		final ConnectedResponse connResponse = server.connect(response.getHosts().get(0).getId());
		assertTrue(connResponse.isConnected());
		final UIResponse uiResponse = server.updateUI();
		assertTrue(uiResponse.isConnected());
		assertTrue(uiResponse.getTorrents().size() > 0);
	}

}
