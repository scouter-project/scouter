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

public class NumberUtils {
    static public String intToPercent( int value ) {

        StringBuilder buffer = new StringBuilder();
        int highValue = value / 100;
        int lowValue = value % 100;

        buffer.append(highValue).append('.');
        if ( lowValue < 10 )
            buffer.append('0');

        buffer.append(lowValue);

        return buffer.toString();
    }

    static public String intToString( int value ) {
        StringBuilder buffer = new StringBuilder(20);
        String str = Integer.toString(value);

        int size = str.length();
        if ( size <= 3 )
            return str;

        int divide = size / 3;
        int remain = size % 3;
        buffer.append(str.substring(0, remain));

        int pos = 0;
        int index = 0;
        while ( index < divide ) {
            pos = remain + (3 * index);
            if ( !(index == 0 && remain == 0) )
                buffer.append(',');
            buffer.append(str.substring(pos, pos + 3));
            index++;
        }
        pos = remain + (3 * index);
        if ( size > pos )
            buffer.append(',').append(str.substring(pos));

        return buffer.toString();
    }

    static public String secsToTime( int value ) {
        StringBuilder buffer = new StringBuilder(20);
        if ( value == 0 ) {
            return "0:0:0";
        }
        buffer.append((value / 3600)).append(':');
        buffer.append(((value % 3600) / 60)).append(':');
        buffer.append((value % 60));
        return buffer.toString();
    }
}
