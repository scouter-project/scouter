/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */

package scouter.agent.batch.netio.mtu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class MultiPacket{
    private int added = 0;
    private ArrayList<byte []> data;
    private long openTime = System.currentTimeMillis();
    
    private int total;
    private int objHash;
    private InetAddress addr;
    
    public MultiPacket(int total, int objHash, InetAddress addr){
    	this.total = total;
    	this.objHash = objHash;
    	this.addr = addr;
    	this.data = new ArrayList<byte[]>(total);
    	
    	for(int i=0; i < total; i++){
    		data.add(null);
    	}
    }

    public void set(int n, byte [] data){
        if (n < total) {
            if (this.data.get(n) == null)
                added += 1;
            this.data.add(n, data);
        }    	
    }

    public boolean isExpired(){
        return ((System.currentTimeMillis() - this.openTime) >= 1000);
    }

    public boolean isDone() {
        return total == added;
    }

    public byte[] toBytes() throws IOException{
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < total; i++) {
            out.write(this.data.get(i));
        }
        return out.toByteArray();
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder();
        sb.append("MultiPacket total=").append(total);
        sb.append(" recv=").append(added);
        sb.append(" object=(").append(objHash).append(")").append(objHash);
        sb.append(" ").append(addr);
        return sb.toString();
    }
}