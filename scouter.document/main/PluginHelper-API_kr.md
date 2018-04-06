# PluginHelper Class
[![English](https://img.shields.io/badge/language-English-orange.svg)](PluginHelper-API.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](PluginHelper-API_kr.md)

### PluginHelper APIs (since v1.7.3)

| method | desc |
| ------------ | ---------- |
| ```void log(Object)```                                                      | logging   |
| ```void println(Object)```                                                  | System.out.println   |
| ```NumberFormat getNumberFormatter()```                                     | get NumberFormat with fraction digit 1   |
| ```NumberFormat getNumberFormatter(int fractionDigits)```                   | get NumberFormat   |
| ```formatNumber(float\|double\|int\|long)```                                   | format number with fraction digit 1  |
| ```formatNumber(float\|double\|int\|long val, int fractionDigits)```           | format number  |
| ```alertInfo(int objHash, String objType, String title, String message)```  |    |
| ```alertWarn(int objHash, String objType, String title, String message)```  |    |
| ```alertError(int objHash, String objType, String title, String message)```  |    |
| ```alertFatal(int objHash, String objType, String title, String message)```  |    |
| ```alert(byte level, int objHash, String objType, String title, String message)```  |    |
| ```String getErrorString(String yyyymmdd, int hash)```                      |    |
| ```String getApicallString(int hash)```                                     |    |
| ```String getApicallString(String yyyymmdd, int hash)```                    | if the option ```mgr_text_db_daily_api_enabled``` is true |
| ```String getMethodString(int hash)```                                      |    |
| ```String getServiceString(int hash)```                                     |    |
| ```String getServiceString(String yyyymmdd, int hash)```                    | if the option ```mgr_text_db_daily_service_enabled``` is true |
| ```String getSqlString(int hash)```                                         |    |
| ```String getSqlString(String yyyymmdd, int hash)```                        |    |
| ```String getObjectString(int hash)```                                      |    |
| ```String getRefererString(int hash)```                                     |    |
| ```String getUserAgentString(int hash)```                                   |    |
| ```String getUserGroupString(int hash)```                                   |    |
| ```String getCityString(int hash)```                                        |    |
| ```String getLoginString(int hash)```                                       |    |
| ```String getDescString(int hash)```                                        |    |
| ```String getHashMsgString(int hash)```                                     |    |
| ```Object getFieldValue(Object o, String fieldName)```         | get field value as object of 'o'    |
| ```Object invokeMethod(Object o, String methodName)```         | invoke the method    |
| ```Object invokeMethod(Object o, String methodName, Object[] args)```         | invoke the method with args    |
| ```Object invokeMethod(Object o, String methodName, Class[] argTypes, Object[] args)```         | invoke the method with args    |
| ```Object newInstance(String className)```         | new instance of the class    |
| ```Object newInstance(String className, ClassLoader loader)```         | new instance of the class from the classloader    |
| ```Object newInstance(String className, Object[] args)```         | new instance of the class with arguments    |
| ```Object newInstance(String className, ClassLoader loader, Object[] args)```         | new instance of the class with arguments from the classloader    |
| ```Object newInstance(String className, ClassLoader loader, Class[] argTypes, Object[] args)```         | new instance of the class with arguments from the classloader    |
| ```String toString(Object o)```         | invoke toString() of the object    |
| ```String toString(Object o, String def)```         | invoke toString() of the object, if null, return def.    |
| ```void alert(char level, String title, String message)```         | invoke alert (level : i\|w\|e\|f as info, warn, error, fatal).    |
| ```Class[] makeArgTypes(Class class0, Class class1, ..., classN)```         | assemble argument types array to call the reflection method ```invokeMethod()```    |
| ```Object[] makeArgs(Object obj0, Object obj1, ..., objN)```         | assemble arguments array to call the reflection method ```invokeMethod()```     |