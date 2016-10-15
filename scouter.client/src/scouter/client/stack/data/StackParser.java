/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.client.stack.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import scouter.client.stack.base.MainProcessor;
import scouter.client.stack.base.ProgressBarWindow;
import scouter.client.stack.config.AnalyzerValue;
import scouter.client.stack.config.ParserConfig;
import scouter.client.stack.config.ParserConfigReader;
import scouter.client.stack.config.preprocessor.PreProcessor;
import scouter.client.stack.utils.ResourceUtils;
import scouter.client.stack.utils.StringUtils;


public abstract class StackParser {
    public final static String EXTENSION = "scouter";
    public final static String INFO_EXTENSION = "info";

    public final static String INFO_EXT = "INFO";
    public final static String WORKINGTHREAD_EXT = "WS";
    public final static String TOP_EXT = "TOP";
    public final static String TOP_NAME = "Top Stack ";
    public final static String SQL_EXT = "SQL";
    public final static String SQL_NAME = "SQL Call";
    public final static String SERVICE_EXT = "SVC";
    public final static String SERVICE_NAME = "Service Call";
    public final static String LOG_EXT = "LOG";
    public final static String LOG_NAME = "Logging Call";
    public final static String UNIQUE_EXT = "UNQ";
    public final static String UNIQUE_NAME = "Unique Stack";
    
    private ParserConfig m_config = null;
    private StackFileInfo m_stackFile = null;
    private String m_stackContents = null;
    
    private BufferedWriter m_workingThread_writer = null;

    private ArrayList<String> m_workingThread = null;
    private ArrayList<String> m_sql = null;
    private ArrayList<String> m_service = null;
    private ArrayList<String> m_log = null;
    private ArrayList<String> m_workerThread = null;

    private ArrayList<String> m_topList = null;
    private ArrayList<String> m_sqlList = null;
    private ArrayList<String> m_serviceList = null;
    private ArrayList<String> m_logList = null;
    private ArrayList<String> m_timeList = null;
    private ArrayList<String> m_threadStatusList = null;

    /* Total Count */
    protected int m_totalWorkingCount = 0;
    protected int m_totalWorkerCount = 0;
    protected int m_dumpCount = 0;
    protected int m_totalSecond = 0;

    private ArrayList<AnalyzerValue> m_analyzer = null;
    @SuppressWarnings("rawtypes")
	private ArrayList[] m_analyzerList = null;
    private int m_analyzerCount = 0;

    /* FilterStackParser */
    private String m_filter = null;

    private ProgressBarWindow m_progressBarWindow = null;
    
    // ProgressPar
	private int m_totalLineCount = 0;
	private int m_processPercent = 0; 
	
	// MainProcessor
	private MainProcessor m_mainProcessor = null;

	// Single Stack
	HashMap<Integer, UniqueStackValue> m_uniqueStackMap = null;
	
    protected StackParser() {
    }

    protected StackParser(ParserConfig config) {
        if ( config == null )
            throw new RuntimeException("Configuration is not exist!");
        m_config = config;
    }

    public void setConfig( ParserConfig config ) {
        if ( config == null )
            throw new RuntimeException("Configuration is not exist!");
        m_config = config;
    }

    public ParserConfig getConfig() {
        return m_config;
    }

    public void setFilter( String filter ) {
        m_filter = filter;
    }

    public String getFilter() {
        return m_filter;
    }

    public ArrayList<String> getWorkerThread() {
        return m_workerThread;
    }

    public ArrayList<String> getWorkingThread() {
        return m_workingThread;
    }

    public StackFileInfo getStackFileInfo() {
        return m_stackFile;
    }
    
    public void setStackContents(String contents){
    	m_stackContents = contents;
    }
    
    public String getStackContents(){
    	return m_stackContents;
    }

