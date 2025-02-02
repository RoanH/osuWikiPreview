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

import java.time.Instant;
import java.util.Optional;

/**
 * Record containing information on the current state of an osu! web
 * instance. This contains both information on the currently checked
 * out wiki reference as well as information on any other changes.
 * @author Roan
 * @param namespace The namespace (user or organisation) the checked
 *        out reference is under.
 * @param ref The reference (branch/sha/tag) that is currently checked out.
 * @param redate Whether news posts were redated.
 * @param master Whether ppy/master was merged into the ref beforehand.
 */
public record WebState(String namespace, String ref, boolean redate, boolean master, Optional<Long> pr, Instant available){
	//TODO need PR number + id | OR can I work with only the PR number?
	//TODO is this really a record and not a class?

	/**
	 * Gets the link to the GitHub tree associated with this state.
	 * @return The GitHub page for the namespace and reference.
	 */
	public String getGitHubTree(){
		return "https://github.com/" + namespace + "/osu-wiki/tree/" + ref;
	}
	
	public String getNamespaceWithRef(){
		return namespace + "/" + ref;
	}
	
	public boolean hasPR(){
		
	}
	
	public String getPrLink(){
		
	}
	
	public int getPrNumber(){
		
	}
	
	/**
	 * Returns a new web state with the redate flag set to true.
	 * @return A copy of this web state with the redate flag set to true.
	 */
	public WebState withRedate(){
		return new WebState(namespace, ref, true, master, pr, available);
	}

	/**
	 * Returns a new web state with the master flag set to true.
	 * @return A copy of this web state with the master flag set to true.
	 */
	public WebState withMaster(){
		return new WebState(namespace, ref, redate, true, pr, available);
	}
	
	/**
	 * Checks if this is an internal branch used by the preview system itself.
	 * @return True if this is an internal branch.
	 */
	public boolean isInternalBranch(){
		return namespace.equals("RoanH") && ref.startsWith("wikisync-");
	}
}
