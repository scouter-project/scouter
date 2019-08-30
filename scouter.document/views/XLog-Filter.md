# XLog Filter Dialog
[![English](https://img.shields.io/badge/language-English-orange.svg)](XLog-Filter.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](XLog-Filter_kr.md)

XLog's Filter Dialog is used to filter XLog with various search conditions.   

## Search expression
You can use the following search expression, including * (asta) for most entries where strings are included:  
The search speed is fast when there is no asta.  
\* Can be used at the beginning, end, and middle of the search term.  

* If you enter `/order/\*` in the Service field, the following types of services are searched.
  * `/order/1<GET>`
  * `/order/100/products<POST>`

* If you enter `\*/order/\*` in the Service field, the following types of services are searched.
  * `/order/1<GET>`
  * `v1/order/100/products<POST>`
  * `/global/v1/order/100/products<POST>`
 
## StartHMS
Filter by duration of start time.  
It is usually used to find the cause of the delay and search in the following format.  
* start `hhmmss` ~ end `hhmmss`
  * `101030` ~ `101032` (Search xlog between 10:10 30s and 10:10 32s.)   

## Profile Size
It is usually used to identify requests that are too large for the Scouter's storage disk because the Profile size is too large.  
Use an expression with an inequality before the number.
* `"> 300"` : Filter requests with a Profile size greater than 300 (row).  
