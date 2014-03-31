package me.desht.scrollingmenusign;

import com.google.common.collect.ImmutableList;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class UUIDFetcher implements Callable<Map<String, UUID>> {
	private static final Gson GSON = new Gson();
	private static final int MAX_SEARCH = 100;
	private static final String PROFILE_URL = "https://api.mojang.com/profiles/page/";
	private static final String AGENT = "minecraft";
	private final List<String> names;

	public UUIDFetcher(List<String> names) {
		this.names = ImmutableList.copyOf(names);
	}

	public Map<String, UUID> call() throws Exception {
		Map<String, UUID> uuidMap = new HashMap<String, UUID>();
		String body = buildBody(names);
		for (int i = 1; i < MAX_SEARCH; i++) {
			HttpURLConnection connection = createConnection(i);
			writeBody(connection, body);
			ProfileArray response = GSON.fromJson(new InputStreamReader(connection.getInputStream()), ProfileArray.class);
			if (response.size == 0) {
				break;
			}
			for (Profile profile : response.profiles) {
				UUID uuid = UUID.fromString(profile.id.substring(0, 8) + "-" + profile.id.substring(8, 12) + "-" + profile.id.substring(12, 16) + "-" + profile.id.substring(16, 20) + "-" + profile.id.substring(20, 32));
				uuidMap.put(profile.name, uuid);
			}
		}
		return uuidMap;
	}

	private static void writeBody(HttpURLConnection connection, String body) throws Exception {
		DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(body.getBytes());
		writer.flush();
		writer.close();
	}

	private static HttpURLConnection createConnection(int page) throws Exception {
		URL url = new URL(PROFILE_URL+page);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}
	private static String buildBody(List<String> names) {
		List<ProfileLookup> lookups = new ArrayList<ProfileLookup>();
		for (String name : names) {
			ProfileLookup lookup = new ProfileLookup(name, AGENT);
			lookups.add(lookup);
		}
		return GSON.toJson(lookups.toArray());
	}
	private static class ProfileLookup {
		private String name;
		private String agent;
		public ProfileLookup(String name, String agent) {
			this.name = name;
			this.agent = agent;
		}
	}
	private static class Profile {
		private String id;
		private String name;
	}
	private static class ProfileArray {
		private int size;
		private Profile[] profiles;
	}
}