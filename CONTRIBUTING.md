# Contributing


## Getting the source

- Check out source code:
  ```bash
  git clone https://github.com/orientechnologies/orientdb.git orientdb
  ```

## Prerequisites

A Java JDK version 17 or later is needed (the code is compiled for Java 17 and tested on 17, 21 and 25).  
Maven 3.7 or later is needed, or use the bundled wrapper (`./mvnw`).  

## Build 

- Only compile

    ```bash
    mvn clean compile
    ```
- Run base tests

    ```bash
    mvn clean test
    ```
- Full build

    ```bash
    mvn clean install
    ```
- Run integration tests (hours long tests)

    ```bash
    mvn clean install failsafe:integration-test
    ```
- Format the code with standard format used by OrientDB

    ```bash
    mvn spotless:apply 
    ```

## Produce Community Distribution

To produce the community distribution just run  

```bash
mvn clean install
```  

this command will produce two files `packages/community/target/orientdb-community-(version).tar.gz` `packages/community/target/orientdb-community-(version).zip`

## Opening a issue

If you have problem you can just open a issue on [Github Issue Tracker](github.com/orientechnologies/orientdb/issues) following the issue template.

## Send a Pull Request

You can send a pull request for fixes or for features to the [Github Pull Requests](https://github.com/orientechnologies/orientdb/pulls) to be accepted the 
PR should respect the PR template, and provide test cases for the issue fixed or new features, and should not include regressions on existing tests. 



