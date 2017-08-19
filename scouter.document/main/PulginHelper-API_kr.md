# Build Scouter
![English](https://img.shields.io/badge/language-English-orange.svg) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](PluginHelper-API.md)

### PluginHelper APIs (since v1.7.3)

| method | desc |
| ------------ | ---------- |
| void log(Object)                                                      | logging   |
| void println(Object)                                                  | System.out.println   |
| NumberFormat getNumberFormatter()                                     | get NumberFormat with fraction digit 1   |
| NumberFormat getNumberFormatter(int fractionDigits)                   | get NumberFormat   |
| formatNumber(float|double|int|long)                                   | format number with fraction digit 1  |
| formatNumber(float|double|int|long val, int fractionDigits)           | format number  |
| alertInfo(int objHash, String objType, String title, String message)  |    |
| alertWarn(int objHash, String objType, String title, String message)  |    |
| alertError(int objHash, String objType, String title, String message)  |    |
| alertFatal(int objHash, String objType, String title, String message)  |    |
| alert(byte level, int objHash, String objType, String title, String message)  |    |
| String getErrorString(String yyyymmdd, int hash)                      |    |
| String getApicallString(int hash)                                     |    |
| String getApicallString(String yyyymmdd, int hash)                    | if the option ```mgr_text_db_daily_api_enabled``` is true |
| String getMethodString(int hash)                                      |    |
| String getServiceString(int hash)                                     |    |
| String getServiceString(String yyyymmdd, int hash)                    | if the option ```mgr_text_db_daily_service_enabled``` is true |
| String getSqlString(int hash)                                         |    |
| String getSqlString(String yyyymmdd, int hash)                        |    |
| String getObjectString(int hash)                                      |    |
| String getRefererString(int hash)                                     |    |
| String getUserAgentString(int hash)                                   |    |
| String getUserGroupString(int hash)                                   |    |
| String getCityString(int hash)                                        |    |
| String getLoginString(int hash)                                       |    |
| String getDescString(int hash)                                        |    |
| String getHashMsgString(int hash)                                     |    |