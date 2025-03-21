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
package dev.roanh.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import dev.roanh.wiki.GitHub.GitHubException;
import dev.roanh.wiki.GitHub.PullRequestInfo;

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
		Optional<PullRequestInfo> found = github.getPullRequestForCommit("itsmehoaq", "1ea83f97f8fa13a679417e9687b1305215199005");
		assertTrue(found.isPresent());
		
		PullRequestInfo pr = found.get();
		assertEquals(2106313984, pr.id());
		assertEquals(12265, pr.number());
		assertTrue(pr.isOfficial());
	}
	
	private static ResponseDefinitionBuilder readJson(String name) throws IOException{
		try(InputStream in = ClassLoader.getSystemResourceAsStream("resources/github/" + name + ".json")){
			return WireMock.okJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}
}
