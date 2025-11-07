package dev.roanh.wiki.data;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import dev.roanh.osuapi.user.Group;

public class GroupSet implements Iterable<UserGroup>{
	private int groups;
	
	public GroupSet(){
		this(0);
	}
	
	protected GroupSet(int groups){
		this.groups = groups;
	}
	
	public void remove(UserGroup group){
		groups &= ~group.getId();
	}
	
	public void add(UserGroup group){
		groups |= group.getId();
	}
	
	public boolean containsAny(GroupSet other){
		return (groups & other.groups) != 0;
	}
	
	protected int encode(){
		return groups;
	}
	
	@Override
	public Iterator<UserGroup> iterator(){
		return decodeGroups(groups).iterator();
	}
	
	public static Set<UserGroup> decodeGroups(int groups){
		Set<UserGroup> userGroups = EnumSet.noneOf(UserGroup.class);
		for(UserGroup group : UserGroup.values()){
			if((group.getId() & groups) != 0){
				userGroups.add(group);
			}
		}
		
		return userGroups;
	}
	
	public static int encodeGroups(Collection<? extends Group> groups){
		GroupSet set = new GroupSet();
		groups.stream().map(UserGroup::from).filter(Objects::nonNull).forEach(set::add);
		return set.encode();
	}
}
