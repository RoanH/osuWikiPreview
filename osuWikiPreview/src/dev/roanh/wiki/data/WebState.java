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
package dev.roanh.wiki.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.PullRequest;
import dev.roanh.wiki.github.GitHub.PullRequestInfo;

/**
 * Object containing information on the current state of an osu! web
 * instance. This contains both information on the currently checked
 * out wiki reference as well as information on any other changes.
 * @author Roan
 */
public class WebState{
	/**
	 * The namespace (user or organisation) the checked
	 * out reference is under.
	 */
	private final String namespace;
	/**
	 * The reference (branch/sha/tag) that is currently checked out.
	 */
	private final String ref;
	/**
	 * Whether news posts were redated.
	 */
	private boolean redate;
	/**
	 * Whether ppy/master was merged into the ref beforehand.
	 */
	private boolean master;
	/**
	 * The pull request associated with the current ref if any.
	 */
	private PullRequest pr;
	/**
	 * The instant the web instance becomes available for automatic claiming.
	 */
	private Instant available;
	//TODO Optional<User> user
	
	/**
	 * Constructs a new web state from the given result set. 
	 * @param rs The result set to read from.
	 * @throws SQLException When an SQL exception occurs.
	 */
	protected WebState(ResultSet rs) throws SQLException{
		long prId = rs.getLong("pr_id"); 
		pr = prId == -1L ? null : new PullRequest(prId, rs.getInt("pr_num"));
		namespace = rs.getString("namespace");
		ref = rs.getString("ref");
		redate = rs.getBoolean("redate");
		master = rs.getBoolean("master");
		available = Instant.ofEpochSecond(rs.getLong("available"));
	}
	
	/**
	 * Constructs a new web state.
	 * @param namespace The namespace for the new state.
	 * @param ref The ref for the new state (commit/branch/tag).
	 * @param redate True to redate newsposts.
	 * @param master True to merge ppy/master into the preview.
	 */
	private WebState(String namespace, String ref, boolean redate, boolean master){
		this.namespace = namespace;
		this.ref = ref;
		this.redate = redate;
		this.master = master;
		pr = null;
		available = Instant.now();
	}

	/**
	 * Gets the link to the GitHub tree associated with this state.
	 * @return The GitHub page for the namespace and reference.
	 */
	public String getGitHubTree(){
		return "https://github.com/" + namespace + "/osu-wiki/tree/" + ref;
	}
	
	/**
	 * Gets the namespace for the current web state.
	 * @return The namespace (users / organisation).
	 */
	public String getNamespace(){
		return namespace;
	}
	
	/**
	 * Gets the combination of the namespace and ref.
	 * @return The namespace and ref for this state.
	 * @see #getNamespace()
	 * @see #getRef()
	 */
	public String getNamespaceWithRef(){
		return namespace + "/" + ref;
	}
	
	/**
	 * Checks if news redating is enabled for the preview.
	 * @return True if news is redated.
	 */
	public boolean hasRedate(){
		return redate;
	}
	
	/**
	 * Checks if ppy/master is merged into the preview.
	 * @return True if master is merged into the preview.
	 */
	public boolean hasMaster(){
		return master;
	}
	
	/**
	 * Gets the ref shown on the preview.
	 * @return The ref being previewed.
	 */
	public String getRef(){
		return ref;
	}
	
	/**
	 * Checks if the current ref as an associated pull request
	 * on the official osu! wiki repository.
	 * @return True if a pull request exists.
	 */
	public boolean hasPullRequest(){
		return pr != null;
	}
	
	/**
	 * Gets the pull request for the current ref if any.
	 * @return True pull request for the current ref.
	 * @see #hasPullRequest()
	 */
	public Optional<PullRequest> getPullRequest(){
		return Optional.ofNullable(pr);
	}
	
	/**
	 * Sets the pull request for the current ref.
	 * @param pr The pull request for the current ref.
	 */
	public void setPullRequest(PullRequest pr){
		this.pr = pr;
	}
	
	/**
	 * Sets the pull request for the current ref.
	 * @param pr The pull request for the current ref.
	 */
	public void setPullRequest(PullRequestInfo pr){
		setPullRequest(new PullRequest(pr.id(), pr.number()));
	}
	
	/**
	 * Checks when this preview instance becomes available for automatic claiming.
	 * @return The instant this preview instance becomes available.
	 */
	public Instant getAvailableAt(){
		return available;
	}
	
	/**
	 * Locks this instance for at least the given duration.
	 * @param duration The duration to claim this instance for.
	 * @see #getAvailableAt()
	 */
	public void refreshClaim(Duration duration){
		available = Instant.now().plus(duration);
	}
	
	/**
	 * Returns a new web state with the redate flag set to true.
	 * @return This web state with the redate flag set to true.
	 */
	public WebState withRedate(){
		redate = true;
		return this;
	}

	/**
	 * Returns a new web state with the master flag set to true.
	 * @return This web state with the master flag set to true.
	 */
	public WebState withMaster(){
		master = true;
		return this;
	}
	
	/**
	 * Checks if this is an internal branch used by the preview system itself.
	 * @return True if this is an internal branch.
	 */
	public boolean isInternalBranch(){
		return namespace.equals("RoanH") && ref.startsWith("wikisync-");
	}
	
	/**
	 * Constructs a new web state for showing manually uploaded newspost preview.
	 * @param web The instance the preview will be on.
	 * @return The constructed web state.
	 */
	public static final WebState forNewspostPreview(OsuWeb web){
		return new WebState("RoanH", web.getWikiSyncBranch(), true, false);
	}
	
	/**
	 * Constructs a new web state for showing the given ref.
	 * @param namespace The namespace for the ref.
	 * @param ref The ref to preview.
	 * @param redate True to redate any news.
	 * @param master True to merge ppy/master into the preview.
	 * @return The constructed web state.
	 */
	public static final WebState forRef(String namespace, String ref, boolean redate, boolean master){
		return new WebState(namespace, ref, redate, master);
	}
}
