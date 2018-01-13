# Quick Start
[![English](https://img.shields.io/badge/language-English-orange.svg)](Method-Profiling.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Method-Profiling_kr.md)

Scouter를 설치하고 Java Agent에 아무런 설정을 하지 않았다면 Scouter는 HTTP 서비스의 시작점과 종료지점, 그리고 실행한 쿼리에 대한 프로파일만을 수행한다.

하지만 경우에 따라서는 method 수준의 프로파일링이 필요한 경우도 있으며 간단한 설정 및 어플리케이션 재시작을 통해 적용이 가능하다.

너무 많은 양의 프로파일링은 오히려 서버에 부담이 될 수 있으므로 적절한 수준으로 적용하는 것이 중요하다.

아래 데모시스템에서 method profiling 을 설정하지 않으면 아래와 같은 수준으로 정보를 수집하게 된다.
> 데모 시스템 설치는 [Quick Start 가이드](../main/Quick-Start_kr.md) 참고
![](../img/tech/method_none_profile_1.png)


## Basic Options

Option 명           | 설명
--------------------|-------
hook_method_patterns| Hooking 하여 기록할 method의 pattern 정의 <br>여러개인 경우 comma(,)로 구분한다.<br> ` format : package.Class.method,package.Class2.method2`

```properties
hook_method_patterns = com.scouter.HelloWorld.hello
//hook_method_patterns = com.scouter.*.* //package : com.scouter, class : any, method : any
//hook_method_patterns = com.scouter.HelloWorld.* //package : com.scouter, class : HelloWorld method : any
```
데모시스템에서 hook_method_patterns를 설정하고 WAS를 재기동 하면 다음과 같이 프로파일에 설정한 method가 포함된 것을 확인 할 수 있다.

![](../img/tech/method_profile_1.png)

## Advanced Options

Option 명           | 설명     | default
--------------------|-------  | -------
hook_method_ignore_prefixes| profiling에서 제외할 method 패턴 prefix | get,set 
hook_method_ignore_classes | profiling에서 제외할 class | 
hook_method_access_public_enabled | public method 수집 여부 | true 
hook_method_access_private_enabled | private method 수집 여부 | false
hook_method_access_protected_enabled | protected method 수집 여부 | false
hook_method_access_none_enabled | default method 수집 여부 | false
