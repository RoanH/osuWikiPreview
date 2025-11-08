package dev.roanh.wiki.data;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class AccessList{
	/**
	 * Default set of users with initial access to all private mode instances.
	 * <p>
	 * In order: Roan, peppy, Hivie, Walavouchey
	 */
	private static final Set<Integer> DEFAULT_USERS = Set.of(8214639, 2, 14102976, 5773079);
	private final Set<Integer> users;
	private final GroupSet groups;
	
	public AccessList(){
		this(new HashSet<>(DEFAULT_USERS), new GroupSet());
	}
	
	private AccessList(HashSet<Integer> users, GroupSet groups){
		this.users = users;
		this.groups = groups;
	}
	
	public boolean contains(User user){
		return groups.containsAny(user.groups()) || users.contains(user.osuId());
	}
	
	public void add(int userId){
		users.add(userId);
	}
	
	public void remove(int userId){
		users.remove(userId);
	}
	
	public void add(UserGroup group){
		groups.add(group);
	}
	
	public void remove(UserGroup group){
		groups.remove(group);
	}
	
	public Set<UserGroup> getGroups(){
		return groups.getGroups();
	}
	
	public Set<Integer> getUsers(){
		return users;
	}
	
	public byte[] encode(){
		ByteBuffer buf = ByteBuffer.allocate((users.size() + 2) * Integer.BYTES);
		buf.putInt(groups.encode());
		buf.putInt(users.size());
		users.forEach(buf::putInt);
		return buf.array();
	}

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
