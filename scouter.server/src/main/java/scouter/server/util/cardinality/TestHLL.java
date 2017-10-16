package scouter.server.util.cardinality;

import java.util.HashSet;
import java.util.Random;

public class TestHLL {


	public static void main(String[] args) {
		int rsd=20;
		HashSet<Long> realSet = new HashSet<Long>();
		HyperLogLog all = new HyperLogLog(rsd);
		HyperLogLog odd = new HyperLogLog(rsd);
		HyperLogLog even = new HyperLogLog(rsd);
		Random r = new Random();
		for (int i = 1; i <= 10000000; i++) {
			long value=r.nextLong();
			all.offer(value);
			realSet.add(value);
			if(i%2==0)
				even.offer(value);
			else
				odd.offer(value);
			
			int u = unit(i);
			if (i % u == 0) {
				HyperLogLog sum = new HyperLogLog(rsd);
				sum.addAll(even);
				sum.addAll(odd);
				sum.addAll(all);
				System.out.println(realSet.size() + " => all=" + all.cardinality()  +" even="+even.cardinality() + " odd=" + odd.cardinality()+ "  sum=" +sum.cardinality() );
			}
		}

	}

	private static int unit(int value) {
		if (value < 10)
			return 10;
		int decVal = 1;
		for (int x = value; x >= 10; x /= 10) {
			decVal *= 10;
		}
		return decVal;
	}

}
