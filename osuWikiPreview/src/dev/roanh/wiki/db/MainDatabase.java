package dev.roanh.wiki.db;

import java.util.List;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.wiki.WebState;
import dev.roanh.wiki.data.Instance;

public final class MainDatabase{
	private static DBExecutorService executor;

	/**
	 * Prevent instantiation.
	 */
	private MainDatabase(){
	}
	
	public static void init(Configuration config){
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(config.readString("db-url") + "wikipreview", "osuweb", config.readString("db-pass")), "wiki");
	}
	
	/**
	 * Saves the current state of the web instance with the given ID.
	 * @param id The ID of the web instance to save the state of.
	 * @param state The state of the web instance.
	 * @throws DBException When a database exception occurs.
	 */
	public static void saveState(int id, WebState state) throws DBException{
		executor.insert(
			"REPLACE INTO state (id, namespace, ref, redate, master) VALUES (?, ?, ?, ?, ?)",
			id, state.namespace(), state.ref(), state.redate(), state.master()
		);
	}
	
	/**
	 * Retrieves the last known state of the web instance with the given ID.
	 * @param id The ID of the web instance to retrieve the state of.
	 * @return The last known state of the given instance or null if not known.
	 * @throws DBException When a database exception occurs.
	 */
	public static WebState getState(int id) throws DBException{
		return executor.selectFirst("SELECT * FROM state WHERE id = ?", rs->{
			return new WebState(
				rs.getString("namespace"),
				rs.getString("ref"),
				rs.getBoolean("redate"),
				rs.getBoolean("master")
			);
		}, id).orElse(null);
	}

	public static void addInstance(int id, long channel, int port) throws DBException{
		executor.insert("INSERT INTO instances (id, channel, port) VALUES (?, ?, ?)", id, channel, port);
	}
	
	public static List<Instance> getInstances() throws DBException{
		return executor.selectAll("SELECT * FROM instances", rs->{
			return new Instance(
				rs.getInt("id"),
				rs.getLong("channel"),
				rs.getInt("port")
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
