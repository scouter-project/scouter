# XLog Filter Dialog
[![English](https://img.shields.io/badge/language-English-orange.svg)](XLog-Filter.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](XLog-Filter_kr.md)

XLog의 Filter Dialog는 여러가지 검색조건으로 XLog를 필터링하는데 사용한다. 

## 검색 표현식
문자열이 들어가는 대부분의 항목에 *(asta)를 포함하여 아래와 같은 검색 표현식을 사용할 수 있다.   
단 *(asta)가 없는 경우의 검색 속도가 빠르다.    
\* 는 검색어의 맨앞, 맨뒤, 그리고 중간에 하나씩 사용할 수 있다.   

* 만약 Service 항목에 /order/* 로 기입하면 아래와 같은 유형의 Service 들이 검색된다.
  * /order/1<GET>
  * /order/100/products<POST>

* 만약 Service 항목에 */order/* 로 기입하면 아래와 같은 유형의 Service 들이 검색된다.
  * /order/1<GET>
  * v1/order/100/products<POST>
  * /global/v1/order/100/products<POST>
 
## StartHMS
start time 의 duration으로 필터링 한다.   
보통 지연이 발생된 원인을 찾기 위해 사용하며 아래와 같은 형식으로 검색한다.
* start hhmmss ~ end hhmmss
  * 101030 ~ 101032 (10:10 30s ~ 10:10 32s 사이의 xlog를 검색한다.)   

## Profile Size
보통 Profile의 size가 너무 커서 Scouter의 저장 디스크를 많이 사용하는 요청을 식별하기 위해서 사용한다.
숫자 앞에 부등호가 붙은 표현식을 사용한다.
* "> 300" : Profile size가 300(row) 이상인 요청을 필터링한다.  
