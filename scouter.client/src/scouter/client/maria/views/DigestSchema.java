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
package scouter.client.maria.views;

import org.eclipse.swt.SWT;

public enum DigestSchema {
	DIGEST_TEXT("SQL/Instance", 150, SWT.LEFT, false),
    SCHEMA_NAME("DATABASE", 100, SWT.LEFT, false),
    COUNT_STAR("Executions", 70, SWT.CENTER, true),
    SUM_ERRORS("Errors", 100, SWT.RIGHT, true),
    SUM_WARNINGS("Warnings", 100, SWT.RIGHT, true),
    SUM_TIMER_WAIT("Sum Response Time", 100, SWT.RIGHT, true),
    AVG_TIMER_WAIT("Avg Response Time", 100, SWT.RIGHT,  true),
    MIN_TIMER_WAIT("Min Response Time", 100, SWT.RIGHT,  true),
    MAX_TIMER_WAIT("Max Response Time", 100, SWT.RIGHT, true),
    SUM_LOCK_TIME("Sum Lock Time", 100, SWT.RIGHT, true),
    SUM_ROWS_AFFECTED("Sum Rows Affected", 100, SWT.RIGHT, true),
    SUM_ROWS_SENT("Sum Rows Sent", 100, SWT.RIGHT, true),
    SUM_ROWS_EXAMINED("Sum Rows Examined", 100, SWT.RIGHT, true),
    SUM_CREATED_TMP_DISK_TABLES("Sum Created Tmp Disk Tables", 100, SWT.RIGHT, true),
    SUM_CREATED_TMP_TABLES("Sum Created Tmp Tables", 100, SWT.RIGHT, true),
    SUM_SELECT_FULL_JOIN("Sum Select Full Join", 100, SWT.RIGHT, true),
    SUM_SELECT_FULL_RANGE_JOIN("Sum Select Full Range Join", 100, SWT.RIGHT, true),
    SUM_SELECT_RANGE("Sum Select Range", 100, SWT.RIGHT, true),
    SUM_SELECT_RANGE_CHECK("Sum Select Range Check", 100, SWT.RIGHT, true),
    SUM_SELECT_SCAN("Sum Select Scan", 100, SWT.RIGHT, true),
    SUM_SORT_MERGE_PASSES("Sum Sort Merge Passes", 100, SWT.RIGHT, true),
    SUM_SORT_RANGE("Sumr Sort Range", 100, SWT.RIGHT, true),
    SUM_SORT_ROWS("Sum Sort Rows", 100, SWT.RIGHT, true),
    SUM_SORT_SCAN("Sum Sort Scan", 100, SWT.RIGHT, true),
    SUM_NO_INDEX_USED("Sum No Index Used", 100, SWT.RIGHT, true),
    SUM_NO_GOOD_INDEX_USED("Sum No Good Index Used", 100, SWT.RIGHT, true),
    FIRST_SEEN("First Seen", 100, SWT.CENTER, true),
    LAST_SEEN("Last Seen", 100, SWT.CENTER, true);

    private final String title;
    private final int width;
    private final int alignment;
    private final boolean isNumber;

    private DigestSchema(String text, int width, int alignment, boolean isNumber) {
        this.title = text;
        this.width = width;
        this.alignment = alignment;
        this.isNumber = isNumber;
    }
    
    public String getTitle(){
        return title;
    }

    public int getAlignment(){
        return alignment;
    }

	public int getWidth() {
		return width;
	}
	
	public boolean isNumber() {
		return this.isNumber;
	}
}
