# Scouter Developer Guide
[![Englsh](https://img.shields.io/badge/language-English-orange.svg)](Developer-Guide.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Developer-Guide_kr.md)

## You can import scouter project as maven project.

* build
  - server & agent : ```mvn clean package```
  - client : ```mvn -f ./scouter.client.build/pom.xml clean package ```

* import project to the IDE
  - Scouter project : import maven project using the parent pom.
  - Scouter client project (for Eclipse Neon+)
    - build server & agent first then it makes scouter.common.jar and register it for client project library.
    - use "import existing maven project"
       - select the directory, ./scouter.client.build/pom.xml (It's parent pom)
    - If your eclipse doesn't have ZEST plugin, you should install it.
       - Help > Install New Software : Work with : http://download.eclipse.org/tools/gef/updates/releases/
         - And then install GEF(Graphical Editing Framework)


# contents below are deprecated.

## Git Fork
* browse https://github.com/scouter-project/scouter and fork it.
> <img src="../img/tech/developer/guide_20.png" width="700">

## make development environment
##### 1. Install Eclipse #####
* Download Eclipse(Java EE Developers) : https://www.eclipse.org/downloads/
* Extract and run it.

##### 2. Open git perspective #####
><img src="../img/tech/developer/guide_00.png" width="230">

><img src="../img/tech/developer/guide_01.png" width="320">

##### 3. copy scouter url #####
* https://github.com/${account}/scouter.git

##### 4. Click - Clone a Git repository #####
><img src="../img/tech/developer/guide_03.png" width="320">

##### 5. paste the url copied and click next #####
><img src="../img/tech/developer/guide_04.png" width="470">

##### 6. Choose branch and click next #####
><img src="../img/tech/developer/guide_05.png" width="470">

##### 7. Choose repository for clone #####

><img src="../img/tech/developer/guide_06.png" width="470">

> **Check "Import all existing Eclipse project after clone finishes" for auto import.**

##### 8. Import Projects #####
><img src="../img/tech/developer/guide_07.png" width="600">

##### 9. Choose Import existing Eclipse projects #####
><img src="../img/tech/developer/guide_08.png" width="500">

##### 10. Click Finish #####
><img src="../img/tech/developer/guide_09.png" width="500">

##### 11. It can be shown in Java EE perspective #####
><img src="../img/tech/developer/guide_10.png" width="250">

##### 12. Change to Maven Project #####
* change scouter.client.build project to Maven Project

><img src="../img/tech/developer/guide_11.png" width="700">

##### 13. add "TOOLS_JAR" Classpath Variable #####
* Eclipse Prefrences : Java -> Build Path -> Classpath Variables

><img src="../img/tech/developer/guide_12.png" width="700">

* Name : "TOOLS_JAR", choose $JAVA_HOME/lib/tools.jar for Path

><img src="../img/tech/developer/guide_13.png" width="500">

##### 14. plugin install : Zest for client build error
* Help -> Eclipse Marketplace : searching Zest
 of (http://download.eclipse.org/tools/gef/updates/releases/)

><img src="../img/tech/developer/guide_14.png" width="450">

##### 2. Client Build #####
* mvn clean package on scouter.client.build/pom.xml

* artifact location : target/products

><img src="../img/tech/developer/guide_23.png" width="350">

## Appendix
##### Install Scala IDE(for scouter.server) #####
* Help - Eclipse Marketplace : search "scala ide"

><img src="../img/tech/developer/guide_16.png" width="500">

* Install Scala IDE 4.2.x

