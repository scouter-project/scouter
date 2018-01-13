# XLog View
[![English](https://img.shields.io/badge/language-English-orange.svg)](XLog-View.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](XLog-View_kr.md)

## Keys
keys              | Description
----------------- | --------------------------
Left,Right arrows (← →)     | move XLog time range
Up, Down arrows (↑ ↓)     | change Y-axis scale

## Context Menu (right-click on XLog view.)
Menu1       |  Description
------------|---------------------------
Filter      | Open XLog filter dialog <br>(URL Pattern, IP, UserAgent ... )
Y Axis      | change Y-axis legend <br>(Elapsed, Cpu Time, Sql Time, API Call Time, Memory Usage ...)
Summary     | instance statistic by Service/IP/UserAgent of XLog  
Load History | Load past data of XLog <br>(adjust time range)
Search       | search XLog data by xlog-id(txid), url pattern, ip ...

> Filter and Search term can be a pattern. (eg) \*/user/dept/\*

## Another helps related

Help          |        Description
------------ | --------------
[XLog Profile View](./XLog-Profile-View.md) | about XLog profile view
[Reading XLog Chart](../client/Reading-XLog.md) | How to read XLog chart
[How to use Client](../client/How-To-Use-Client.md) | Scouter client basic