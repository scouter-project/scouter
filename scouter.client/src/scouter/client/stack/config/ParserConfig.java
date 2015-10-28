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
package scouter.client.stack.config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import scouter.client.stack.config.preprocessor.Processor;


public class ParserConfig {
    private String m_configFilename = null;
    private String m_parserName = null;
    private int m_stackStartLine = 2;	//Default Stack start line: 2
    private String m_divideStack = null;
    
    private String m_timeFormat = null;
    private SimpleDateFormat m_simpleDateFormat = null;
    private int m_timeSize = 0;
    private String m_timeFilter = null;
    private int m_timePosition = 0;
    private String m_threadStatus = null;
    
    private boolean m_serviceExclude = false;
    
    private ArrayList<String> m_workerThread = new ArrayList<String>();
    private ArrayList<String> m_workingThread = new ArrayList<String>();
    private ArrayList<String> m_sql = new ArrayList<String>();
    private ArrayList<String> m_service = new ArrayList<String>();
    private ArrayList<String> m_log = new ArrayList<String>();
    private ArrayList<String> m_excludeStack = new ArrayList<String>();
    private ArrayList<String> m_singleStack = new ArrayList<String>();
    private JmxConfig m_jmxConfig = null;

    private ArrayList<AnalyzerValue> m_analyzer = null;
    
    private ArrayList<Processor> m_stackPreprocessorList = null;
    private Processor.TARGET m_stackPreprocessorTarget = null;

    public ArrayList<String> getWorkingThread() {
        return m_workingThread;
    }
    
    public void setStackStartLine(int line){
    	m_stackStartLine = line;
    }
    
    public int getStackStartLine(){
    	return m_stackStartLine;
    }

    public void setDivideStack(String divideStack){
    	m_divideStack = divideStack;
    }
    
    public String getDivideStack(){
    	return m_divideStack;
    }
    
    public ArrayList<String> getSql() {
        return m_sql;
    }

    public ArrayList<String> getService() {
        return m_service;
    }
    
    public boolean isServiceExclude(){
    	return m_serviceExclude;
    }

    public ArrayList<String> getLog() {
        return m_log;
    }

    public ArrayList<String> getExcludeStack() {
        return m_excludeStack;
    }

    public ArrayList<String> getSingleStack() {
        return m_singleStack;
    }

    public ArrayList<String> getWorkerThread() {
        return m_workerThread;
    }
    
    public String getThreadStatus(){
    	return m_threadStatus;
    }

    public void setThreadStatus(String status){
    	m_threadStatus = status;
    }
    
    public void setWorkerThread( ArrayList<String> workerThread ) {
        m_workerThread = workerThread;
    }

    public void setWorkingThread( ArrayList<String> workingThread ) {
        m_workingThread = workingThread;
    }

    public void setSql( ArrayList<String> sql ) {
        m_sql = sql;
    }

    public void setService( ArrayList<String> service ) {
        m_service = service;
    }

    public void setServiceExclude(boolean value){
    	m_serviceExclude = value;
    }
    
    public void setLog( ArrayList<String> log ) {
        m_log = log;
    }

    public void setExcludeStack( ArrayList<String> excludeStack ) {
        m_excludeStack = excludeStack;
    }

    public void setSingleStack(ArrayList<String> singleStack ) {
        m_singleStack = singleStack;
    }

    public void setConfigFilename( String filename ) {
        m_configFilename = filename;
    }

    public String getConfigFilename() {
        return m_configFilename;
    }

    public String getParserName() {
        return m_parserName;
    }

    public void setParserName( String parserName ) {
        m_parserName = parserName;
    }

    public String getTimeFormat() {
        return m_timeFormat;
    }

    public void setTimeFormat( String timeFormat ) {
        m_timeFormat = timeFormat;
        if ( m_timeFormat != null )
            m_simpleDateFormat = new SimpleDateFormat(m_timeFormat);
    } 

    public void setStackPreprocessorList(ArrayList<Processor> list){
    	m_stackPreprocessorList = list;
    }
   
    public ArrayList<Processor> getStackPreprocessorList(){
    	return m_stackPreprocessorList;
    }
    
    public void setStackPreprocessorTarget(String target){
    	if(target == null || target.length() == 0){
    		throw new RuntimeException("preProcessor target attribute of preprocessor is not exist!");
    	}
    	
		if(target.equals("header")){
			m_stackPreprocessorTarget = Processor.TARGET.HEADER;
		}else if(target.equals("stack")){
			m_stackPreprocessorTarget = Processor.TARGET.STACK;
		}else if(target.equals("all")){
			m_stackPreprocessorTarget = Processor.TARGET.ALL;
		}else{
			throw new RuntimeException("preProcessor target (header/stack/all) of preprocessor is wrong(" + target + ")!");
		}
    }

    public  Processor.TARGET getStackPreprocessorTarget(){
    	return m_stackPreprocessorTarget;
    }
    
    public void setTimeSize( int size ) {
        m_timeSize = size;
    }

    public int getTimeSize() {
        return m_timeSize;
    }

    public void setTimePosition( int position ) {
        m_timePosition = position;
    }

    public int getTimePosition() {
        return m_timePosition;
    }
    
    public void setTimeFilter( String filter ) {
        m_timeFilter = filter;
    }

    public String getTimeFilter() {
        return m_timeFilter;
    }
        
    public SimpleDateFormat getSimpleDateFormat() {
        return m_simpleDateFormat;
    }

    public void setSimpleDateFormat( SimpleDateFormat format ) {
        m_simpleDateFormat = format;
    }

    public ArrayList<AnalyzerValue> getAnalyzerList() {
        return m_analyzer;
    }

    public boolean addAnalyzer( AnalyzerValue value ) {
        if ( !value.isValid() )
            return false;

        if ( m_analyzer == null )
            m_analyzer = new ArrayList<AnalyzerValue>();

        m_analyzer.add(value);
        return true;
    }

    public JmxConfig getJMXConfig() {
        return m_jmxConfig;
    }

    public void setJMXConfig( int count, int interval, String filePath ) {
        m_jmxConfig = new JmxConfig(count, interval, filePath);
    }
}
