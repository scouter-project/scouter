package scouter.server.netio.req.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import scouter.net.TcpFlag;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.db.BatchZipDB;
import scouter.util.DataUtil;

public class TcpSendStack implements ReqCommand {
	public void process(InputStream in, OutputStream out) throws IOException{
		long startTime = DataUtil.readLong(in);
		String objName = DataUtil.readText(in);
		String filename = DataUtil.readText(in);
		long fileSize = DataUtil.readLong(in);
		
        if (Configure.getInstance().log_udp_batch) {
            Logger.println(new StringBuilder(100).append("Batch stack file: ").append(objName).append('(').append(startTime).append(") - ").append(filename).toString());
        }		
		BatchZipDB.write(startTime, objName, filename, fileSize, in);
		out.write(TcpFlag.OK);
	}	
}	