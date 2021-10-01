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
package scouter.client.cubrid;

import org.eclipse.swt.SWT;

import scouter.lang.constants.StatusConstants;
import scouter.net.RequestCmd;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;

public enum CubridSingleItem {
	CPU(InfoType.DB_INFO, "Cpu Usage", RequestCmd.CUBRID_DB_SERVER_INFO, StatusConstants.CUBRID_DB_SERVER_INFO, "cpu_used", SWT.COLOR_DARK_BLUE, TraceType.BAR, 0, false),
	ACTIVE_SESSION(InfoType.DB_INFO, "Active Sessions", RequestCmd.CUBRID_DB_SERVER_INFO, StatusConstants.CUBRID_DB_SERVER_INFO, "active_session", SWT.COLOR_DARK_BLUE, TraceType.BAR, 1, false),
	LOCK_WAIT_SESSIONS(InfoType.DB_INFO, "Lock Wait Sessions", RequestCmd.CUBRID_DB_SERVER_INFO, StatusConstants.CUBRID_DB_SERVER_INFO, "lock_wait_sessions", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 2, false),
	DATA_PAGE_IO_WRITES(InfoType.DB_INFO, "Data Page_IO Writes",  RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_data_page_iowrites", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 3, false),
	DATA_PAGE_IO_READS(InfoType.DB_INFO, "Data Page IO Reads", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_data_page_ioreads", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 4, false),
	DATA_PAGE_FETCHES(InfoType.DB_INFO, "Data Page Fetches", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_data_page_fetches", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 5, false),
	DATA_PAGE_DIRTIES(InfoType.DB_INFO, "Data Page Dirties", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_data_page_dirties", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 6, false),
	DATA_BUFFER_HIT_RATIO(InfoType.DB_INFO, "Data Buffer Hit_Ratio", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "data_page_buffer_hit_ratio", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 7, false),
	QUERY_SSCANS(InfoType.DB_INFO, "Query Sscans", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_query_sscans", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 8, false),
	SORT_IO_PAGE(InfoType.DB_INFO, "Sort IO_Page", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_sort_io_pages", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 9, false),
	SORT_DATA_PAGE(InfoType.DB_INFO, "Sort Data Page", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "num_sort_data_pages", SWT.COLOR_DARK_BLUE, TraceType.SOLID_LINE, 10, false),
	XASL_PLAN_HIT_RATE(InfoType.DB_INFO, "XASL Plan Hit Rate (%)",  RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "xasl_plan_hit_rate", SWT.COLOR_DARK_BLUE, TraceType.AREA, 11, true),
	FILTER_PLAN_HIT_RATE(InfoType.DB_INFO, "Filter Predicate Hit Rate (%)", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "filter_plan_hit_rate", SWT.COLOR_DARK_BLUE, TraceType.AREA, 12, true),
	TPS(InfoType.BROKER_INFO, "Transaction Per 5 Second",  RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "as_num_tran", SWT.COLOR_DARK_CYAN, TraceType.DASH_LINE, 0, false),
	QPS(InfoType.BROKER_INFO, "Query Per 5 Second", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "as_num_query", SWT.COLOR_DARK_CYAN, TraceType.DASH_LINE, 1, false),
	ERROR_QUERY(InfoType.BROKER_INFO, "Error Query Per 5 Second", RequestCmd.CUBRID_DB_REALTIME_STATUS, StatusConstants.CUBRID_DB_DUMP_INFO, "as_error_query", SWT.COLOR_DARK_CYAN, TraceType.DASH_LINE, 2, false);

	private InfoType infoType;
	private String title;
	private String requestCmd;
	private String statusConstants;
	private String counterName;
	private int color;
	private TraceType traceType;
	private int index;
	private boolean isPercentData;

	CubridSingleItem(InfoType infoType, String title, String requestCmd, String statusConstants, String counterName,
			int color, TraceType traceType, int index, boolean isPercentData) {
		this.infoType = infoType;
		this.title = title;
		this.requestCmd = requestCmd;
		this.statusConstants = statusConstants;
		this.counterName = counterName;
		this.color = color;
		this.traceType = traceType;
		this.index = index;
		this.isPercentData = isPercentData;
	}

	public InfoType getInfoType() {
		return infoType;
	}

	public String getTitle() {
		return title;
	}

	public String getRequestCmd() {
		return requestCmd;
	}

	public String getStatusConstants() {
		return statusConstants;
	}

	public String getCounterName() {
		return counterName;
	}

	public int getColor() {
		return color;
	}

	public TraceType getTraceType() {
		return traceType;
	}

	public int getIndex() {
		return index;
	}
	
	public boolean isPercent() {
		return isPercentData;
	}

	public static CubridSingleItem fromCounterName(String title) {
		if (title != null) {
			for (CubridSingleItem type : CubridSingleItem.values()) {
				if (title.equalsIgnoreCase(type.title)) {
					return type;
				}
			}
		}
		return null;
	}

	public static InfoType infotypeFromOrdinal(int ordinal) {
		for (CubridSingleItem type : CubridSingleItem.values()) {
			if (ordinal == type.ordinal()) {
				return type.infoType;
			}
		}
		return null;
	}

	public enum InfoType {
		DB_INFO("DB_INFO"),
		BROKER_INFO("BROKER_INFO");
		
		private String title;
		
		private InfoType(String title) {
			this.title = title;
		}
		
		public String getTitle() {
			return title;
		}
	}
}