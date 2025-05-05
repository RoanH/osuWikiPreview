package dev.roanh.wiki.github.obj;

import com.google.gson.annotations.SerializedName;

public record GitHubUser(
		String login,
		int id,
		@SerializedName("avatar_url")
		String avatarUrl,
		UserType type
	){
}
