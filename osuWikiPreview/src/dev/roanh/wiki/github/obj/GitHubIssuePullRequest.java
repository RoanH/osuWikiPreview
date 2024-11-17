package dev.roanh.wiki.github.obj;

import java.time.Instant;

import com.google.gson.annotations.SerializedName;

public record GitHubIssuePullRequest(
		@SerializedName("merged_at")
		Instant mergedInstant
	){
}
