# What special in SOUTER
[![English](https://img.shields.io/badge/language-English-orange.svg)](What-special-in-SCOUTER.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](What-special-in-SCOUTER_kr.md)

What is special in SCOUTER? In fact, monitoring the performance of applications and analysis is no different look at the big picture. But you can see that with different points of view, if you want to use that tool in a more professional. SCOUTER also has his own unique and powerful features built in.

## SCOUTER is an installation tool(not a service). 
SCOUTER must be installed inside security zone of application systems
So that SCOUTER can collect more heavy data in the target systems. This is the biggest difference to the SaaS service.
SaaS services are easy to access. But SCOUTER can deeply monitor with bigger data.

## SCOUTER is a standalone client.(not a WEB client)
SaaS services  are usually serviced by http web. But SCOUTER is standalone client, so SCOUTER can monitor bigger performance data.
That’s why SCOUTER using Eclipse RCP platform to make the client.


SCOUTER compresses performance data to store.
SCOUTER wants to collect bigger data and analyze each service transaction(request).
so SCOUTER should control a lot of data. That’s why SCOUTER save service performance and profile data on compressed files.

## SCOUTER monitors the service transactions as an individual.(XLOG)
Every service call is individually traced(profiled) and saved it.
It is possible with the compressed archiving and  standalone clients.

## SCOUTER cumulatively analyzes the active thread-stacks.
Sometimes it is not clear to understand the performance problem in a separate thread information.
At that time,  we have to think about different way. If we collect full thread stacks in many times and  analyze the stacks together, we could get an another chance to solve the performance problem.
("Stack Frequency Analyzer" Feature)

