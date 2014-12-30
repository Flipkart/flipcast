REM Start script for flipcast service

REM Application Name
set PACKAGE=flipcast-service

REM Application Jar
set APP_JAR="flipcast-service.jar"

REM Memory settings
set JAVA_OPTS="%JAVA_OPTS% -Xms512m"
set JAVA_OPTS="%JAVA_OPTS% -Xmx512m"
set JAVA_OPTS="%JAVA_OPTS% -XX:NewSize=256m"
set JAVA_OPTS="%JAVA_OPTS% -XX:MaxNewSize=256m"

REM Set Server JVM
set JAVA_OPTS="%JAVA_OPTS% -server"

REM Enable NUMA (For modern processors) - Reduces thread contention
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseNUMA"

REM Set to headless, just in case
set JAVA_OPTS="%JAVA_OPTS% -Djava.awt.headless=true"

REM Set encoding to UTF-8
set JAVA_OPTS="%JAVA_OPTS% -Dfile.encoding=UTF-8"

REM Reduce the per-thread stack size
set JAVA_OPTS="%JAVA_OPTS% -Xss180k"

REM Force the JVM to use IPv4 stack
set JAVA_OPTS="%JAVA_OPTS% -Djava.net.preferIPv4Stack=true"

REM GC Options
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseCompressedOops"
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseG1GC"
set JAVA_OPTS="%JAVA_OPTS% -XX:+DisableExplicitGC"
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseTLAB"

REM Memory Setting
set JAVA_OPTS="%JAVA_OPTS% -XX:MetaspaceSize=512m"
set JAVA_OPTS="%JAVA_OPTS% -XX:MaxMetaspaceSize=512m"

REM Enable HotSpot optimizations)
set JAVA_OPTS="%JAVA_OPTS% -XX:+AggressiveOpts"
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseNUMA"

REM General Tuning Settings
set JAVA_OPTS="%JAVA_OPTS% -XX:CompileThreshold=50000"

REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS="%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError"

REM Setup remote JMX monitoring
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.port=29005"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.authenticate=false"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.ssl=false"

REM Provide application configuration & log configuration file path
set JAVA_OPTS="%JAVA_OPTS% -Dapp.config=config\\application.conf"
set JAVA_OPTS="%JAVA_OPTS% -Dlogback.configurationFile=config\\logback.xml"

REM Execute main application JAR
java -jar "%JAVA_OPTS%" "%APP_JAR%"