    private void init() {
        m_workingThread = m_config.getWorkingThread();
        if ( m_workingThread == null || m_workingThread.size() == 0 )
            throw new RuntimeException("WorkingThread infomation must be exist!");

        m_sql = m_config.getSql();
        m_service = m_config.getService();
        m_log = m_config.getLog();
        m_workerThread = m_config.getWorkerThread();
        m_analyzer = m_config.getAnalyzerList();

        m_topList = new ArrayList<String>(2000);
        m_sqlList = new ArrayList<String>(2000);
        m_serviceList = new ArrayList<String>(2000);
        m_logList = new ArrayList<String>(2000);
        m_timeList = new ArrayList<String>(2000);
        m_threadStatusList = new ArrayList<String>();
        m_uniqueStackMap = new HashMap<Integer, UniqueStackValue>(2000);
        
        if ( m_analyzer != null ) {
            m_analyzerCount = m_analyzer.size();
            m_analyzerList = new ArrayList[m_analyzerCount];

            for ( int i = 0; i < m_analyzerCount; i++ ) {
                m_analyzerList[i] = new ArrayList<String>(2000);
            }
        }
    	
        m_totalWorkingCount = 0;

        try {
            m_workingThread_writer = new BufferedWriter(new FileWriter(getWorkingThreadFilename()));
            ;
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }

        
    }

    public void analyze( StackFileInfo stackFile ) {
        m_stackFile = stackFile;
        m_stackFile.clearAll();
        m_stackFile.setUsedParser(this);
        m_stackFile.setParserConfig(m_config);
        m_mainProcessor = MainProcessor.instance();
        
        init();
    	m_progressBarWindow = new ProgressBarWindow(m_mainProcessor.getParentComposite().getShell(), "Stack log file Analyzing");

        try {
            process();
            makeTotalSecond();
        } finally {
            if ( m_workingThread_writer != null ) {
                try {
                    m_workingThread_writer.close();
                } catch ( Exception e ) {
                }
            }

        	m_progressBarWindow.close();
        }

        if ( m_filter != null ) {
            String stackFilename = getNewFilterFilename();
            if(stackFilename != null){
            	m_stackFile.setFilename(stackFilename);            	
            }
        }

        saveAll();
        clearAll();
        m_stackFile = null;
    }
    
    private String getNewFilterFilename(){
        String stackFilename = m_stackFile.getFilename();
        String ext = new StringBuilder(20).append('_').append(WORKINGTHREAD_EXT).append('.').append(EXTENSION).toString();
        int index = stackFilename.indexOf(ext);
        if ( index > 0 ) {
        	if(this instanceof FilterExcludeStackParser){
        		stackFilename = new StringBuilder(200).append(stackFilename.substring(0, index)).append(".(E)").append(m_filter).toString();
        	}else{
        		stackFilename = new StringBuilder(200).append(stackFilename.substring(0, index)).append(".(I)").append(m_filter).toString();
        	}
        	return stackFilename;
        }
        return null;
    }

    protected void clearAll() {
        m_topList = null;
        m_sqlList = null;
        m_serviceList = null;
        m_logList = null;
        m_timeList = null;
        m_threadStatusList = null;
    	m_analyzerList = null;
    	m_uniqueStackMap = null;

        m_totalWorkingCount = 0;
        m_totalWorkerCount = 0;
        m_totalSecond = 0;
        m_dumpCount = 0;
    	m_totalLineCount = 0;
    	m_processPercent = 0;  
    	m_analyzerCount = 0;
   	    	
        m_workingThread = null;
        m_sql = null;
        m_service = null;
        m_log = null;
        m_workerThread = null;
        m_analyzer = null;
    }

    abstract public void process();

    @SuppressWarnings("unchecked")
	private void saveAll() {
        m_stackFile.setDumpCount(m_dumpCount);
        m_stackFile.setTotalWorkerCount(m_totalWorkerCount);
        m_stackFile.setTotalWorkingCount(m_totalWorkingCount);
        m_stackFile.setTotalSecond(m_totalSecond);

        StackAnalyzedInfo info = saveStackAnalyzedInfo(m_topList, TOP_NAME, TOP_EXT);
        if ( info != null )
            m_stackFile.addStackAnalyzedInfo(info);
        info = saveStackAnalyzedInfo(m_sqlList, SQL_NAME, SQL_EXT);
        if ( info != null )
            m_stackFile.addStackAnalyzedInfo(info);
        info = saveStackAnalyzedInfo(m_serviceList, SERVICE_NAME, SERVICE_EXT);
        if ( info != null )
            m_stackFile.addStackAnalyzedInfo(info);
        info = saveStackAnalyzedInfo(m_logList, LOG_NAME, LOG_EXT);
        if ( info != null )
            m_stackFile.addStackAnalyzedInfo(info);        

        for ( int i = 0; i < m_analyzerCount; i++ ) {
            info = saveStackAnalyzedInfo(m_analyzerList[i], m_analyzer.get(i).getName(), m_analyzer.get(i).getExtension());
            if ( info != null )
                m_stackFile.addStackAnalyzedInfo(info);
        }

        //unique Stack
        info = saveUniqueStackAnalyzedInfo(m_uniqueStackMap, UNIQUE_NAME, UNIQUE_EXT);
        if ( info != null )
            m_stackFile.addStackAnalyzedInfo(info);

        if ( m_timeList.size() > 0 ) {
            m_stackFile.setTimeList(m_timeList);
        }
        if ( m_threadStatusList.size() > 0 ) {
            m_stackFile.setThreadStatusList(m_threadStatusList);
        }
        
        saveAnalyzedInfo();
    }

