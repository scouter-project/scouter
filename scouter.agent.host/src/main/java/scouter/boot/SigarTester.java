package scouter.boot;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2016. 5. 9.
 */
public class SigarTester {
//    private static Sigar sigar = new Sigar();
//    private HashMap<String, Map> previousNetworkStats = new HashMap<String, Map>();
//    private HashMap<String, Map> previousDiskStats = new HashMap<String, Map>();
//
//
//    public static void main(String[] args) {
//        SigarTester tester = new SigarTester();
//        tester.run();
//        ThreadUtil.sleep(2000);
//        tester.run();
//        ThreadUtil.sleep(2000);
//        tester.run();
//        ThreadUtil.sleep(2000);
//        tester.run();
//        ThreadUtil.sleep(2000);
//        tester.run();
//        ThreadUtil.sleep(2000);
//        tester.run();
//    }
//
//    public void run() {
//        boolean skip = false;
//        CpuInfo[] cpuinfo = null;
//        CpuPerc[] cpuPerc = null;
//        Mem mem = null;
//        Swap swap = null;
//        FileSystem[] fs = null;
//        String[] netIf = null;
//        Uptime uptime = null;
//        double[] loadavg = null;
//        Map map = new HashMap();
//        try {
//            // CPU utilization
//            List list = new ArrayList();
//            try {
//                cpuinfo = sigar.getCpuInfoList();
//                cpuPerc = sigar.getCpuPercList();
//                List cpuList = new ArrayList();
//                for (int i = 0; i < cpuinfo.length; i++) {
//                    Map cpuMap = new HashMap();
//                    cpuMap.putAll(cpuinfo[i].toMap());
//                    cpuMap.put("combined", cpuPerc[i].getCombined());
//                    cpuMap.put("user", cpuPerc[i].getUser());
//                    cpuMap.put("sys", cpuPerc[i].getSys());
//                    cpuMap.put("idle", cpuPerc[i].getIdle());
//                    cpuMap.put("wait", cpuPerc[i].getWait());
//                    cpuMap.put("nice", cpuPerc[i].getNice());
//                    cpuMap.put("irq", cpuPerc[i].getIrq());
//                    cpuList.add(cpuMap);
//                }
//                sigar.getCpuPerc();
//                map.put("cpu", cpuList);
//
//                // Uptime
//                uptime = sigar.getUptime();
//                map.put("uptime", uptime.getUptime());
//
//                // Load Average
//                loadavg = sigar.getLoadAverage();
//                list.add(loadavg[0]);
//                list.add(loadavg[1]);
//                list.add(loadavg[2]);
//            } catch(SigarException se) {
//                se.printStackTrace();
////                log.error("SigarException caused during collection of CPU utilization");
////                log.error(ExceptionUtils.getStackTrace(se));
//            } finally {
//                map.put("loadavg", list);
//            }
//
//
//            // Memory Utilization
//            Map memMap = new HashMap();
//            Map swapMap = new HashMap();
//            try {
//                mem = sigar.getMem();
//                memMap.putAll(mem.toMap());
//
//                // Swap Utilization
//                swap = sigar.getSwap();
//                swapMap.putAll(swap.toMap());
//            } catch(SigarException se){
//                se.printStackTrace();
////                log.error("SigarException caused during collection of Memory utilization");
////                log.error(ExceptionUtils.getStackTrace(se));
//            } finally {
//                map.put("memory", memMap);
//                map.put("swap", swapMap);
//            }
//
//            // Network Utilization
//            List netInterfaces = new ArrayList();
//            try {
//                netIf = sigar.getNetInterfaceList();
//                for (int i = 0; i < netIf.length; i++) {
//                    NetInterfaceStat net = new NetInterfaceStat();
//                    try {
//                        net = sigar.getNetInterfaceStat(netIf[i]);
//                    } catch(SigarException e){
//                        // Ignore the exception when trying to stat network interface
//                        System.out.println("SigarException trying to stat network device "+netIf[i]);
//                        //log.warn("SigarException trying to stat network device "+netIf[i]);
//                        continue;
//                    }
//                    Map netMap = new HashMap();
//                    netMap.putAll(net.toMap());
//                    if(previousNetworkStats.containsKey(netIf[i])) {
//                        Map deltaMap = previousNetworkStats.get(netIf[i]);
//                        deltaMap.put("RxBytes", Long.parseLong(netMap.get("RxBytes").toString()) - Long.parseLong(deltaMap.get("RxBytes").toString()));
//                        deltaMap.put("RxDropped", Long.parseLong(netMap.get("RxDropped").toString()) - Long.parseLong(deltaMap.get("RxDropped").toString()));
//                        deltaMap.put("RxErrors", Long.parseLong(netMap.get("RxErrors").toString()) - Long.parseLong(deltaMap.get("RxErrors").toString()));
//                        deltaMap.put("RxPackets", Long.parseLong(netMap.get("RxPackets").toString()) - Long.parseLong(deltaMap.get("RxPackets").toString()));
//                        deltaMap.put("TxBytes", Long.parseLong(netMap.get("TxBytes").toString()) - Long.parseLong(deltaMap.get("TxBytes").toString()));
//                        deltaMap.put("TxCollisions", Long.parseLong(netMap.get("TxCollisions").toString()) - Long.parseLong(deltaMap.get("TxCollisions").toString()));
//                        deltaMap.put("TxErrors", Long.parseLong(netMap.get("TxErrors").toString()) - Long.parseLong(deltaMap.get("TxErrors").toString()));
//                        deltaMap.put("TxPackets", Long.parseLong(netMap.get("TxPackets").toString()) - Long.parseLong(deltaMap.get("TxPackets").toString()));
//                        netInterfaces.add(deltaMap);
//                        skip = false;
//                    } else {
//                        netInterfaces.add(netMap);
//                        skip = true;
//                    }
//                    previousNetworkStats.put(netIf[i], netMap);
//                }
//            } catch(SigarException se){
//                se.printStackTrace();
////                log.error("SigarException caused during collection of Network utilization");
////                log.error(ExceptionUtils.getStackTrace(se));
//            } finally {
//                map.put("network", netInterfaces);
//            }
//
//            // Filesystem Utilization
//            List fsList = new ArrayList();
//            try {
//                fs = sigar.getFileSystemList();
//                for (int i = 0; i < fs.length; i++) {
//                    FileSystemUsage usage = sigar.getFileSystemUsage(fs[i].getDirName());
//                    Map fsMap = new HashMap();
//                    fsMap.putAll(fs[i].toMap());
//                    fsMap.put("ReadBytes", usage.getDiskReadBytes());
//                    fsMap.put("Reads", usage.getDiskReads());
//                    fsMap.put("WriteBytes", usage.getDiskWriteBytes());
//                    fsMap.put("Writes", usage.getDiskWrites());
//                    if(previousDiskStats.containsKey(fs[i].getDevName())) {
//                        Map deltaMap = previousDiskStats.get(fs[i].getDevName());
//                        deltaMap.put("ReadBytes", usage.getDiskReadBytes() - (Long) deltaMap.get("ReadBytes"));
//                        deltaMap.put("Reads", usage.getDiskReads() - (Long) deltaMap.get("Reads"));
//                        deltaMap.put("WriteBytes", usage.getDiskWriteBytes() - (Long) deltaMap.get("WriteBytes"));
//                        deltaMap.put("Writes", usage.getDiskWrites() - (Long) deltaMap.get("Writes"));
//                        deltaMap.put("Total", usage.getTotal());
//                        deltaMap.put("Used", usage.getUsed());
//                        deltaMap.putAll(fs[i].toMap());
//                        fsList.add(deltaMap);
//                        skip = false;
//                    } else {
//                        fsList.add(fsMap);
//                        skip = true;
//                    }
//                    previousDiskStats.put(fs[i].getDevName(), fsMap);
//                }
//            } catch(SigarException se){
//                se.printStackTrace();
////                log.error("SigarException caused during collection of FileSystem utilization");
////                log.error(ExceptionUtils.getStackTrace(se));
//            } finally {
//                map.put("disk", fsList);
//            }
//            map.put("timestamp", System.currentTimeMillis());
//
//
//            //byte[] data = map.toString().getBytes();
//
////            sendOffset += data.length;
////            ChunkImpl c = new ChunkImpl("SystemMetrics", "Sigar", sendOffset, data, systemMetrics);
////            if(!skip) {
////                receiver.add(c);
////            }
//        } catch (Exception se) {
//            se.printStackTrace();
//            //log.error(ExceptionUtil.getStackTrace(se));
//        }
//    }

}
