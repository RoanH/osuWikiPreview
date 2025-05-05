package dev.roanh.wiki.github.obj;

/**
 * Enum of GitHub user types.
 * @author Roan
 */
public enum UserType{
	/**
	 * Indicates a regular GitHub user.
	 */
	USER,
	/**
	 * Indicates a bot user or application.
	 */
	BOT,
	/**
	 * Used for unrecognised user types.
	 */
	UNKNOWN;
}
