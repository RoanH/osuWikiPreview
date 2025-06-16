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

		GitHubUser commentUser = comment.user();
		assertNotNull(commentUser);
		assertEquals("RoanH", commentUser.login());
		assertEquals(8530896, commentUser.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", commentUser.avatarUrl());
		assertEquals(UserType.USER, commentUser.type());

		GitHubIssue issue = data.issue();
		assertNotNull(issue);
		assertEquals(2664436632L, issue.id());
		assertEquals(2, issue.number());
		assertEquals(IssueState.OPEN, issue.state());
		assertEquals("Preview Test", issue.title());
		assertTrue(issue.isPullRequest());

		GitHubIssuePullRequest pr = issue.pullRequest();
		assertNotNull(pr);
		assertNull(pr.mergedInstant());
		
		GitHubUser issueUser = comment.user();
		assertNotNull(issueUser);
		assertEquals("RoanH", issueUser.login());
		assertEquals(8530896, issueUser.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", issueUser.avatarUrl());
		assertEquals(UserType.USER, issueUser.type());
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
		
		GitHubUser user = pr.user();
		assertNotNull(user);
		assertEquals("RoanH", user.login());
		assertEquals(8530896, user.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", user.avatarUrl());
		assertEquals(UserType.USER, user.type());
		
		GitHubBranch head = pr.head();
		assertNotNull(head);
		assertEquals("RoanH:preview2", head.label());
		assertEquals("preview2", head.ref());
		
		GitHubRepository repo = head.repo();
		assertNotNull(repo);
		assertEquals("osu-wiki", repo.name());
		
		GitHubUser owner = repo.owner();
		assertNotNull(owner);
		assertEquals("RoanH", owner.login());
		assertEquals(8530896, owner.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", owner.avatarUrl());
		assertEquals(UserType.USER, owner.type());
	}
	
	//TODO PR commit
	
	//TODO PR merged
}
