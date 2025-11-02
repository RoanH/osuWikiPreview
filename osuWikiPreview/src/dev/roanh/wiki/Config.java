package dev.roanh.wiki;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;

public record Config(Configuration config){

	
	
	
	
	
	
	public DBContext getDatabaseContext(String schema){
		return new DBContext(config.readString("db-url") + schema, "osuweb", config.readString("db-pass"));
	}
}
