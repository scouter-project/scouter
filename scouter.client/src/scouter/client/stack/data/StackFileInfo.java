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

import java.util.ArrayList;

import scouter.client.stack.config.ParserConfig;


@SuppressWarnings("serial")
public class StackFileInfo extends AbstractInfo {
    private String m_filename;
    private StackParser m_usedParser;
    private ParserConfig m_parserConfig;

    private int m_dumpCount = 0;
    private int m_totalWorkerCount = 0;
    private int m_totalWorkingCount = 0;
    private int m_totalSecond = 0;

    private ArrayList<String> m_timeList = null;
    private ArrayList<StackAnalyzedInfo> m_stackAnalyzedInfoList = null;
    private ArrayList<String> m_threadStatusList = new ArrayList<String>();

    public StackFileInfo(String value) {
        m_filename = value;
        setName("StackFile");
    }

    public String getFilename() {
        return m_filename;
    }

    public void setFilename( String filename ) {
        m_filename = filename;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder(200);
        buffer.append(m_filename).append(" - ").append(m_totalWorkingCount);
        if(m_totalWorkerCount > 0){
        	buffer.append(" (").append(m_totalWorkerCount).append(')');
        }
        if(m_dumpCount > 0){
        	buffer.append(" - ").append(m_dumpCount).append("dump");
        }
        return buffer.toString();
    }

    public String [] toTreeInfo() {
    	String [] info = new String[3];
    	info[0] = m_filename;
    	StringBuilder buffer =  new StringBuilder().append(m_totalWorkingCount);
        if(m_totalWorkerCount > 0){
        	buffer.append(" (").append(m_totalWorkerCount).append(')');
        }
        info[1] = buffer.toString();
        info[2] = new StringBuffer().append(m_dumpCount).append(" dump").toString();
        return info;
    }

    
    public StackParser getUsedParser() {
        return m_usedParser;
    }

    public void setUsedParser( StackParser parser ) {
        if ( m_usedParser == null ) {
            m_usedParser = parser;
        }
    }

    public int getDumpCount() {
        return m_dumpCount;
    }

    public void setDumpCount( int value ) {
        m_dumpCount = value;
    }

    public int getTotalWorkingCount() {
        return m_totalWorkingCount;
    }

    public void setTotalWorkingCount( int value ) {
        m_totalWorkingCount = value;
    }

    public int getTotalWorkerCount() {
        return m_totalWorkerCount;
    }

    public void setTotalWorkerCount( int value ) {
        m_totalWorkerCount = value;
    }

    public void setTotalSecond( int seconds ) {
        m_totalSecond = seconds;
    }

    public int getTotalSecond() {
        return m_totalSecond;
    }

    public StackAnalyzedInfo getStackAnalyzedInfo( int i ) {
        if ( m_stackAnalyzedInfoList == null )
            return null;

        return m_stackAnalyzedInfoList.get(i);
    }

    public ArrayList<StackAnalyzedInfo> getStackAnalyzedInfoList() {
        return m_stackAnalyzedInfoList;
    }

    public void addStackAnalyzedInfo( StackAnalyzedInfo info ) {
        if ( m_stackAnalyzedInfoList == null )
            m_stackAnalyzedInfoList = new ArrayList<StackAnalyzedInfo>();

        m_stackAnalyzedInfoList.add(info);
    }

    public void setParserConfig( ParserConfig config ) {
        m_parserConfig = config;
    }

    public ParserConfig getParserConfig() {
        return m_parserConfig;
    }

    public void clearAll() {
        if ( m_stackAnalyzedInfoList != null )
            m_stackAnalyzedInfoList.clear();

        m_dumpCount = 0;
        m_totalWorkerCount = 0;
        m_totalWorkingCount = 0;
        m_totalSecond = 0;
    }

    public void setTimeList( ArrayList<String> list ) {
        m_timeList = list;
    }

    public ArrayList<String> getTimeList() {
        return m_timeList;
    }
    
    public void setThreadStatusList(ArrayList<String> list){
    	m_threadStatusList = list;
    }
    
    public ArrayList<String> getThreadStatusList(){
    	return m_threadStatusList;
    }
}