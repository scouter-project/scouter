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
package scouter.client.stack.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import scouter.client.stack.base.MainProcessor;
import scouter.client.stack.base.PreferenceManager;
import scouter.client.stack.data.StackFileInfo;


public class HtmlUtils {
    static public final int HTML_MAX_LINES = 20000;

    public static String getAsTable( String prefix, String[] elements ) {
        StringBuffer result = new StringBuffer();
        // int from = elements.length > 4 ? elements.length - 4 : 0;
        int from = 0;

        for ( int i = from; i < elements.length; i++ ) {
            if ( elements[i].trim().length() > 0 ) {
                // remove backslashes as they confuse the html display.
                String elem = elements[i].replaceAll("\\\\", "/");
                result.append("<tr><td width=\"20px\"></td><td><a href=\"");
                result.append(prefix);
                result.append(elem);
                result.append("\">");
                result.append(cutLink(elem, 80));
                result.append("</a></td></tr>\n");
            }
        }

        return (result.toString());
    }

    public static String cutLink( String link, int len ) {
        if ( link.length() > len ) {
            String cut = link.substring(0, len / 2) +
                    "..." + link.substring(link.length() - (len / 2));
            return (cut);
        }

        return (link);
    }

    public static String getMainBodyStart() {
        return "<TABLE width='1500'><TR><TD width='50'></TD><TD>";
    }

    public static String getMainBodyEnd() {
        return "</TD></TR></TABLE>";
    }

    public static String getStackFileInfo( StackFileInfo stackFileInfo ) {
        StringBuffer buffer = new StringBuffer(1024000);
        buffer.append(getMainBodyStart());
        buffer.append(getCurrentConfigurationBody()).append("<br><br>");
        buffer.append("<b>[ ").append(stackFileInfo.getFilename()).append(" ]</b><BR>");
        buffer.append("Parser configuration filename : ").append(stackFileInfo.getParserConfig().getConfigFilename()).append("<br>");
        buffer.append("Dump count : ").append(NumberUtils.intToString(stackFileInfo.getDumpCount())).append("<BR>");
        buffer.append("Worker count : ").append(NumberUtils.intToString(stackFileInfo.getTotalWorkerCount())).append("<BR>");
        buffer.append("Working count : ").append(NumberUtils.intToString(stackFileInfo.getTotalWorkingCount())).append("<BR>");
        ArrayList<String> list = stackFileInfo.getTimeList();
        if ( list == null || list.size() == 0 ) {
            return buffer.toString();
        }
        int size = list.size();
        if ( stackFileInfo.getTotalSecond() > 0 ) {
            buffer.append("Monitoring seconds : ").append(NumberUtils.secsToTime(stackFileInfo.getTotalSecond())).append(" -> ");
            buffer.append(NumberUtils.intToString(stackFileInfo.getTotalSecond())).append(" secs ");
        }

        if ( list != null && size > 1 ) {
            buffer.append('(').append(list.get(0).substring(0, stackFileInfo.getParserConfig().getTimeSize())).append(" - ")
                    .append(list.get(size - 1).substring(0, stackFileInfo.getParserConfig().getTimeSize())).append(')');
        }
        boolean isSimpleDumpList = MainProcessor.instance().isSimpleDumpTimeList();
        buffer.append("<BR><BR>");
        if ( isSimpleDumpList ) {
            buffer.append("<b>[ Simple dump time list ]</b><BR>");
        } else {
            buffer.append("<b>[ Dump time list ]</b><BR>");
        }
        buffer.append("<TABLE border='1'>");
        buffer.append("<TR><TH align='center'>Time</TH><TH align='center'>Worker</TH><TH align='center'>Working</TH>");
        
        ArrayList<String> statusList = stackFileInfo.getThreadStatusList();
        int statusCnt = 0;
        if(statusList != null){
        	statusCnt = statusList.size();
        	for(int i = 0; i < statusCnt ; i++){
                buffer.append("<TH align='center'>").append(statusList.get(i)).append("</TH>");
        	}
        }        
        buffer.append("</TR>");

        if ( isSimpleDumpList ) {
            int workerCnt = 0;
            int workingCnt = 0;
            int [] threadStatusList = null;
            if(statusCnt > 0){
            	threadStatusList = new int[statusCnt];
            	for(int i= 0; i < statusCnt; i++){
            		threadStatusList[i] = 0;
            	}
            }
            
            StringTokenizer token = null;
            int index, value;
            for ( int i = 0; i < size; i++ ) {
                token = new StringTokenizer(list.get(i), "\t");
                index = 0;

                while ( token.hasMoreElements() ) {
                    if ( index > 0 ) {
                        try {
                            value = Integer.parseInt((String)token.nextElement());
                        } catch ( Exception ex ) {
                            ex.printStackTrace();
                            value = 0;
                        }
                        switch ( index ) {
                        case 1:
                            workerCnt += value;
                            break;
                        case 2:
                            workingCnt += value;
                            break;
                        default:
                        	if(index > 2){
                        		threadStatusList[index - 3] += value;
                        	}
                        	break;
                        }
                    } else {
                        token.nextElement();
                    }
                    index++;
                }
            }
            buffer.append("<TR>");
            buffer.append("<TD  align='center'>").append(size).append(" Average (Sum)</TD>");
            buffer.append("<TD  align='right'>").append((workerCnt / size)).append("  (").append(NumberUtils.intToString(workerCnt)).append(")</TD>");
            buffer.append("<TD  align='right'>").append((workingCnt / size)).append("  (").append(NumberUtils.intToString(workingCnt)).append(")</TD>");
            for(int i= 0; i < statusCnt; i++){
            	buffer.append("<TD  align='right'>").append((threadStatusList[i] / size)).append("  (").append(NumberUtils.intToString(threadStatusList[i])).append(")</TD>");
            }
            buffer.append("</TR>");
        } else {
            StringTokenizer token = null;
            int index;
            for ( int i = 0; i < size; i++ ) {
                token = new StringTokenizer(list.get(i), "\t");
                buffer.append("<TR>");
                index = 0;
                while ( token.hasMoreElements() ) {
                    if ( index == 0 ) {
                        buffer.append("<TD  align='center'>");
                    } else {
                        buffer.append("<TD  align='right'>");
                    }
                    buffer.append(token.nextElement());
                    buffer.append("</TD>");
                    index++;
                }

                buffer.append("</TR>");

            }
        }
        buffer.append("</TABLE>");
        buffer.append(getMainBodyEnd());
        return buffer.toString();
    }

