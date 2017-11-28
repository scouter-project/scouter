# HyperLogLog를 이용하여 방문자 계산하기
[![English](https://img.shields.io/badge/language-English-orange.svg)](Counting-Visit-Users.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Counting-Visit-Users_kr.md)

하룻동안 방문한 사용자는 시스템의 성능과 비즈니스 연관도를 설명하기 위한 중요한 지표이다. 
성능적으로 보면 동시 사용자가 중요한 기준이 되지만 
IT담당자가 아닌경우에 동시사용자 보다는 방문사용자를 많이 사용한다. 

## 방문사용자를 어떻게 측정할 것인가?

방문사용자는 측정하기 위해서는 먼저 서버 시스템에서 사용자를 유일하게 식별할  수 있어야 한다.
사용자를 유일하게 식별하는 방법은 로그인 아이디나 IP를 사용하거나JSESSIONID혹은 그와 유사한 사용자별 고유의 식별아이디를 사용할 수 있다.
 
각 방법마다 약간의 단점들이 있다.
* 로그인 - 사용자가 로그인하기 전에는 측정이 않됨
* IP - 서버에서는 식별이안됨
* JSESSIONID - 한 사용자가 여러번 반복 로그인할경우 사용자가 과댜계산될 수 있음
* COOKIE - 시스템에 Cookie를 추가하기 때문에 영향을 줄 수 있음
이러한 단점들을 고려하여 서비스 요청별 사용자를 식별한다. 

그런데 방문자 측정에 가장 어려운점은 사용자 식별보다는 실제 숫자를 계산해 내는 것에 있다. 
HashSet과 같은 클래스를 통해 몇명인지를 계산하는것이 정확하지만 메모리를 많이 사용하기 때문에 쉽지 않다. 

이러한 문제를 해결하고 적은 메모리로 방문사용자를 계산할 수 있는 적합한 알고리즘이 HyperLogLog이다. 

자세한 설명은 HyperLogLog에 대한 설명을 참조하고 여기서는 샘플 프로그램을 통해서 방문 사용자 계산하는 방법을 시뮬레이션 해본다. 

## 소스 코드

TestHLL은 천만명의 사용자를 가정하고 HyperLogLog를 사용하여 계산하면 어떤 결과가 나오는지를 시뮬레이션 해보았다.

* realSet => 실제 방문자를 계산한다(기준값)
* all =>전체 
* even => 짝수번호 사용자
* odd => 홀수 번호 사용자
* sum => all + even + odd

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

## 실행결과
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
천만명이 방문한다고 가정하고 각 구간별로 HyperLogLog알고리즘으로 계산된 방문자의 숫자이다.
어느정도 인정한만한 오차범위에서 값을 계산해 내는 것을 볼 수 있다. 

특히 부분 사용자를 통해 전체를 구할 수 있다.
위의  TestHLL 소스코드를 보면 sum에 all 변수의 사용자 값을 모두 add하여도 sum ==all을 확인 할 수 있다. 
```
HyperLogLog sum = new HyperLogLog(20);
	sum.addAll(even);
	sum.addAll(odd);
	sum.addAll(all);
```

이것은 서버별 방문자를 계산한 다음 업무별 사용자를 다시 계산 할 수 있는 의미가 된다.

방문사용자는 시스템의 성능이나 장애가 비즈니스에 미치는 영향도를 파악하는데 중요한 지표이다.
하지만 그 값이 완전히 정교할 필요는 없으며 어떠한 방법을 사용하여도 실제의 값을 구할 수는 없다.
어차피 대략의 규모를 효과적으로 파악하는 것이 중요하다.
