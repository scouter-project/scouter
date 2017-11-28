# Getting unique visitors with HyperLogLog algorithm
[![English](https://img.shields.io/badge/language-English-orange.svg)](Counting-Visit-Users.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Counting-Visit-Users_kr.md)

'Visitors' is important metric to understand relationship between business frequency and system performance. Just with performance, the highest concurrent users are in case. But with the perspective of business visitors are more important than concurrent user. 

## How to count unique visitors?

When it count the unique visitors, each user's request should be distinguishable. There are several ways to give uniqueness to each request like using login ID, IP address, checking special cookie (like JSESSIONID), or other methodologies giving unique ID.
 
Each way has pros also cons,
* Login ID - It will not be counted until user logged in the system.
* IP Address - In some environments getting client's IP address is not possible.
* JSESSIONID - Multiple accessing of system within counting period will be counted redundantly.
* COOKIE - Adopting new cookie can make system more complexed.

You should understand all of this technique and cons to count exact visiting number. 

For the developer it is also difficult to implement effective source code to count up, because counting is very slow and greedy. For example counting with HashSet collection consumes very much memory.

The HyperLogLog is proper algorithm not consuming so much memory to estimate or approximate unique visitors. Please refer to the other articles or documents for more about HyperLogLog. Here is the simulation of it. 

## Example

TestHLL example had simulated the situation of 10 million users accessing the system, and calculated result with HyperLogLog.

* realSet : Calculate real visitor
* all : means total users 
* even : user with even number ticket
* odd : user with odd number ticket
* sum = all + even + odd

```
import java.util.HashSet;
import java.util.Random;

public class TestHLL {

	public static void main(String[] args) {
		HashSet<Long> realSet = new HashSet<Long>();
		HyperLogLog all = new HyperLogLog(20);
		HyperLogLog odd = new HyperLogLog(20);
		HyperLogLog even = new HyperLogLog(20);
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
				HyperLogLog sum = new HyperLogLog(20);
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
```

## Results
```
10 => all=10 even=5 odd=5  sum=10
20 => all=20 even=10 odd=10  sum=20
30 => all=30 even=15 odd=15  sum=30
40 => all=40 even=20 odd=20  sum=40
50 => all=50 even=25 odd=25  sum=50
60 => all=60 even=30 odd=30  sum=60
70 => all=70 even=35 odd=35  sum=70
80 => all=80 even=40 odd=40  sum=80
90 => all=90 even=45 odd=45  sum=90
100 => all=100 even=50 odd=50  sum=100
200 => all=200 even=100 odd=100  sum=200
300 => all=300 even=150 odd=150  sum=300
400 => all=400 even=200 odd=200  sum=400
500 => all=500 even=250 odd=250  sum=500
600 => all=600 even=300 odd=300  sum=600
700 => all=700 even=350 odd=350  sum=700
800 => all=800 even=400 odd=400  sum=800
900 => all=900 even=450 odd=450  sum=900
1000 => all=1000 even=500 odd=500  sum=1000
2000 => all=2001 even=1000 odd=999  sum=2001
3000 => all=3002 even=1501 odd=1500  sum=3002
4000 => all=4004 even=2002 odd=1999  sum=4004
5000 => all=5007 even=2502 odd=2500  sum=5007
6000 => all=6006 even=3001 odd=2999  sum=6006
7000 => all=7007 even=3502 odd=3499  sum=7007
8000 => all=8007 even=4003 odd=3996  sum=8007
9000 => all=9011 even=4504 odd=4496  sum=9011
10000 => all=10004 even=5002 odd=4992  sum=10004
20000 => all=20035 even=10012 odd=9989  sum=20035
30000 => all=30030 even=15007 odd=14992  sum=30030
40000 => all=40023 even=20017 odd=19989  sum=40023
50000 => all=50016 even=25005 odd=25002  sum=50016
60000 => all=60047 even=30000 odd=30023  sum=60047
70000 => all=70068 even=35011 odd=35017  sum=70068
80000 => all=80038 even=40020 odd=40016  sum=80038
90000 => all=90021 even=45023 odd=45017  sum=90021
100000 => all=100018 even=50039 odd=50017  sum=100018
200000 => all=199911 even=99994 odd=99963  sum=199911
300000 => all=299710 even=149991 odd=149879  sum=299710
400000 => all=399831 even=199970 odd=199799  sum=399831
500000 => all=499895 even=249955 odd=249630  sum=499895
600000 => all=599831 even=300063 odd=299386  sum=599831
700000 => all=700233 even=350118 odd=349448  sum=700233
800000 => all=800473 even=400133 odd=399261  sum=800473
900000 => all=900218 even=450040 odd=449229  sum=900218
1000000 => all=1000371 even=500330 odd=499040  sum=1000371
2000000 => all=2001974 even=1000016 odd=1000177  sum=2001974
3000000 => all=3040087 even=1499566 odd=1500214  sum=3040087
4000000 => all=4004521 even=1999197 odd=1998733  sum=4004521
5000000 => all=4992361 even=2493801 odd=2497940  sum=4992361
6000000 => all=5991484 even=3036032 odd=3039342  sum=5991484
7000000 => all=6996597 even=3515749 odd=3522037  sum=6996597
8000000 => all=7993206 even=4003765 odd=4012036  sum=7993206
9000000 => all=8986135 even=4495527 odd=4506118  sum=8986135
10000000 => all=9984382 even=4992773 odd=5001554  sum=9984382
```
Each row represents the simulation result to 10 million users. As HyperLogLog algorithm is using statistical approach, the countering number is not absolutely exact number, provides margin of error. But the margin rate is low and acceptable. 

And with the number of part user, we can get all user value. You can find that 'sum' variable is equivalent to 'all' variable.
```
HyperLogLog sum = new HyperLogLog(20);
	sum.addAll(even);
	sum.addAll(odd);
	sum.addAll(all);
```

This means counting users for each business function and each server.

Unique visitors is important because it has relationship between incoming request and system performance. And it is important to apply estimating or approximating algorithm with acceptable margin of errors.
