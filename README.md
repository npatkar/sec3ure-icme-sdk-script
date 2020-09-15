
##Steps to importing project in Intellj

```bash
File -> new -> Project from existing source -> got to project path select pom.xml
```


##Command to execute 

```bash
mvn clean package

```
Above command Will generate jar file located in Target directory.

>java -jar genkeys-1.0-SNAPSHOT.jar input.json vendor-settings.json sec3ure-icme-sdk.jar

Accepting 2 arguements
1) input json file 
2) vendor-settings json file
3) jar file where p12 certificate file is going to append.