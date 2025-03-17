/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.roanh.wiki.github;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.roanh.wiki.exception.GitHubException;

/**
 * Simple GitHub API wrapper.
 * @author Roan
 */
public final class GitHub{
	/**
	 * Algorithm used by GitHub to sign webhook events.
	 */
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	/**
	 * HTTP client used to make API requests.
	 */
	private static final HttpClient client = HttpClient.newHttpClient();
	/**
	 * The GSON instance to use to deserialise response payloads.
	 */
	private static final Gson gson = new Gson();
	/**
	 * The instance of the GitHub API.
	 */
	private static final GitHub instance = new GitHub();
	/**
	 * The base url for the GitHub API.
	 */
	private final String baseUrl;
	
	/**
	 * Constructs a new GitHub API instance.
	 */
	private GitHub(){
		this("https://api.github.com/");
	}
	
	/**
	 * Constructs a new GitHub API instance.
	 * @param baseUrl The GitHub API base endpoint.
	 */
	public GitHub(String baseUrl){
		this.baseUrl = baseUrl;
	}
	
	/**
	 * Gets the instance of the GitHub API.
	 * @return The GitHub API instance.
	 */
	public static final GitHub instance(){
		return instance;
	}

	/**
	 * Attempts to find an open PR for the given commit.
	 * @param namespace The namespace the commit belongs to.
	 * @param sha The hash of the commit to find.
	 * @return An open PR with the given commit.
	 * @throws GitHubException When some GitHub API exception occurs.
	 */
	public final Optional<PullRequestInfo> getPullRequestForCommit(String namespace, String sha) throws GitHubException{
		try{
			return Arrays.stream(
				gson.fromJson(executeGet("repos/" + namespace + "/osu-wiki/commits/" + sha + "/pulls"), PullRequestInfo[].class)
			).filter(PullRequestInfo::isOfficial).findFirst();
		}catch(InterruptedException ignore){
			Thread.currentThread().interrupt();
			throw new GitHubException(ignore);
		}catch(JsonSyntaxException | URISyntaxException | IOException ignore){
			throw new GitHubException(ignore);
		}
	}
	
	/**
	 * Executes a HTTP GET request against the given endpoint.
	 * @param path The endpoint to request.
	 * @return The response JSON payload.
	 * @throws URISyntaxException When the given URL is invalid.
	 * @throws IOException When an IOException occurs.
	 * @throws InterruptedException When the current thread is interrupted.
	 */
	private final String executeGet(String path) throws URISyntaxException, IOException, InterruptedException{
		HttpResponse<String> response = client.send(
			HttpRequest.newBuilder(new URI(baseUrl + path)).GET().header("Accept", "application/vnd.github.v3+json").build(),
			BodyHandlers.ofString()
		);
		
		if(response.statusCode() != 200){
			throw new IOException("Received status " + response.statusCode() + " from GitHub.");
		}
		
		return response.body();
	}
	
	public static final Key createSigningKey(String secret){
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}
	
	public static final byte[] sign(Key secret, String payload) throws NoSuchAlgorithmException, InvalidKeyException{
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(secret);
		return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
	}
	
	public static final boolean validateSignature(Key secret, String signature, String payload){
		try{
			byte[] hexSignature = sign(secret, payload);
			
			int diff = 0;
			for(int i = 0; i < hexSignature.length; i++){
				diff |= ((HexFormat.fromHexDigit(signature.charAt(i * 2)) << 4) | HexFormat.fromHexDigit(signature.charAt(i * 2 + 1))) ^ (hexSignature[i] & 0xFF);
			}
			
			return diff == 0;
		}catch(NoSuchAlgorithmException | InvalidKeyException ignore){
			return false;
		}
	}
	
	/**
	 * Record with information about a PR.
	 * @author Roan
	 * @param id The GitHub internal pull request ID.
	 * @param number The PR number as shown in the web UI.
	 * @param base The base ref for the PR.
	 */
	public static final record PullRequestInfo(int id, int number, BaseRef base){

		/**
		 * Checks if this PR is on the official ppy repository.
		 * @return True if this PR is on the official wiki repository.
		 */
		public boolean isOfficial(){
			return base.isOfficial();
		}
	}

	/**
	 * PR base reference record.
	 * @author Roan
	 * @param label The namespace:ref label for this reference.
	 */
	public static final record BaseRef(String label){
		
		/**
		 * Checks if this PR is on the official ppy repository.
		 * @return True if this PR is on the official wiki repository.
		 */
		public boolean isOfficial(){
			return label.startsWith("ppy:");
		}
	}
}
