package dev.roanh.wiki;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Simple GitHub API wrapper.
 * @author Roan
 */
public final class GitHub{
	/**
	 * HTTP client used to make API requests.
	 */
	private static final HttpClient client = HttpClient.newHttpClient();
	/**
	 * The GSON instance to use to deserialise response payloads.
	 */
	private static final Gson gson = new Gson();
	
	/**
	 * Prevent instantiation.
	 */
	private GitHub(){
	}

	/**
	 * Attempts to find an open PR for the given commit.
	 * @param namespace The namespace the commit belongs to.
	 * @param sha The hash of the commit to find.
	 * @return An open PR with the given commit.
	 * @throws GitHubException When some GitHub API exception occurs.
	 */
	public static final Optional<PullRequestInfo> getPullRequestForCommit(String namespace, String sha) throws GitHubException{
		try{
			return Arrays.stream(
				gson.fromJson(executeGet("https://api.github.com/repos/" + namespace + "/osu-wiki/commits/" + sha + "/pulls"), PullRequestInfo[].class)
			).filter(PullRequestInfo::isOfficial).findFirst();
		}catch(JsonSyntaxException | URISyntaxException | IOException | InterruptedException ignore){
			throw new GitHubException(ignore);
		}
	}
	
	/**
	 * Executes a HTTP GET request against the given endpoint.
	 * @param url The URL to request.
	 * @return The response JSON payload.
	 * @throws URISyntaxException When the given URL is invalid.
	 * @throws IOException When an IOException occurs.
	 * @throws InterruptedException When the current thread is interrupted.
	 */
	private static final String executeGet(String url) throws URISyntaxException, IOException, InterruptedException{
		HttpResponse<String> response = client.send(
			HttpRequest.newBuilder(new URI(url)).GET().header("Accept", "application/vnd.github.v3+json").build(),
			BodyHandlers.ofString()
		);
		
		if(response.statusCode() != 200){
			throw new IOException("Received status " + response.statusCode() + " from GitHub.");
		}
		
		return response.body();
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
		 * Gets the complete web url for this PR assuming it is in the official repository.
		 * @return The PR web url.
		 * @see #isOfficial()
		 */
		public String getUrl(){
			return "https://github.com/ppy/osu-wiki/pull/" + number;
		}
		
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
	
	/**
	 * Exception thrown for GitHub API issues.
	 * @author Roan
	 */
	public static final class GitHubException extends Exception{
		/**
		 * Serial ID.
		 */
		private static final long serialVersionUID = 1202212041452857347L;
		
		/**
		 * Constructs a new GitHub exception with the given cause.
		 * @param cause The root cause.
		 */
		public GitHubException(Exception cause){
			super(cause);
		}
	}
}