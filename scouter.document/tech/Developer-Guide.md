# Scouter Developer Guide
[![English](https://img.shields.io/badge/language-English-orange.svg)](Developer-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Developer-Guide_kr.md)

## Short version

* build
  - server & agent : ```build_package.sh```
  - client : ```build_client.sh```

* import project to the IDE
  - Scouter project
    - import maven project using the parent pom.
    - install Scala IDE for Eclipse (or Scala for IntelliJ) : It needs for Scouter server module.
      -- **Scala IDE 4.3.0 for scala 2.11.7**
    - install and enable Lombok support for your IDE.

  - Scouter client project (for Eclipse Neon+ & java8)
    - build server & agent first then it makes scouter.common.jar and register it for client project library.
    - use "import existing maven project"
       - select the directory, ./scouter.client.build/pom.xml (It's parent pom)
    - If your eclipse doesn't have ZEST plugin, you should install it.
       - Help > Install New Software : Work with : http://download.eclipse.org/tools/gef/updates/legacy/releases/
         - And then install GEF(Graphical Editing Framework)

## Long version

  - [setting IDE for Scouter APM development](https://translate.google.co.kr/translate?sl=ko&tl=en&js=y&prev=_t&hl=ko&ie=UTF-8&u=https%3A%2F%2Fgunsdevlog.blogspot.kr%2F2017%2F10%2Fscouter-apm-developer-environment.html&edit-text=&act=url)

