package scouter.util;

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

class StackUtil {
    private MBeanServerConnection server;
    private ThreadMXBean mXBean;
    private ObjectName objName;
    private String headerString;

    private boolean hasDumpAllThreads;

    public StackUtil(MBeanServerConnection server) throws IOException {
        this.server = server;
        this.mXBean = newPlatformMXBeanProxy(server, THREAD_MXBEAN_NAME, ThreadMXBean.class);

        try {
            objName = new ObjectName(THREAD_MXBEAN_NAME);
            checkDumpAllThreads();
            headerString = getHeaderString();
        } catch ( Exception e ) {
            InternalError ie = new InternalError(e.getMessage());
            ie.initCause(e);
            throw ie;
        }
    }
   
    public StackUtil() {
        this.mXBean = getThreadMXBean();
        checkDumpAllThreads();
        headerString = getHeaderString();
    }

    private Properties getSystemProperties() {
        try {
            RuntimeMXBean runtime = getRuntimeMXBean();
            ;
            if ( runtime != null ) {
                Properties prop = new Properties();
                prop.putAll(runtime.getSystemProperties());
                return prop;
            }
            return null;
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public String getHeaderString() {
        Properties prop = getSystemProperties();
        StringBuilder sb = new StringBuilder(100);
        sb.append("Full thread dump ").append(prop.getProperty("java.vm.name")).append(" (").append(prop.getProperty("java.vm.version")).append(' ').append(prop.getProperty("java.vm.info"));
        return sb.toString();
    }

    public List<String> takeThreadDump() throws Exception {
        ThreadMXBean threadMXBean = this.mXBean;
        if ( threadMXBean == null ) {
            return null;
        }

        List<String> list = new ArrayList<String>(100);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        list.add(df.format(new Date()));
        list.add(headerString);
 
        ThreadInfo[] threads;
        if ( hasDumpAllThreads ) {
            threads = threadMXBean.dumpAllThreads(true, true);
        } else {
            long[] threadIds = threadMXBean.getAllThreadIds();
            threads = threadMXBean.getThreadInfo(threadIds, 2147483647);
        }
        printThreads(list, threadMXBean, threads);
        return list;
    }

    private void printThreads( List<String> list, ThreadMXBean threadMXBean, ThreadInfo[] threads ) {
        boolean jdk16 = hasDumpAllThreads;

        for ( ThreadInfo thread : threads ){
            if ( thread != null ){
                if ( jdk16 )
                    print16Thread(list, threadMXBean, thread);
                else
                    print15Thread(list, thread);
            }
        }
    }

    private void print16Thread( List<String> list, ThreadMXBean threadMXBean, ThreadInfo thread )
    {
        MonitorInfo[] monitors = null;
        if ( threadMXBean.isObjectMonitorUsageSupported() ) {
            monitors = thread.getLockedMonitors();
        }
        list.add("");
        list.add(new StringBuilder(100).append('\"').append(thread.getThreadName()).append("\" - Thread t@").append(thread.getThreadId()).toString());
        list.add("   java.lang.Thread.State: " + thread.getThreadState());
        int index = 0;
        
        StringBuilder sb;
    	LockInfo lock = thread.getLockInfo();
        String lockOwner = thread.getLockOwnerName();
        for ( StackTraceElement st : thread.getStackTrace() ) {
       
        	list.add("\tat " + st.toString());
            
            if ( index == 0 ) {
                if ( ("java.lang.Object".equals(st.getClassName())) && ("wait".equals(st.getMethodName())) )
                {
                    if ( lock != null ) {
                    	sb = new StringBuilder(100);
                        sb.append("\t- waiting on ");
                        printLock(sb, lock);
                        list.add(sb.toString());
                    }
                } else if ( lock != null ) {
                    if ( lockOwner == null ) {
                    	sb = new StringBuilder(100);
                        sb.append("\t- parking to wait for ");
                        printLock(sb, lock);
                    } else {
                    	sb = new StringBuilder(100);
                        sb.append("\t- waiting to lock ");
                        printLock(sb, lock);
                        sb.append(" owned by \"").append(lockOwner).append("\" t@").append(thread.getLockOwnerId());
                        printLock(sb, lock);
                    }
                }
            }
            printMonitors(list, monitors, index);
            index++;
        }
    }

    private void printMonitors( List<String> list, MonitorInfo[] monitors, int index )
    {
        if ( monitors != null )
            for ( MonitorInfo mi : monitors )
                if ( mi.getLockedStackDepth() == index ) {
                	StringBuilder sb = new StringBuilder(100);
                    sb.append("\t- locked ");
                    printLock(sb, mi);
                    list.add(sb.toString());
                }
    }

    private void print15Thread( List<String> list, ThreadInfo thread )
    {
    	list.add("");
    	list.add(new StringBuilder(100).append('\"').append(thread.getThreadName()).append("\" - Thread t@").append(thread.getThreadId()).toString());

    	StringBuilder sb = new StringBuilder(100);
        sb.append("   java.lang.Thread.State: ").append(thread.getThreadState());
        if ( thread.getLockName() != null ) {
            sb.append(" on ").append(thread.getLockName());
            if ( thread.getLockOwnerName() != null ) {
                sb.append(" owned by: ").append(thread.getLockOwnerName());
            }
        }
        list.add(sb.toString());
        for ( StackTraceElement st : thread.getStackTrace() )
            list.add("\tat " + st.toString());
    }

    private void printLock( StringBuilder sb, LockInfo lock )
    {
        sb.append('<').append(Integer.toHexString(lock.getIdentityHashCode())).append("> (a ").append(lock.getClassName()).append(')');
    }

    private void checkDumpAllThreads() {
        synchronized (this) {
            hasDumpAllThreads = false;
            if(server == null){
            	if(System.getProperty("java.version").compareTo("1.5") >= 0){
            		hasDumpAllThreads = true;
            	}
            }else{
	            try {
	                MBeanOperationInfo[] mopis = server.getMBeanInfo(objName).getOperations();
	                if ( mopis != null ) {
	                    for ( MBeanOperationInfo op : mopis ) {
	                        if ( "dumpAllThreads".equals(op.getName()) ) {
	                            hasDumpAllThreads = true;
	                            break;
	                        }
	                    }
	                }
	            } catch ( Exception ex ) {
	                ex.printStackTrace();
	            }
            }
        }
    }
}
