REM Start script for flipcast service

REM Application Name
set PACKAGE=flipcast-service

REM Application Jar
set APP_JAR="flipcast-service.jar"

REM Memory settings
set JAVA_OPTS="%JAVA_OPTS% -Xms1024m"
set JAVA_OPTS="%JAVA_OPTS% -Xmx1024m"
set JAVA_OPTS="%JAVA_OPTS% -Xmn512m"
set JAVA_OPTS="%JAVA_OPTS% -XX:NewSize=512m"
set JAVA_OPTS="%JAVA_OPTS% -XX:MaxNewSize=512m"


REM Set Server JVM
set JAVA_OPTS="%JAVA_OPTS% -server"

REM Enable NUMA (For modern processors) - Reduces thread contention
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseNUMA"

REM Set to headless, just in case
set JAVA_OPTS="%JAVA_OPTS% -Djava.awt.headless=true"

REM Set encoding to UTF-8
set JAVA_OPTS="%JAVA_OPTS% -Dfile.encoding=UTF-8"

REM Program Name
set JAVA_OPTS="%JAVA_OPTS% -D${PACKAGE}"

REM Reduce the per-thread stack size
set JAVA_OPTS="%JAVA_OPTS% -Xss180k"

REM Force the JVM to use IPv4 stack
set JAVA_OPTS="%JAVA_OPTS% -Djava.net.preferIPv4Stack=true"

REM GC Options
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseParNewGC"
set JAVA_OPTS="%JAVA_OPTS% -XX:-UseParallelGC"
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseConcMarkSweepGC"
set JAVA_OPTS="%JAVA_OPTS% -XX:+CMSParallelRemarkEnabled"

REM Memory Setting
set JAVA_OPTS="%JAVA_OPTS% -XX:PermSize=256m"
set JAVA_OPTS="%JAVA_OPTS% -XX:MaxPermSize=256m"

REM Enable HotSpot optimizations)
set JAVA_OPTS="%JAVA_OPTS% -XX:+AggressiveOpts"
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseNUMA"

REM Disable Biased locking
set JAVA_OPTS="%JAVA_OPTS% -XX:-UseBiasedLocking"

REM Threading Policies
set JAVA_OPTS="%JAVA_OPTS% -XX:+UseThreadPriorities"
set JAVA_OPTS="%JAVA_OPTS% -XX:ThreadPriorityPolicy=30"

REM General Tuning Settings
set JAVA_OPTS="%JAVA_OPTS% -XX:CompileThreshold=50000"

REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS="%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError"

REM Setup remote JMX monitoring
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.port=29005"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.authenticate=false"
set JAVA_OPTS="%JAVA_OPTS% -Dcom.sun.management.jmxremote.ssl=false"
set JAVA_OPTS="%JAVA_OPTS% -Dhazelcast.jmx=true"

REM Execute main application JAR
java -jar "%JAVA_OPTS%" "%APP_JAR%"
