# Why Recent User
[![English](https://img.shields.io/badge/language-English-orange.svg)](Why-Recent-User.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Why-Recent-User_kr.md)

The concept of 'users' is separated by Visitors, Concurrent Users, and Active Users. Each one has different purpose to tell about system. With the view of performance, concurrent users are most important. 

### Concurrent Users
Concurrent users represents the number of users who are connecting to the system at the same time. Someone is writing text on the client text box, and anothers are sending submit() signal, and others are waiting the response. All of these users are counted to concurrent users. For instance, let's assume some CRM center has 500 tellers. At the peak time, concurrent users of CRM system is above 500. 500 people is telling via their telephone and others are waiting on the line. 

### Difficulty on Measurement
Though concurrent users are important metric to system, it is not easy to calculate and measure. With Little's Rule calculation of response time, TPS, think time, the equation result may not be exact. Because response time is affecting factor. There is no clear evidence to tell the preceding relationship between lateness of response time and arisen concurrent users. 

### Recent Users
Scouter measures recently visited user count, and display it as 'RecentUsers'. The time period of measurement is important unit in RecentUsers concept. By default, Scouter is measuring the number of unique visitors of last 5 minutes. But some cases like performance test which has lower thant 5 mins of think time, this should be modifiable.

You can specify this time period by modifying conter_recentuser_valid_ms.

```properties
counter_recentuser_valid_ms=300000
```
