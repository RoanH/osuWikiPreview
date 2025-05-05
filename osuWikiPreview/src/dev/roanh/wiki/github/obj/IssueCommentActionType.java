package dev.roanh.wiki.github.obj;

/**
 * Enum of comment action types.
 * @author Roan
 */
public enum IssueCommentActionType{
	/**
	 * A new comment was created.
	 */
	CREATED,
	/**
	 * An existing comment was deleted.
	 */
	DELETED,
	/**
	 * An existing comment was edited.
	 */
	EDITED;
}
