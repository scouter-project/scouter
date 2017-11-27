# Console Mode Running
[![English](https://img.shields.io/badge/language-English-orange.svg)](Console-Mode-Running.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Console-Mode-Running_kr.md)

It is also provided console-mode-monitoring. But remember this method is temporary for rarely special cases.

For example, you can use console-mode-monitoring,

1. When the communication is not establishable between Server and Client due to Company firewall policy
2. Network quality is not very good,

For any situations when Client is useless not to connect to Server directly, console-mode-monitoring is useful.

Use startcon.sh startup script to use Scouter as console-mode-monitoring. You'll see command line prompt like, 
![Tomcat](../img/server/scouter_console.png)  


Important points,

1. When console-mode was stopped, Server will go down. To startup as daemon mode, you should run the server again.
2. Console mode supports full features.

