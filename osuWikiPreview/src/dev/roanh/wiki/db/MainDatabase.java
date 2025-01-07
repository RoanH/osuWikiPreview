package dev.roanh.wiki.db;

import java.util.List;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.wiki.data.Instance;

public class MainDatabase{
	private static DBExecutorService executor;

	public static void init(Configuration config){
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(config.readString("db-url") + "wikipreview", "osuweb", config.readString("db-pass")), "wiki");
	}

	public static void addInstance(int id, long channel) throws DBException{
		executor.insert("INSERT INTO instances (id, channel) VALUES (?, ?)", id, channel);
	}
	
	public static List<Instance> getInstances() throws DBException{
		return executor.selectAll("SELECT * FROM instances", rs->{
			return new Instance(
				rs.getInt("id"),
				rs.getLong("channel")
			);
		});
	}

	public static void dropExtraSchemas() throws DBException{
		executor.delete("DROP DATABASE `osu_charts`");
		executor.delete("DROP DATABASE `osu_chat`");
		executor.delete("DROP DATABASE `osu_mp`");
		executor.delete("DROP DATABASE `osu_store`");
		executor.delete("DROP DATABASE `osu_updates`");
	}
}
