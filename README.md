# RATE-server

This is the API server for RATE, a Recognition Algorithm Test Engine. RATE-server contains APIs concerning only to algorithm
evaluation, it contains the following APIs.

1. Database management
2. View management
3. Benchmark management
4. Algorithm management
5. Algorithm evaluation

## Up and running

### Install IDE and other tools/softwares needed
1. Install JDK1.7+, you have to set the environment variable `JAVA_HOME` properly
2. Mysql 5.5+
3. Maven
4. Install IntelliJ 13+ and open this project using IntelliJ (File -> Open)
5. Run `mvn package`, it will compile a executable rate-server jar package named `APIServer-xx` into `target` directory
6. Run with: `java -jar APIServer-xx`