    public String getWorkingThreadFilename() {
        String filename = null;
        if ( m_filter != null ) {
            filename = getNewFilterFilename();
        } else {
            filename = m_stackFile.getFilename();
        }
        return getWorkingThreadFilename(filename);
    }

	private StackAnalyzedInfo saveStackAnalyzedInfo( ArrayList<String> list, String analyzedName, String extension ) {
        if ( list == null || list.size() == 0 )
            return null;
        BufferedWriter writer = null;
        StackAnalyzedInfo aynalyzedInfo = null;
        try {
            writer = new BufferedWriter(new FileWriter(getAnaylzedFilename(m_stackFile.getFilename(), extension)));

            int i = 0;
            int size = list.size();
            HashMap<String, StackAnalyzedValue> searchMap = new HashMap<String, StackAnalyzedValue>(400);
            ArrayList<StackAnalyzedValue> sortList = new ArrayList<StackAnalyzedValue>(400);

            String key = null;
            StackAnalyzedValue value = null;
            for ( i = 0; i < size; i++ ) {
                key = StringUtils.makeStackValue((String)list.get(i), true);
                if ( (value = (StackAnalyzedValue)searchMap.get(key)) == null ) {
                    value = new StackAnalyzedValue(key, 1, 0, 0);
                    searchMap.put(key, value);
                    sortList.add(value);
                } else {
                    value.addCount();
                }
            }
            searchMap = null;
            Collections.sort(sortList, new StackAnalyzedValueComp());

            aynalyzedInfo = new StackAnalyzedInfo(analyzedName, m_stackFile, extension);
            aynalyzedInfo.setTotalCount(size);
            aynalyzedInfo.setAnaylizedList(sortList);

            StringBuilder buffer = new StringBuilder(100);
            buffer.append(m_totalWorkingCount).append('\t').append(size).append('\t').append((int)((10000 * size) / m_totalWorkingCount)).append('\n');
            writer.write(buffer.toString());

            for ( i = 0; i < sortList.size(); i++ ) {
                value = sortList.get(i);
                value.setExtPct((int)((10000 * value.getCount()) / m_totalWorkingCount));
                value.setIntPct((int)((10000 * value.getCount()) / size));

                buffer = new StringBuilder(100);
                buffer.append(value.getCount()).append('\t').append(value.getExtPct()).append('\t').append(value.getIntPct()).append('\t').append(value.getValue()).append('\n');
                writer.write(buffer.toString());
            }
            writer.write("\n");

            for ( i = 0; i < size; i++ ) {
                writer.write((String)list.get(i));
                writer.write("\n");
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( writer != null ) {
                try {
                    writer.close();
                } catch ( Exception e ) {
                }
            }
        }
        return aynalyzedInfo;
    }
	
	private StackAnalyzedInfo saveUniqueStackAnalyzedInfo( HashMap<Integer, UniqueStackValue> map, String analyzedName, String extension ) {
        if ( map == null || map.size() == 0 )
            return null;
        
        BufferedWriter writer = null;
        StackAnalyzedInfo aynalyzedInfo = null;
        try {
            writer = new BufferedWriter(new FileWriter(getAnaylzedFilename(m_stackFile.getFilename(), extension)));

            int i = 0;
            ArrayList<StackAnalyzedValue> sortList = new ArrayList<StackAnalyzedValue>(400);

            UniqueStackValue value = null;

            int totalCount = 0;            
            Iterator<UniqueStackValue> iter = map.values().iterator();
            while(iter.hasNext()){
            	totalCount += iter.next().getCount();
            }            
            
            iter = map.values().iterator();            
            while(iter.hasNext()){
            	value = iter.next();
            	value.setExtPct((int)((10000 * value.getCount()) / m_totalWorkingCount));
                value.setIntPct((int)((10000 * value.getCount()) / totalCount));
            	sortList.add(value);
            }
            Collections.sort(sortList, new StackAnalyzedValueComp());

            aynalyzedInfo = new StackAnalyzedInfo(analyzedName, m_stackFile, extension);
            aynalyzedInfo.setTotalCount(totalCount);
            aynalyzedInfo.setAnaylizedList(sortList);

            StringBuilder buffer = new StringBuilder(100);
            buffer.append(m_totalWorkingCount).append('\t').append(totalCount).append('\t').append((int)((10000 * totalCount) / m_totalWorkingCount)).append('\n');
            writer.write(buffer.toString());

            for ( i = 0; i < sortList.size(); i++ ) {
                value = (UniqueStackValue)sortList.get(i);
                buffer = new StringBuilder(100);
                buffer.append(value.getCount()).append('\t').append(value.getExtPct()).append('\t').append(value.getIntPct()).append('\t').append(value.getValue()).append('\n');
                writer.write(buffer.toString());
            }
            writer.write("\n");

            for ( i = 0; i < sortList.size(); i++ ) {
            	writer.write("[[]]"+i +"\n");
                value = (UniqueStackValue)sortList.get(i);
                for(String stack : value.getStack()){
                	writer.write(stack);
                	writer.write("\n");
                }
                writer.write("\n");
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( writer != null ) {
                try {
                    writer.close();
                } catch ( Exception e ) {
                }
            }
        }
        return aynalyzedInfo;
    }

    protected void processStack( ArrayList<String> workingList, ThreadStatusInfo tsInfo ) {
    	PreProcessor.process(m_config, workingList);
        processStackBasic(workingList, tsInfo);
        if ( m_analyzerCount > 0 ) {
            processStackAnalyzer(workingList);
        }
    }

    @SuppressWarnings("unchecked")
	private void processStackAnalyzer( ArrayList<String> workingList ) {
        AnalyzerValue analyzer = null;
        for ( int i = 0; i < m_analyzerCount; i++ ) {
            analyzer = m_analyzer.get(i);
            if ( analyzer.getFilter() == AnalyzerValue.FILTER_ALL )
                processStackAnalyzerAll(analyzer, m_analyzerList[i], workingList);
            else if ( analyzer.getFilter() == AnalyzerValue.FILTER_EACH )
                processStackAnalyzerEach(analyzer, m_analyzerList[i], workingList);
        }

    }

    private void processStackAnalyzerEach( AnalyzerValue analyzer, ArrayList<String> list, ArrayList<String> workingList ) {
        int workingSize = workingList.size();
        String line = null;
        ArrayList<String> searchList = analyzer.getList();
        int reader = analyzer.getReader();

        boolean isSearch = false;
        String searchLine = null;
        
        int startStackLine = m_config.getStackStartLine();

        for ( int i = startStackLine; i < workingSize; i++ ) {
            line = workingList.get(i);

            if ( StringUtils.isLockStack(line) )
                continue;

            if ( StringUtils.checkExist(line, searchList) ) {
                if ( reader == AnalyzerValue.READER_FIRST ) {
                    searchLine = line;
                    break;
                } else if ( reader == AnalyzerValue.READER_LAST ) {
                    searchLine = line;
                } else if ( reader == AnalyzerValue.READER_NEXT ) {
                    isSearch = true;
                }
            } else if ( isSearch ) {
                searchLine = line;
                break;
            }
        }

        if ( searchLine != null ) {
            list.add(searchLine);
        }
    }

    private void processStackAnalyzerAll( AnalyzerValue analyzer, ArrayList<String> list, ArrayList<String> workingList ) {
        int workingSize = workingList.size();
        String line = null;
        ArrayList<String> searchList = analyzer.getList();
        ArrayList<String> searchListMain = analyzer.getListMain();
        int reader = analyzer.getReader();

        boolean isSearch = false;
        String searchLine = null;

        int stackStartLine = m_config.getStackStartLine();
        for ( int i = stackStartLine; i < workingSize; i++ ) {
            line = workingList.get(i);

            if ( StringUtils.isLockStack(line) )
                continue;

            if ( !StringUtils.checkExist(line, searchList) ) {
                if ( StringUtils.checkExist(line, searchListMain) ) {
                    if ( reader == AnalyzerValue.READER_FIRST ) {
                        searchLine = line;
                        break;
                    } else if ( reader == AnalyzerValue.READER_LAST ) {
                        searchLine = line;
                    } else if ( reader == AnalyzerValue.READER_NEXT ) {
                        isSearch = true;
                    }
                } else {
                    if ( isSearch ) {
                        searchLine = line;
                        break;
                    }

                    break;
                }
            }
        }

        if ( searchLine != null ) {
            list.add(searchLine);
        }
    }

    private void processStackBasic( ArrayList<String> workingList, ThreadStatusInfo tsInfo ) {
        m_totalWorkingCount++;
        boolean isSql = false;
        String line = null;
        String requestLine = null;
        String sqlLine = null;
        String logLine = null;

        boolean isServiceExclude = m_config.isServiceExclude();
        int stackStartLine = m_config.getStackStartLine();
    	String threadStatus = m_config.getThreadStatus();
    	int threadStatusLength = 0;    	
    	if(threadStatus != null){
    		threadStatusLength = threadStatus.length();
    	}
    	
        try {
            int workingSize = workingList.size();
            for ( int i = 0; i < workingSize; i++ ) {           	
                line = (String)workingList.get(i);

                m_workingThread_writer.write(line);
                m_workingThread_writer.write("\n");
                
		    	//Thread status count
	    		if(i < stackStartLine){
	    			if(threadStatusLength > 0){
		    			int tIndex = line.indexOf(threadStatus);
		    			if(tIndex >= 0){
		    				tsInfo.checkStatusCount(m_threadStatusList, tIndex + threadStatusLength, line);
		    			}
	    			}
	    			continue;
				}
	    		
                if ( i == stackStartLine ) {
                    m_topList.add(line);
                }

                if ( StringUtils.isLockStack(line)){
                    continue;
                }
                
                // SQL
                if ( StringUtils.checkExist(line, m_sql) ) {
                    isSql = true;
                } else if ( isSql ) {
                    isSql = false;
                    sqlLine = line;
                }

                if (!isServiceExclude && StringUtils.checkExist(line, m_service) )
                    requestLine = line;

                if ( StringUtils.checkExist(line, m_log) ) {
                    logLine = line;
                }
            }
            m_workingThread_writer.write("\n");
            
            if ( requestLine == null ) {
                if ( workingList.size() > stackStartLine ) {
                	if(isServiceExclude){
                		requestLine = getReqeustLineByAuto(workingList, stackStartLine);
                	}
                }
                if(requestLine == null){
                	requestLine = workingList.get(stackStartLine);
                }
            }
            
            if(requestLine != null){
                m_serviceList.add(requestLine);
            }
            
            if(logLine != null ) {
                m_logList.add(logLine);
            }
            
            if(sqlLine != null) {
            	m_sqlList.add(sqlLine);
            }
            
            // unique Stack
            ArrayList<String> simpleList = StringUtils.makeStackToSimpe(workingList, stackStartLine, m_config.getSingleStack());
            int hashCode = StringUtils.hashCode(simpleList);
            UniqueStackValue uniqueStack = m_uniqueStackMap.get(hashCode);
            if(uniqueStack == null){
            	uniqueStack = new UniqueStackValue(simpleList);
            	m_uniqueStackMap.put(hashCode, uniqueStack);
            }
            uniqueStack.addCount();
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }

	private String getReqeustLineByAuto(ArrayList<String> workingList, int stackStartLine){
		String line;
		int size = workingList.size();
		for(int i = (size - 1); i >= stackStartLine ;i--){
			line = workingList.get(i);
			
			if( StringUtils.checkExist(line, m_service)){
				continue;
			}else{
				return line;				
			}
		}
		return null;
	}
   
    protected void addTime( String time ) {
        m_timeList.add(time);
    }

    protected void writeTime( String time ) {
        try {
            m_workingThread_writer.write(time);
            m_workingThread_writer.write("\n\n");
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    protected ArrayList<String> getThreadStatusList(){
    	return m_threadStatusList;
	}   

    protected void setTotalWorkerCount( int count ) {
        m_totalWorkerCount = count;
    }

    protected void setDumpCount( int count ) {
        m_dumpCount = count;
    }

    private void makeTotalSecond() {
        int size = m_timeList.size();
        if ( size < 2 ) {
            m_totalSecond = 0;
            return;
        }
        SimpleDateFormat format = m_config.getSimpleDateFormat();
        if ( format == null ) {
            m_totalSecond = 0;
            return;
        }

        Date start, end;
        try {
            start = format.parse(m_timeList.get(0).substring(0, m_config.getTimeSize()));
            end = format.parse(m_timeList.get(m_timeList.size() - 1).substring(0, m_config.getTimeSize()));
            m_totalSecond = (int)((end.getTime() - start.getTime()) / 1000);
        } catch ( Exception ex ) {
            ex.printStackTrace();
            m_totalSecond = 0;
        }
    }

    private void saveAnalyzedInfo() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getAnaylzedInfoFilename(m_stackFile.getFilename())));

            writer.write(m_config.getConfigFilename());
            writer.write('\n');
            writer.write("" + m_dumpCount);
            writer.write('\t');
            writer.write("" + m_totalWorkerCount);
            writer.write('\t');
            writer.write("" + m_totalWorkingCount);
            writer.write('\t');
            writer.write("" + m_totalSecond);

            int i;
            if(m_threadStatusList.size() > 0){
            	for(i = 0; i < m_threadStatusList.size(); i++){
                    writer.write('\t');
                    writer.write(m_threadStatusList.get(i));            		
            	}
            }
            
            writer.write('\n');
            
            int size = m_timeList.size();

            for ( i = 0; i < size; i++ ) {
                writer.write((String)m_timeList.get(i));
                writer.write('\n');
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( writer != null ) {
                try {
                    writer.close();
                } catch ( Exception e ) {
                }
            }
        }
    }

    protected void progressBar(){
		m_totalLineCount++;
		if(m_totalLineCount % 3000 == 0){
			m_processPercent++;
			if(m_processPercent == 100){
				m_processPercent = 0;
			}
	        m_progressBarWindow.setValue(m_processPercent);
		}
    }
    
    protected boolean isWorkingThread(String line){
    	if(m_mainProcessor.isAnalyzeAllThreads()){
    		return true;
    	}
    	if(StringUtils.checkExist(line, m_workingThread)){
    		return true;
    	}
    	return false;
    }
    
    static public StackFileInfo loadAnalyzedInfo( String filename ) {
        String endString = new StringBuilder(20).append(StackParser.INFO_EXT).append('.').append(INFO_EXTENSION).toString();
        if ( !filename.endsWith(endString) )
            throw new RuntimeException(filename + " is not a Scouter analyzed info file!");

        String stackFilename = filename.substring(0, filename.indexOf(endString) - 1);

        StackFileInfo stackFileInfo = new StackFileInfo(stackFilename);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(filename)));

            String line = null;
            int lineCount = 0;
            ArrayList<String> timeList = new ArrayList<String>();
            ArrayList<String> threadStatusList = new ArrayList<String>();

            while ( (line = reader.readLine()) != null ) {
                line = line.trim();
                if ( line.length() == 0 )
                    continue;
                if ( lineCount == 0 ) {
                    ParserConfigReader ConfigReader = new ParserConfigReader(line);
                    ParserConfig config = ConfigReader.read();
                    stackFileInfo.setParserConfig(config);
                    StackParser parser = StackParser.getParser(config, null, true);
                    stackFileInfo.setUsedParser(parser);
                } else if ( lineCount == 1 ) {
                    StringTokenizer token = new StringTokenizer(line, "\t");
                    String value = null;
                    int index = 0;
                    while ( token.hasMoreElements() ) {
                        value = token.nextToken();
                        if ( index == 0 ) {
                            stackFileInfo.setDumpCount(Integer.parseInt(value));
                        } else if ( index == 1 ) {
                            stackFileInfo.setTotalWorkerCount(Integer.parseInt(value));
                        } else if ( index == 2 ) {
                            stackFileInfo.setTotalWorkingCount(Integer.parseInt(value));
                        } else if ( index == 3 ) {
                            stackFileInfo.setTotalSecond(Integer.parseInt(value));
                        }else{
                        	threadStatusList.add(value);
                        }
                        index++;
                    }
                } else {
                    timeList.add(line);
                }
                lineCount++;
            }
            if ( timeList.size() > 0 ) {
                stackFileInfo.setTimeList(timeList);
            }
            if(threadStatusList.size() > 0){
            	stackFileInfo.setThreadStatusList(threadStatusList);
            }
            
            loadStackAnalyzedInfo(stackFileInfo);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if ( reader != null )
                    reader.close();
            } catch ( Exception ex ) {
            }
        }
        return stackFileInfo;
    }

    static private void loadStackAnalyzedInfo( StackFileInfo stackFileInfo ) {
        File file = null;
        StackAnalyzedInfo analyzedInfo = null;
        try {
            analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, StackParser.TOP_NAME, StackParser.TOP_EXT);
            if ( analyzedInfo != null )
                stackFileInfo.addStackAnalyzedInfo(analyzedInfo);

            analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, StackParser.SQL_NAME, StackParser.SQL_EXT);
            if ( analyzedInfo != null )
                stackFileInfo.addStackAnalyzedInfo(analyzedInfo);

            analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, StackParser.SERVICE_NAME, StackParser.SERVICE_EXT);
            if ( analyzedInfo != null )
                stackFileInfo.addStackAnalyzedInfo(analyzedInfo);

            analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, StackParser.LOG_NAME, StackParser.LOG_EXT);
            if ( analyzedInfo != null )
                stackFileInfo.addStackAnalyzedInfo(analyzedInfo);

            ArrayList<AnalyzerValue> list = stackFileInfo.getParserConfig().getAnalyzerList();
            if ( list != null ) {
                for ( int i = 0; i < list.size(); i++ ) {
                    analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, list.get(i).getName(), list.get(i).getExtension());
                    if ( analyzedInfo != null )
                        stackFileInfo.addStackAnalyzedInfo(analyzedInfo);
                }
            }

            analyzedInfo = readStackAnalyzedInfo(stackFileInfo, file, StackParser.UNIQUE_NAME, StackParser.UNIQUE_EXT);
            readUniqueStackExtraInfo(analyzedInfo, stackFileInfo, file, StackParser.UNIQUE_NAME, StackParser.UNIQUE_EXT);
            if ( analyzedInfo != null )
                stackFileInfo.addStackAnalyzedInfo(analyzedInfo);            
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }

    static private StackAnalyzedInfo readStackAnalyzedInfo( StackFileInfo stackFileInfo, File file, String name, String extension ) {
        String filename = stackFileInfo.getFilename();
        String analyzedFilename = StackParser.getAnaylzedFilename(filename, extension);
        file = new File(analyzedFilename);
        if ( !file.isFile() )
            return null;

        BufferedReader reader = null;
        StackAnalyzedInfo analyzedInfo = new StackAnalyzedInfo(name, stackFileInfo, extension);
        ArrayList<StackAnalyzedValue> list = new ArrayList<StackAnalyzedValue>();

        try {
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            int lineCount = 0;

            StringTokenizer token = null;
            int index;
            String value;

            int count;
            int extPct;
            int intPct;
            String keyValue;
            StackAnalyzedValue analyzedValue;
            boolean isUniqueStack = (name.equals(StackParser.UNIQUE_NAME))?true:false;
            
            while ( (line = reader.readLine()) != null ) {
                line = line.trim();
                if ( line.length() == 0 )
                    continue;

                if ( lineCount == 0 ) {
                    token = new StringTokenizer(line, "\t");
                    index = 0;

                    while ( token.hasMoreElements() ) {
                        value = token.nextToken();
                        if ( index == 0 ) {
                            // skip total working count total count
                        } else if ( index == 1 ) {
                            analyzedInfo.setTotalCount(Integer.parseInt(value));
                        } else if ( index == 2 ) {
                            // skip percentage;
                        }
                        index++;
                    }
                } else {
                    char ch = line.charAt(0);
                    if ( ch >= '0' && ch <= '9' ) {
                        count = 0;
                        extPct = 0;
                        intPct = 0;
                        keyValue = null;

                        token = new StringTokenizer(line, "\t");
                        index = 0;

                        while ( token.hasMoreElements() ) {
                            value = token.nextToken();
                            if ( index == 0 ) {
                                count = Integer.parseInt(value);
                            } else if ( index == 1 ) {
                                extPct = Integer.parseInt(value);
                            } else if ( index == 2 ) {
                                intPct = Integer.parseInt(value);
                            } else if ( index == 3 ) {
                                keyValue = value;
                            }
                            index++;
                        }
                        if ( index == 4 ) {
                        	if(isUniqueStack){
                        		analyzedValue = new UniqueStackValue(keyValue, count, intPct, extPct);                        		
                        	}else{
                        		analyzedValue = new StackAnalyzedValue(keyValue, count, intPct, extPct);
                        	}
                            list.add(analyzedValue);
                        }
                    } else {
                        break;
                    }
                }
                lineCount++;
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if ( reader != null )
                    reader.close();
            } catch ( Exception ex ) {
            }
        }

        analyzedInfo.setAnaylizedList(list);

        return analyzedInfo;
    }

    static private StackAnalyzedInfo readUniqueStackExtraInfo(StackAnalyzedInfo analyzedInfo, StackFileInfo stackFileInfo, File file, String name, String extension ) {
        String filename = stackFileInfo.getFilename();
        String analyzedFilename = StackParser.getAnaylzedFilename(filename, extension);
        file = new File(analyzedFilename);
        if ( !file.isFile() )
            return null;

        BufferedReader reader = null;
        ArrayList<StackAnalyzedValue> list = analyzedInfo.getAnalyzedList();
        try {
            reader = new BufferedReader(new FileReader(file));

            String line;
            int indexNum = 0;
            boolean isStack = false;
            ArrayList<String> stackList = null;
            while ( (line = reader.readLine()) != null ) {
                line = line.trim();
                if ( line.length() == 0 )
                    continue;
                
                if(!isStack && line.startsWith("[[]]")){
                	isStack = true;
                }
                
                if(!isStack){
                	continue;
                }
                
                if(line.startsWith("[[]]")){
                	if(stackList != null && stackList.size() > 0){
                		((UniqueStackValue)list.get(indexNum)).setStack(stackList);
                	}
                	indexNum = Integer.parseInt(line.substring(4));
                	stackList = new ArrayList<String>();
                }else{
                	stackList.add(line);
                }
            }
        	if(stackList != null && stackList.size() > 0){
        		((UniqueStackValue)list.get(indexNum)).setStack(stackList);
        	}            
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if ( reader != null )
                    reader.close();
            } catch ( Exception ex ) {
            }
        }

        return analyzedInfo;
    }
    
    static public StackParser getParser( ParserConfig config, String filter, boolean isInclude ) {
        StackParser parser = null;
        if ( filter != null ) {
            if ( isInclude ) {
                parser = new FilterStackParser();
            } else {
                parser = new FilterExcludeStackParser();
            }
            parser.setFilter(filter);
        } else {
            String parserName = config.getParserName();
            if ( parserName == null ) {
                parser = new BasicFileStackParser();
            } else {
                if ( parserName.indexOf(',') < 0 ) {
                    parserName = "scouter.client.stack.data." + parserName;
                }

                try {
                    @SuppressWarnings("rawtypes")
					Class cl = Class.forName(parserName);
                    parser = (StackParser)cl.newInstance();
                } catch ( Exception ex ) {
                    throw new RuntimeException(ex);
                }
            }
        }
        parser.setConfig(config);
        return parser;
    }

    static public String getAnaylzedFilename( String filename, String extension ) {
        return new StringBuffer(200).append(filename).append('_').append(extension).append('.').append(EXTENSION).toString();
    }

    static public String getWorkingThreadFilename( String filename ) {
        return getAnaylzedFilename(filename, WORKINGTHREAD_EXT);
    }

    static public String getAnaylzedInfoFilename( String filename ) {
        return new StringBuilder(200).append(filename).append('_').append(INFO_EXT).append('.').append(INFO_EXTENSION).toString();
    }

    static public void removeAllAnalyzedFile( StackFileInfo stackFileInfo ) {
        String filename = stackFileInfo.getFilename();
        if ( filename != null ) {
        	ResourceUtils.removeFile(getAnaylzedInfoFilename(filename));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, WORKINGTHREAD_EXT));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, SERVICE_EXT));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, SQL_EXT));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, TOP_EXT));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, LOG_EXT));
            ResourceUtils.removeFile(getAnaylzedFilename(filename, UNIQUE_EXT));
        }

        ParserConfig config = stackFileInfo.getParserConfig();
        if ( config != null ) {
            ArrayList<AnalyzerValue> list = config.getAnalyzerList();
            if ( list != null ) {
                for ( int i = 0; i < list.size(); i++ ) {
                	ResourceUtils.removeFile(getAnaylzedFilename(filename, list.get(i).getExtension()));
                }
            }
        }
    }
}