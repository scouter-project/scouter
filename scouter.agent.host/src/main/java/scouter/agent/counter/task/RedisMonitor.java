package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;

/**
 * This prototype was contributed by jeonwoosung@gmail.com
 * Deprecated - This feature is going to move https://github.com/scouter-project/scouter-redis-agent
 */
public class RedisMonitor {
	private static HashSet<String> floatSet = new HashSet<String>();
	private static HashSet<String> decimalSet = new HashSet<String>();
	private Socket s;

	static {
		floatSet.add("used_cpu_sys");
		floatSet.add("used_cpu_user");
		floatSet.add("used_cpu_sys_children");
		floatSet.add("used_cpu_user_children");
		floatSet.add("mem_fragmentation_ratio");
		decimalSet.add("uptime_in_seconds");
		decimalSet.add("uptime_in_days");
		decimalSet.add("lru_clock");
		decimalSet.add("connected_clients");
		decimalSet.add("connected_slaves");
		decimalSet.add("client_longest_output_list");
		decimalSet.add("client_biggest_input_buf");
		decimalSet.add("blocked_clients");
		decimalSet.add("used_memory");
		decimalSet.add("used_memory_rss");
		decimalSet.add("used_memory_peak");
		decimalSet.add("loading");
		decimalSet.add("aof_enabled");
		decimalSet.add("changes_since_last_save");
		decimalSet.add("bgsave_in_progress");
		decimalSet.add("bgrewriteaof_in_progress");
		decimalSet.add("total_connections_received");
		decimalSet.add("total_commands_processed");
		decimalSet.add("expired_keys");
		decimalSet.add("evicted_keys");
		decimalSet.add("keyspace_hits");
		decimalSet.add("keyspace_misses");
		decimalSet.add("pubsub_channels");
		decimalSet.add("pubsub_patterns");
		decimalSet.add("latest_fork_usec");
		decimalSet.add("vm_enabled");
	}

	//@Counter(interval = 10000)
	public void process(CounterBasket pw) throws IOException {
		Configure conf = Configure.getInstance();

		boolean redisEnabled = conf.getBoolean("redis_enabled", false);

		if (redisEnabled) {
			String serverIp = conf.getValue("redis_server_ip", "127.0.0.1");
			int serverPort = conf.getInt("redis_server_port", 6379);

			String perfInfo = getRedisPerfInfo(serverIp, serverPort);

			String[] lines = perfInfo.split("\n");

			PerfCounterPack p = pw.getPack(conf.getObjName(), TimeTypeEnum.REALTIME);

			for (String line : lines) {
				String key = line.substring(0, line.indexOf(':'));
				String value = line.substring(line.indexOf(':') + 1);

				if (floatSet.contains(key)) {
					p.put(key, new FloatValue(Float.valueOf(value.trim())));
				}

				if (decimalSet.contains(key)) {
					p.put(key, new DecimalValue(Long.valueOf(value.trim())));
				}
			}
		}
	}

	private String getRedisPerfInfo(String serverIp, int serverPort) throws IOException {
		s = new Socket(serverIp, serverPort);
		InputStream is = s.getInputStream();
		OutputStream os = s.getOutputStream();
		os.write("INFO\r\n".getBytes());
		os.flush();

		byte[] size = new byte[10];

		int i = is.read();

		int j = 0;

		while (i != '\n') {
			size[j++] = (byte) i;
			i = is.read();
		}

		int length = Integer.valueOf(new String(size, 1, j - 2));
		byte[] b = new byte[length];
		is.read(b);

		s.close();

		return new String(b);
	}
	
}
