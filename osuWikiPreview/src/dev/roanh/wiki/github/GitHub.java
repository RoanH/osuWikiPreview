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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dev.roanh.wiki.Main;
import dev.roanh.wiki.exception.GitHubException;
import dev.roanh.wiki.exception.GitHubUserNotFoundException;
import dev.roanh.wiki.github.obj.GitHubPullRequest;
import dev.roanh.wiki.github.obj.IssueState;
import dev.roanh.wiki.github.obj.UserType;

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
	private static final Gson gson;
	/**
	 * The instance of the GitHub API.
	 */
	private static final GitHub instance = new GitHub(Main.config.getGitHubToken());
	/**
	 * The base url for the GitHub API.
	 */
	private final String baseUrl;
	/**
	 * The GitHub API access token.
	 */
	private final String token;
	
	/**
	 * Constructs a new GitHub API instance.
	 * @param token The GitHub API token.
	 */
	private GitHub(String token){
		this("https://api.github.com/", token);
	}
	
	/**
	 * Constructs a new GitHub API instance.
	 * @param baseUrl The GitHub API base endpoint.
	 * @param token The GitHub API token.
	 */
	public GitHub(String baseUrl, String token){
		this.baseUrl = baseUrl;
		this.token = token;
	}
	
	/**
	 * Gets the instance of the GitHub API.
	 * @return The GitHub API instance.
	 */
	public static final GitHub instance(){
		return instance;
	}
	
	public static void main(String[] args) throws GitHubException{
		System.out.println(instance.getWikiFork("ppy"));
	}

	/**
	 * Attempts to find the name of the osu! wiki fork for the given GitHub user.
	 * @param user The GitHub user to find the osu! wiki fork for.
	 * @return The name of the osu! wiki fork for the given user if found.
	 * @throws GitHubException When some GitHub API exception occurs.
	 * @throws GitHubUserNotFoundException When the given user does not exist (or is an organisation).
	 */
	public final Optional<String> getWikiFork(String user) throws GitHubException, GitHubUserNotFoundException{
		try{
			JsonObject response = gson.fromJson(
				executeGraphQL(
					"""
					query{
					  user(login: "%s"){
					    repositories(
					      first: 100,
					      isFork: true,
					      ownerAffiliations: OWNER,
					      orderBy: {
					        field: PUSHED_AT,
					        direction: DESC
					      }
					    ){
					      nodes{
					        name,
					        parent{
					          nameWithOwner
					        }
					      }
					    }
					  }
					}
					""".formatted(user.replace("\"", "\\\""))
				),
				JsonObject.class
			);
			
			JsonElement userData = response.getAsJsonObject("data").get("user");
			if(userData.isJsonNull()){
				throw new GitHubUserNotFoundException(user);
			}
			
			for(JsonElement item : userData.getAsJsonObject().getAsJsonObject("repositories").getAsJsonArray("nodes").asList()){
				JsonObject obj = item.getAsJsonObject();
				if(obj.getAsJsonObject("parent").get("nameWithOwner").getAsString().equals("ppy/osu-wiki")){
					return Optional.of(obj.get("name").getAsString());
				}
			}

			return Optional.empty();
		}catch(InterruptedException ignore){
			Thread.currentThread().interrupt();
			throw new GitHubException(ignore);
		}catch(JsonSyntaxException | URISyntaxException | IOException ignore){
			throw new GitHubException(ignore);
		}
	}

	/**
	 * Attempts to find an open PR for the given commit.
	 * @param namespace The namespace the commit belongs to.
	 * @param sha The hash of the commit to find.
	 * @return An open PR with the given commit.
	 * @throws GitHubException When some GitHub API exception occurs.
	 */
	public final Optional<GitHubPullRequest> getPullRequestForCommit(String namespace, String sha) throws GitHubException{
		try{
			return Arrays.stream(
				gson.fromJson(executeGet("repos/" + namespace + "/osu-wiki/commits/" + sha + "/pulls"), GitHubPullRequest[].class)
			).filter(GitHubPullRequest::isOnOfficialRepository).findFirst();
		}catch(InterruptedException ignore){
			Thread.currentThread().interrupt();
			throw new GitHubException(ignore);
		}catch(JsonSyntaxException | URISyntaxException | IOException ignore){
			throw new GitHubException(ignore);
		}
	}
	
	/**
	 * Executes a GitHub GraphQL API query.
	 * @param query The GraphQL query to execute.
	 * @return The JSON result of the query.
	 * @throws URISyntaxException When the given URL is invalid.
	 * @throws IOException When an IOException occurs.
	 * @throws InterruptedException When the current thread is interrupted.
	 */
	private final String executeGraphQL(String query) throws IOException, InterruptedException, URISyntaxException{
		JsonObject obj = new JsonObject();
		obj.addProperty("query", query);
		return executePost("graphql", obj.toString());
	}
	
	/**
	 * Executes a HTTP POST request against the given endpoint.
	 * @param path The endpoint to request.
	 * @param body The JSON payload.
	 * @return The JSON response payload.
	 * @throws URISyntaxException When the given URL is invalid.
	 * @throws IOException When an IOException occurs.
	 * @throws InterruptedException When the current thread is interrupted.
	 */
	private final String executePost(String path, String body) throws IOException, InterruptedException, URISyntaxException{
		Builder request = HttpRequest.newBuilder(new URI(baseUrl + path));
		request.POST(BodyPublishers.ofString(body));
		request.header("Accept", "application/vnd.github.v3+json");
		request.header("Authorization", "bearer " + token);
		
		HttpResponse<String> response = client.send(
			request.build(),
			BodyHandlers.ofString()
		);
		
		if(response.statusCode() != 200){
			throw new IOException("Received status " + response.statusCode() + " from GitHub.");
		}
		
		return response.body();
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
	
	/**
	 * Creates a new secret key based on the given secret string.
	 * @param secret The secret string.
	 * @return A signing key derived from the given secret string.
	 */
	public static final Key createSigningKey(String secret){
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}
	
	/**
	 * Signs the given payload with the given key.
	 * @param secret The key to sign with.
	 * @param payload The payload to sign.
	 * @return The signature that was created.
	 * @throws NoSuchAlgorithmException When HMAC does not exist.
	 * @throws InvalidKeyException When the given key is invalid.
	 */
	public static final byte[] sign(Key secret, String payload) throws NoSuchAlgorithmException, InvalidKeyException{
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(secret);
		return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Validates that the given signature on the given payload was created by the given key.
	 * @param secret The key used to create the signature.
	 * @param signature The signature that was created.
	 * @param payload The payload that was signed.
	 * @return True if the given signature is valid.
	 */
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
	 * Gets the gson instance used to deserialise GitHub payloads.
	 * @return The gson instance to use for serialisation.
	 */
	protected static final Gson getGson(){
		return gson;
	}

	static{
		GsonBuilder builder = new GsonBuilder();
		
		builder.registerTypeAdapter(Instant.class, new InstantDeserializer());
		
		EnumDeserializer enums = new EnumDeserializer();
		builder.registerTypeAdapter(IssueState.class, enums);
		builder.registerTypeAdapter(UserType.class, enums);
		
		gson = builder.create();
	}
}
