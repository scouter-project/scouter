package scouter.xtra.redis;

import io.netty.channel.Channel;
import scouter.agent.proxy.ILettuceTrace;
import scouter.agent.trace.TraceContext;
import scouter.util.StringUtil;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LettuceTracer implements ILettuceTrace {

    private static final int MAX_LENGTH = 30 ;

    @Override
    public void startRedis(TraceContext ctx, Object channelObj) {
        try {
            if (channelObj instanceof Channel) {
                Channel channel = (Channel)channelObj;
                SocketAddress socketAddress = channel.remoteAddress();
                String address = "unknown";
                if(socketAddress != null) {
                    address = socketAddress.toString();
                }
                ctx.lastRedisConnHost = address;
            }
        } catch (Throwable t) {
            //do nothing
        }
    }

    Map<String, Object> commandSet = new ConcurrentHashMap<String, Object>();

    @Override
    public String getCommand(Object command) {
        String commandName = null;
        try {
            if(command != null && command instanceof io.lettuce.core.protocol.RedisCommand) {
                io.lettuce.core.protocol.ProtocolKeyword protocolKeyword = ((io.lettuce.core.protocol.RedisCommand)command).getType();
                if(protocolKeyword != null) {
                    commandName = protocolKeyword.name().toUpperCase();
                    if (commandSet.isEmpty()) {
                        for(io.lettuce.core.protocol.CommandType commandType : io.lettuce.core.protocol.CommandType.values()) {
                            commandSet.put(commandType.name().toUpperCase(), new Object());
                        }
                    }
                    if(commandSet.containsKey(commandName)) {
                        return commandName;
                    }
                }
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
            if(object != null && object instanceof io.lettuce.core.protocol.RedisCommand) {
                io.lettuce.core.protocol.RedisCommand redisCommand = (io.lettuce.core.protocol.RedisCommand)object;
                io.lettuce.core.protocol.CommandArgs commandArgs = redisCommand.getArgs();

                if(commandArgs != null) {
                    message = commandArgs.toCommandString();
                }

                if(StringUtil.isNotEmpty(message) && message.length() > MAX_LENGTH) {
                    message = StringUtil.truncate(message, MAX_LENGTH) + ", Args Count: " + redisCommand.getArgs().count();
                }
                return message;
            }
        } catch(Throwable t) {
            //do nothing
        }

        return message;
    }
}
