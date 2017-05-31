package scouter.agent.netio.data.net;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class Test {

	public static void main(String[] args) throws SocketException {
		ArrayList<DatagramSocket> a = new ArrayList<DatagramSocket>();
		for(int i = 0 ; i < 10;i++){
			DatagramSocket d = new DatagramSocket(0);	
			a.add(d);
			System.out.println(d.getLocalPort());
		}

	}

}