    static public String getDefaultBody() {
        StringBuilder buffer = new StringBuilder(102400);
        buffer.append(getMainBodyStart());
        buffer.append(getCurrentConfigurationBody());
        buffer.append(getMainBodyEnd());
        return buffer.toString();
    }
    
    static public String getCurrentConfigurationBody(){
    	String fileName = PreferenceManager.get().getCurrentParserConfig();
    	StringBuilder buffer = new StringBuilder(100);
        buffer.append("Current parser configuration filename : ");
        if(fileName != null){
        	buffer.append(fileName);
        }
    	return buffer.toString();
    }

    static public String filterThreadStack( String filename, String filter, ArrayList<String> excludeStackList, int stackStartLine ) {
        if ( filename == null )
            return "";

        StringBuilder buffer = new StringBuilder(1024000);
        buffer.append(getMainBodyStart());
        buffer.append("<b>[ View thread stack ]</b><BR>");
        buffer.append("Filter : ").append(filter).append("<BR>");
        buffer.append("<pre><font size=3>");
        BufferedReader reader = null;
        int totalLineCount = 0;
        try {
            File file = new File(filename);
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            boolean isFiltered = false;

            ArrayList<String> list = new ArrayList<String>(300);
            while ( (line = reader.readLine()) != null ) {
                if ( line.length() == 0 ) {
                    if ( isFiltered && list.size() > stackStartLine ) {
                        totalLineCount += printStack(list, buffer, excludeStackList);
                    }

                    list = new ArrayList<String>(300);
                    isFiltered = false;
                    if ( totalLineCount >= HTML_MAX_LINES )
                        break;
                } else {
                    if ( !isFiltered ) {
                        if ( filter == null ) {
                            isFiltered = true;
                        } else if ( line.indexOf(filter) >= 0 ) {
                            isFiltered = true;
                        }
                    }
                    list.add(line);
                }
            }
            if ( isFiltered && list.size() > stackStartLine ) {
                totalLineCount += printStack(list, buffer, excludeStackList);
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( reader != null ) {
                try {
                    reader.close();
                } catch ( Exception e ) {
                }
            }
        }
        buffer.append("</font></pre>");
        buffer.append("Total : ").append(NumberUtils.intToString(totalLineCount)).append(" lines <BR>");
        buffer.append(getMainBodyEnd());
        return buffer.toString();
    }

    static private int printStack( ArrayList<String> list, StringBuilder buffer, ArrayList<String> excludeStackList ) {
        int i;
        int size = list.size();

        if ( excludeStackList == null ) {
            for ( i = 0; i < size; i++ ) {
                buffer.append(list.get(i)).append('\n');
            }
            buffer.append('\n');
            return size + 1;

        } else {
            int lines = 0;
            String line;

            for ( i = 0; i < size; i++ ) {
                line = list.get(i);
                if ( !StringUtils.checkExist(line, excludeStackList) ) {
                    buffer.append(line).append('\n');
                    lines++;
                }
            }
            buffer.append('\n');
            return lines + 1;
        }
    }

    static public String filterServiceCall( String filename, String filter, ArrayList<String> serviceList, int stackStartLine ) {
        if ( filename == null )
            return "";

        BufferedReader reader = null;

        String serviceCall = null;
        int totalServiceCount = 0;
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        try {
            File file = new File(filename);
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            boolean isFiltered = false;
            int lineCount = 0;

            while ( (line = reader.readLine()) != null ) {
                if ( line.length() == 0 ) {
                    if ( isFiltered && lineCount > stackStartLine ) {
                        totalServiceCount++;
                        caculCounter(serviceCall, map);
                    }

                    isFiltered = false;
                    serviceCall = null;
                    lineCount = 0;
                } else {
                    if ( !isFiltered ) {
                        if ( filter == null ) {
                            isFiltered = true;
                        } else if ( line.indexOf(filter) >= 0 ) {
                            isFiltered = true;
                        }
                    }
                    if ( lineCount == stackStartLine || StringUtils.checkExist(line, serviceList) ) {
                        serviceCall = line;
                    }
                    lineCount++;
                }
            }
            if ( isFiltered && lineCount > stackStartLine ) {
                totalServiceCount++;
                caculCounter(serviceCall, map);
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( reader != null ) {
                try {
                    reader.close();
                } catch ( Exception e ) {
                }
            }
        }

        StringBuilder buffer = new StringBuilder(10240);
        buffer.append(getMainBodyStart());
        buffer.append("<b>[ View service call ]</b><BR>");
        buffer.append("Filter : ").append(filter).append("<BR>");
        buffer.append("Total count : ").append(NumberUtils.intToString(totalServiceCount)).append("<BR><BR>");

        if ( totalServiceCount == 0 ) {
            buffer.append(getMainBodyEnd());
            return buffer.toString();
        }
        buffer.append("<TABLE border='1'>");
        buffer.append("<TR><TH align='center'>Count</TH><TH align='center'>Percent</TH><TH align='center'>Service name</TH></TR>");


        ArrayList<ValueObject> list = sortCounter(map);

        ValueObject object = null;
        int value = 0;

        for ( int i = 0; i < list.size(); i++ ) {
            object = list.get(i);
            value = object.getValue();

            buffer.append("<TR>");
            buffer.append("<TD align='right'>").append(NumberUtils.intToString(value)).append("</TD>");
            buffer.append("<TD align='right'>").append(NumberUtils.intToPercent((10000 * value) / totalServiceCount)).append("%</TD>");
            buffer.append("<TD align='left'>").append(object.getKey()).append("</TD>");
            buffer.append("</TR>");
        }

        buffer.append("</TABLE>");
        buffer.append(getMainBodyEnd());
        return buffer.toString();
    }

    static public void caculCounter( String name, HashMap<String, Integer> map ) {
        String key = StringUtils.makeSimpleLine(name, false);
        Integer value = map.get(key);
        if ( value == null ) {
            value = new Integer(1);
        } else {
            value = new Integer(value.intValue() + 1);
        }
        map.put(key, value);
    }
    
    
    static public ArrayList<ValueObject> sortCounter(HashMap<String, Integer> map){
        String key = null;
        Integer value = null;

        Iterator<String> itor = map.keySet().iterator();

        ArrayList<ValueObject> list = new ArrayList<ValueObject>();
        ValueObject object = null;
        while ( itor.hasNext() ) {
            key = itor.next();
            value = map.get(key);
            object = new ValueObject(key, value.intValue());
            list.add(object);

        }

        Collections.sort(list, new ValueObjectComp());
    	return list;
    }
    
    static public String getUniqueStack(ArrayList<String> list){
        if ( list == null )
            return "";

        StringBuilder buffer = new StringBuilder(1024000);
        buffer.append(getMainBodyStart());
        buffer.append("<b>[ Unique Stack ]</b><BR>");
        buffer.append("<pre><font size=3>");
        for(String stack : list){
        	buffer.append(stack).append("\n");
        }
        buffer.append("</font></pre>");
        buffer.append(getMainBodyEnd());
        return buffer.toString();
    	
    }
}
