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

public class GitHub{
	private static final HttpClient client = HttpClient.newHttpClient();
	private static final Gson gson = new Gson();
	
	public static final Optional<PullRequestInfo> getPullRequestsForCommit(String namespace, String sha) throws GitHubException{
		try{
			return Arrays.stream(
				gson.fromJson(executeGet("https://api.github.com/repos/" + namespace + "/osu-wiki/commits/" + sha + "/pulls"), PullRequestInfo[].class)
			).filter(PullRequestInfo::isOfficial).findFirst();
		}catch(JsonSyntaxException | URISyntaxException | IOException | InterruptedException ignore){
			throw new GitHubException(ignore);
		}
	}
	
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
	
	public static final record PullRequestInfo(int id, int number, BaseRef base){
		
		public String getUrl(){
			return "https://github.com/ppy/osu-wiki/pull/" + number;
		}
		
		public boolean isOfficial(){
			return base.isOfficial();
		}
	}
	
	public static final record BaseRef(String label){
		
		public boolean isOfficial(){
			return label.startsWith("ppy:");
		}
	}
	
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
