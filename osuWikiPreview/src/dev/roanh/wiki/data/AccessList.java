package dev.roanh.wiki.data;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Access control list based on osu! user groups and individual users.
 * @author Roan
 * @see User
 * @see UserGroup
 */
public class AccessList{
	/**
	 * Default set of users with initial access to all private mode instances.
	 * <p>
	 * In order: Roan, peppy, Hivie, Walavouchey, TicClick
	 */
	private static final Set<Integer> DEFAULT_USERS = Set.of(8214639, 2, 14102976, 5773079, 672931);
	/**
	 * Set of osu! account IDs of users with access.
	 */
	private final Set<Integer> users;
	/**
	 * Set of osu! user groups with access.
	 */
	private final GroupSet groups;
	
	/**
	 * Constructs a new access control list with only the default set of users.
	 * @see #DEFAULT_USERS
	 */
	public AccessList(){
		this(new HashSet<>(DEFAULT_USERS), new GroupSet());
	}
	
	/**
	 * Constructs a new access control with only the given users and groups.
	 * @param users IDs of users with access.
	 * @param groups Groups with access.
	 */
	private AccessList(HashSet<Integer> users, GroupSet groups){
		this.users = users;
		this.groups = groups;
	}
	
	/**
	 * Checks if this access list contains the given user, either by ID or by user group.
	 * @param user The user to check.
	 * @return True if the given user is on this access list.
	 */
	public boolean contains(User user){
		return groups.containsAny(user.groups()) || users.contains(user.osuId());
	}
	
	/**
	 * Adds the user with the given ID to this access list.
	 * @param userId The ID of the user to add.
	 */
	public void add(int userId){
		users.add(userId);
	}
	
	/**
	 * Removes the user with the given ID from this access list.
	 * @param userId The ID of the user to remove.
	 */
	public void remove(int userId){
		users.remove(userId);
	}
	
	/**
	 * Adds the given user group to this access list.
	 * @param group The group to add.
	 */
	public void add(UserGroup group){
		groups.add(group);
	}
	
	/**
	 * Removes the given user group from this access list.
	 * @param group The group to remove.
	 */
	public void remove(UserGroup group){
		groups.remove(group);
	}
	
	/**
	 * Gets the user groups on this access list.
	 * @return The groups on this access list.
	 */
	public Set<UserGroup> getGroups(){
		return groups.getGroups();
	}
	
	/**
	 * Gets the users on this access list.
	 * @return The users on this access list.
	 */
	public Set<Integer> getUsers(){
		return users;
	}
	
	/**
	 * Encodes this access list as a byte array for storage.
	 * @return The encoded access list.
	 */
	public byte[] encode(){
		ByteBuffer buf = ByteBuffer.allocate((users.size() + 2) * Integer.BYTES);
		buf.putInt(groups.encode());
		buf.putInt(users.size());
		users.forEach(buf::putInt);
		return buf.array();
	}

	/**
	 * Decodes an encoded access list.
	 * @param acl The encoded access list.
	 * @return The decoded access list.
	 */
	protected static AccessList decode(byte[] acl){
		if(acl == null){
			return null;
		}
		
		ByteBuffer buf = ByteBuffer.wrap(acl);
		GroupSet groups = new GroupSet(buf.getInt());
		
		int n = buf.getInt();
		HashSet<Integer> users = HashSet.newHashSet(n);
		for(int i = 0; i < n; i++){
			users.add(buf.getInt());
		}
		
		return new AccessList(users, groups);
	}
}
