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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import dev.roanh.wiki.exception.GitHubException;
import dev.roanh.wiki.github.obj.GitHubBranch;
import dev.roanh.wiki.github.obj.GitHubPullRequest;
import dev.roanh.wiki.github.obj.GitHubRepository;
import dev.roanh.wiki.github.obj.GitHubUser;
import dev.roanh.wiki.github.obj.IssueState;
import dev.roanh.wiki.github.obj.UserType;

@WireMockTest(httpPort = 12345)
public class GitHubTest{
	private static final GitHub github = new GitHub("http://localhost:12345/");
	
	@BeforeEach
	public void setup() throws IOException{
		WireMock.stubFor(WireMock.any(WireMock.anyUrl()).atPriority(10).willReturn(WireMock.serverError()));
		WireMock.stubFor(WireMock.get("/repos/itsmehoaq/osu-wiki/commits/1ea83f97f8fa13a679417e9687b1305215199005/pulls").willReturn(readJson("pr_for_commit")));
	}
	
	@Test
	public void getPullRequestForCommit() throws GitHubException{
		Optional<GitHubPullRequest> found = github.getPullRequestForCommit("itsmehoaq", "1ea83f97f8fa13a679417e9687b1305215199005");
		assertTrue(found.isPresent());
		
		GitHubPullRequest pr = found.get();
		assertEquals(2106313984, pr.id());
		assertEquals(12265, pr.number());
		assertEquals(IssueState.CLOSED, pr.state());
		assertEquals("Add `Resurrection Cup 2024 Concludes` news post", pr.title());
		assertEquals("[web preview](https://osu1.roanh.dev/home/news/2024-11-04-resurrection-cup-2024-results)", pr.body());
		assertTrue(pr.isOnOfficialRepository());
		
		GitHubUser user = pr.user();
		assertNotNull(user);
		assertEquals("itsmehoaq", user.login());
		assertEquals(69494393, user.id());
		assertEquals("https://avatars.githubusercontent.com/u/69494393?v=4", user.avatarUrl());
		assertEquals(UserType.USER, user.type());
		
		GitHubBranch head = pr.head();
		assertNotNull(head);
		assertEquals("itsmehoaq:resurrection-cup-2024", head.label());
		assertEquals("resurrection-cup-2024", head.ref());
		
		GitHubRepository headRepo = head.repo();
		assertNotNull(headRepo);
		assertEquals("osu-wiki", headRepo.name());
		assertFalse(headRepo.isOfficial());
		
		GitHubUser headRepoOwner = headRepo.owner();
		assertNotNull(headRepoOwner);
		assertEquals("itsmehoaq", headRepoOwner.login());
		assertEquals(69494393, headRepoOwner.id());
		assertEquals("https://avatars.githubusercontent.com/u/69494393?v=4", headRepoOwner.avatarUrl());
		assertEquals(UserType.USER, headRepoOwner.type());
		
		GitHubBranch base = pr.base();
		assertNotNull(base);
		assertEquals("ppy:master", base.label());
		assertEquals("master", base.ref());
		
		GitHubRepository baseRepo = base.repo();
		assertNotNull(baseRepo);
		assertEquals("osu-wiki", baseRepo.name());
		assertTrue(baseRepo.isOfficial());
		
		GitHubUser baseRepoOwner = baseRepo.owner();
		assertNotNull(baseRepoOwner);
		assertEquals("ppy", baseRepoOwner.login());
		assertEquals(995763, baseRepoOwner.id());
		assertEquals("https://avatars.githubusercontent.com/u/995763?v=4", baseRepoOwner.avatarUrl());
		assertEquals(UserType.ORGANIZATION, baseRepoOwner.type());
	}
	
	@Test
	public void signatureValidationValid(){
		assertTrue(GitHub.validateSignature(
				GitHub.createSigningKey("It's a Secret to Everybody"),
				"757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17",
				"Hello, World!"
			)
		);
	}
	
	@Test
	public void signatureValidationInvalid(){
		assertFalse(GitHub.validateSignature(
				GitHub.createSigningKey("It's not a Secret to Everybody"),
				"757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17",
				"Hello, World!"
			)
		);
	}
	
	private static ResponseDefinitionBuilder readJson(String name) throws IOException{
		try(InputStream in = ClassLoader.getSystemResourceAsStream("resources/github/" + name + ".json")){
			return WireMock.okJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}
}
