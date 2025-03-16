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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import dev.roanh.wiki.GitHub.PullRequestInfo;
import dev.roanh.wiki.cmd.BaseSwitchCommand;

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
	private String namespace;
	/**
	 * The reference (branch/sha/tag) that is currently checked out.
	 */
	private String ref;
	/**
	 * Whether news posts were redated.
	 */
	private boolean redate;
	/**
	 * Whether ppy/master was merged into the ref beforehand.
	 */
	private boolean master;
	private PullRequest pr;
	private Instant available;
	
	public WebState(ResultSet rs) throws SQLException{
		long prId = rs.getLong("pr_id"); 
		pr = prId == -1L ? null : new PullRequest(prId, rs.getInt("pr_num"));
		namespace = rs.getString("namespace");
		ref = rs.getString("ref");
		redate = rs.getBoolean("redate");
		master = rs.getBoolean("master");
		available = Instant.ofEpochSecond(rs.getLong("available"));
	}
	
	private WebState(String namespace, String ref, boolean redate, boolean master){
		this.namespace = namespace;
		this.ref = ref;
		this.redate = redate;
		this.master = master;
		pr = null;
		available = Instant.now().plus(BaseSwitchCommand.DEFAULT_CLAIM_TIME);
	}

	/**
	 * Gets the link to the GitHub tree associated with this state.
	 * @return The GitHub page for the namespace and reference.
	 */
	public String getGitHubTree(){
		return "https://github.com/" + namespace + "/osu-wiki/tree/" + ref;
	}
	
	public String getNamespace(){
		return namespace;
	}
	
	public String getNamespaceWithRef(){
		return namespace + "/" + ref;
	}
	
	public boolean hasRedate(){
		return redate;
	}
	
	public boolean hasMaster(){
		return master;
	}
	
	public String getRef(){
		return ref;
	}
	
	public boolean hasPR(){
		return pr != null;
	}
	
	public Optional<PullRequest> getPullRequest(){
		return Optional.ofNullable(pr);
	}
	
	public void setPullRequest(PullRequest pr){
		
	}
	
	public void setPullRequest(PullRequestInfo pr){
		setPullRequest(new PullRequest(pr.id(), pr.number()));
	}
	
	public Instant getAvailableAt(){
		return available;
	}
	
	/**
	 * Returns a new web state with the redate flag set to true.
	 * @return A copy of this web state with the redate flag set to true.
	 */
	public WebState withRedate(){
		redate = true;
		return this;
	}

	/**
	 * Returns a new web state with the master flag set to true.
	 * @return A copy of this web state with the master flag set to true.
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
	
	public static final WebState forNewspostPreview(OsuWeb web){
		return new WebState("RoanH", web.getWikiSyncBranch(), true, false);
	}
	
	public static final WebState forRef(String namespace, String ref, boolean redate, boolean master){
		return new WebState(namespace, ref, redate, master);
	}
}
