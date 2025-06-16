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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import dev.roanh.infinity.util.Variable;
import dev.roanh.wiki.github.hooks.IssueCommentCreatedData;
import dev.roanh.wiki.github.hooks.PullRequestOpenData;
import dev.roanh.wiki.github.hooks.PullRequestSyncData;
import dev.roanh.wiki.github.obj.GitHubBranch;
import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;
import dev.roanh.wiki.github.obj.GitHubIssuePullRequest;
import dev.roanh.wiki.github.obj.GitHubPullRequest;
import dev.roanh.wiki.github.obj.GitHubRepository;
import dev.roanh.wiki.github.obj.GitHubUser;
import dev.roanh.wiki.github.obj.IssueState;
import dev.roanh.wiki.github.obj.UserType;

public class WebHookHandlerTest extends WebhookTest{

	@Test
	public void commentEvent() throws Exception{
		Variable<IssueCommentCreatedData> value = new Variable<IssueCommentCreatedData>();
		CountDownLatch latch = new CountDownLatch(1);

		webhook.addIssueCommentHandler(data->{
			value.setValue(data);
			latch.countDown();
		});

		sendPullRequestCommentPayload();
		latch.await(10, TimeUnit.SECONDS);

		IssueCommentCreatedData data = value.getValue();
		assertNotNull(data);

		GitHubComment comment = data.comment();
		assertNotNull(comment);
		assertEquals(2480604277L, comment.id());
		assertEquals("Test 3", comment.body());
		assertRoan(comment.user());

		GitHubIssue issue = data.issue();
		assertNotNull(issue);
		assertEquals(2664436632L, issue.id());
		assertEquals(2, issue.number());
		assertEquals(IssueState.OPEN, issue.state());
		assertEquals("Preview Test", issue.title());
		assertTrue(issue.isPullRequest());
		assertRoan(issue.user());

		GitHubIssuePullRequest pr = issue.pullRequest();
		assertNotNull(pr);
		assertNull(pr.mergedInstant());
	}
	
	@Test
	public void pullRequestOpenEvent() throws Exception{
		Variable<PullRequestOpenData> value = new Variable<PullRequestOpenData>();
		CountDownLatch latch = new CountDownLatch(1);
		
		webhook.addPullRequestCreatedHandler(data->{
			value.setValue(data);
			latch.countDown();
		});

		sendPullRequestOpenPayload();
		latch.await(10, TimeUnit.SECONDS);
		
		PullRequestOpenData data = value.getValue();
		assertNotNull(data);
		
		GitHubPullRequest pr = data.pullRequest();
		assertNotNull(pr);
		assertEquals(2496856356L, pr.id());
		assertEquals(3, pr.number());
		assertEquals(IssueState.OPEN, pr.state());
		assertEquals("Preview Test 2", pr.title());
		assertEquals("Test", pr.body());
		assertRoan(pr.user());
		assertMaster(pr.base());
		
		GitHubBranch head = pr.head();
		assertNotNull(head);
		assertEquals("RoanH:preview2", head.label());
		assertEquals("preview2", head.ref());
		assertOwnRepo(head.repo());
	}
	
	@Test
	public void pullRequestSyncEvent() throws Exception{
		Variable<PullRequestSyncData> value = new Variable<PullRequestSyncData>();
		CountDownLatch latch = new CountDownLatch(1);
		
		webhook.addPullRequestCommitHandler(data->{
			value.setValue(data);
			latch.countDown();
		});

		sendPullRequestCommitPayload();
		latch.await(10, TimeUnit.SECONDS);
		
		PullRequestSyncData data = value.getValue();
		assertNotNull(data);
		assertEquals("57771b7601b0bdc36cb19f74dbac2dc465e4d5af", data.before());
		assertEquals("8cf0ed64f36bbc283ad4bfdefbc50472836f5c10", data.after());
		
		GitHubPullRequest pr = data.pullRequest();
		assertNotNull(pr);
		assertEquals(2183222396L, pr.id());
		assertEquals(2, pr.number());
		assertEquals(IssueState.OPEN, pr.state());
		assertEquals("Preview Test", pr.title());
		assertEquals("Test 1", pr.body());
		assertRoan(pr.user());
		assertMaster(pr.base());
		
		GitHubBranch head = pr.head();
		assertNotNull(head);
		assertEquals("RoanH:preview", head.label());
		assertEquals("preview", head.ref());
		assertOwnRepo(head.repo());
	}
	
	private static void assertMaster(GitHubBranch branch){
		assertNotNull(branch);
		assertEquals("RoanH:master", branch.label());
		assertEquals("master", branch.ref());
		assertOwnRepo(branch.repo());
	}
	
	private static void assertOwnRepo(GitHubRepository repo){
		assertNotNull(repo);
		assertEquals("osu-wiki", repo.name());
		assertRoan(repo.owner());
	}
	
	private static void assertRoan(GitHubUser user){
		assertNotNull(user);
		assertEquals("RoanH", user.login());
		assertEquals(8530896, user.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", user.avatarUrl());
		assertEquals(UserType.USER, user.type());
	}
}
