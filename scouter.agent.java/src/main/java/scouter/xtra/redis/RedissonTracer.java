package scouter.xtra.redis;

import org.redisson.Redisson;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.spring.data.connection.RedissonConnection;
import scouter.agent.proxy.IRedissonTrace;
import scouter.agent.trace.TraceContext;
import scouter.util.StringUtil;

public class RedissonTracer implements IRedissonTrace {

    private static final int MAX_LENGTH = 30 ;

    @Override
    public void startRedis(TraceContext ctx, Object redissonConnection) {
        try {
            if (redissonConnection instanceof RedissonConnection) {
                String address = "unknown";
                RedissonConnection conn = (RedissonConnection)redissonConnection;
                Redisson r = (Redisson) conn.getNativeConnection();
                if (r != null) {
                    String masterAddress = r.getConnectionManager().getConfig().getMasterAddress();
                    if (masterAddress != null) {
                        address = masterAddress;
                    }
                }
                ctx.lastRedisConnHost = address;
            }
        } catch (Throwable t) {
            //do nothing
        }
    }

    @Override
    public String getCommand(Object command) {
        String commandName = null;
        try {
            if(command instanceof RedisCommand) {
                commandName = ((RedisCommand)command).getName();
            }
        } catch (Throwable t) {
            //do nothing
        }

        return commandName;
    }

    @Override
    public String parseArgs(Object object) {
        String message = null;
        try {
            if(object instanceof byte[]) {
                byte[] keyBytes = (byte[])object;
                if(keyBytes != null) {
                    message = new String(keyBytes);
                }

                if(StringUtil.isNotEmpty(message) && message.length() > MAX_LENGTH) {
                    message = StringUtil.truncate(message, MAX_LENGTH);
                }
                return message;
            }
        } catch(Throwable t) {
            //do nothing
        }

        return message;
    }
}
