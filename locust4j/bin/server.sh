BZBIN="${BZ_SOURCE-$0}"
BZBIN="$(dirname "${BZBIN}")"
BZBINDIR="$(cd "${BZBIN}"; pwd)"

LOGCFG=config/log4j2-test.xml

#

if $cygwin
then
    # cygwin has a "kill" in the shell itself, gets confused
    KILL=/bin/kill
else
    KILL=kill
fi




LOGHOME="logs"
if [ ! -w "$LOGHOME" ] ; then
mkdir -p "$LOGHOME"
fi


if $cygwin
then
    LOG_DIR=`cygpath -wp "$LOG_DIR"`
    BZCFGDIR=`cygpath -wp "$BZCFGDIR"`
fi

JVM_MODULES="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED"

JVM_ARGS="-Dlogging.path=${LOGHOME} -Dlog4j.configurationFile=${LOGFILE}"



JVM_OPS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump.hprof \
    -XX:-OmitStackTraceInFastThrow -XX:AutoBoxCacheMax=20000 -XX:+ExplicitGCInvokesConcurrent \
    -Xlog:gc*=debug:file=logs/gc_%t.log:time,uptime,tags \
    -XX:ErrorFile=logs/hs_error%p.log \
    -XX:+UseG1GC -XX:G1ReservePercent=10 \
    -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m \
    -Xms1G -Xmx1G"

CLS="lib"

MAIN_JAR="locust4j-1.0.0.jar"

_BZ_DAEMON_OUT="$LOGHOME/im-server-$HOSTNAME.out"




if [ -z "$BZPIDFILE" ]; then
    BZPIDFILE="$BZBINDIR/im_server.pid"
else
    # ensure it exists, otw stop will fail
    mkdir -p "$(dirname "$BZPIDFILE")"
fi

JAVA=java

case $1 in
start)
    echo -n "Starting IM ..."
    if [ -f "$BZPIDFILE" ]; then
        if kill -0 `cat "$BZPIDFILE"` > /dev/null 2>&1; then
            echo $command already runnning as process `cat "$BZPIDFILE"`.
            exit 1
        fi
    fi
    nohup "$JAVA" "-Dlocust" \
    $JVM_ARGS \
    $JVM_OPS \
    $JVM_MODULES \
    -cp $CLS -jar $MAIN_JAR > $_BZ_DAEMON_OUT 2>&1 < /dev/null &
    if [ $? -eq 0 ]
    then
        /bin/echo -n $! > "$BZPIDFILE"
        if [ $? -eq 0 ];
        then
            sleep 1
            pid=$(cat "${BZPIDFILE}")
            if ps -p "${pid}" > /dev/null 2>&1; then
                echo STARTED
            else
                echo FAILED TO START
                exit 1
            fi
        else
            echo FAILED TO WRITE PID
            exit 1
        fi

    else
        echo SERVER DID NOT START
        exit 1
    fi
    ;;
start-fg)
    BZ_CMD=(exec "$JAVA")
    if [ "${BZ_NOEXEC}" != "" ]; then
        BZ_CMD=("$JAVA")
    fi
    "${BZ_CMD[@]}" "-Dlocust" \
    $JVM_ARGS \
    $JVM_OPS \
    $JVM_MODULES \
    -cp $CLS $MAIN_JAR
    ;;
print-cmd)
    echo "\"$JAVA\" -Dlocust \
    $JVM_ARGS \
    $JVM_OPS \
    $JVM_MODULES \
    -cp \"$CLS\" $MAIN_JAR > \"$_BZ_DAEMON_OUT\" 2>&1 < /dev/null"
    ;;
shutdown)
    echo -n "Stopping IM ..."
    if [ ! -f "$BZPIDFILE" ]
    then
        echo "no im to stop (could not find file $BZPIDFILE)"
    else
        pid=$(cat "${BZPIDFILE}")
        kill $pid
        for i in `seq 10`
        do
            if ps -p "${pid}" > /dev/null 2>&1; then
                echo -n .
                sleep 1
            else
                pid=
                break
            fi
        done
        if [ -z "$pid" ]; then
            rm -f "$BZPIDFILE"
            echo STOPPED
        else
            echo "FAILED TO STOP: $pid"
        fi
    fi
    exit 0
    ;;
restart)
    shift
    "$0" shutdown ${@}
    sleep 3
    "$0" start ${@}
    ;;
*)
    echo "Usage: $0 {start|start-fg|shutdown|restart|print-cmd}" >&2

esac
