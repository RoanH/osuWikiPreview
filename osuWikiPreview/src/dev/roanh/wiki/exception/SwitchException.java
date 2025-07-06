package dev.roanh.wiki.exception;

/**
 * Root class for explainable switch failures.
 * @author Roan
 */
public abstract class SwitchException extends Exception{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 1L;

	protected SwitchException(String reason){
		super(reason);
	}
}
