/*
 *  Copyright 2015 the original author or authors.
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
package scouter.client.xlog;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.xlog.views.XLogProfileView;
import scouter.lang.CountryCode;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallSum;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodSum;
import scouter.lang.step.SocketStep;
import scouter.lang.step.SocketSum;
import scouter.lang.step.SqlStep;
import scouter.lang.step.SqlStep2;
import scouter.lang.step.SqlSum;
import scouter.lang.step.SqlXType;
import scouter.lang.step.Step;
import scouter.lang.step.StepControl;
import scouter.lang.step.StepEnum;
import scouter.lang.step.StepSingle;
import scouter.lang.step.StepSummary;
import scouter.lang.step.ThreadSubmitStep;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.SortUtil;
import scouter.util.StringUtil;

public class ProfileText {
	public static void build(final String date, StyledText text, XLogData xperf, Step[] profiles, int spaceCnt, int serverId) {
		
		boolean truncated = false;
		
		if (profiles == null) {
			profiles = new Step[0];
		}
		profiles = SortUtil.sort(profiles);
		XLogUtil.loadStepText(serverId, date, profiles);

		String error = TextProxy.error.getLoadText(date, xperf.p.error, serverId);
		Color blue = text.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		Color dmagenta = text.getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);

		Color dred = text.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
		Color dgreen = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);

		java.util.List<StyleRange> sr = new ArrayList<StyleRange>();

		int slen = 0;

		final StringBuffer sb = new StringBuffer();
		sb.append("► txid    = ");
		slen = sb.length();
		sb	.append(Hexa32.toString32(xperf.p.txid)).append("\n");
		sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
		if (xperf.p.gxid != 0) {
			sb.append("► gxid    = ");
			slen = sb.length();
			sb.append(Hexa32.toString32(xperf.p.gxid)).append("\n");
			sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
		}
		if (xperf.p.caller != 0) {
			sb.append("► caller    = ");
			slen = sb.length();
			sb.append(Hexa32.toString32(xperf.p.caller)).append("\n");
			sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
		}
		sb.append("► objName = ").append(xperf.objName).append("\n");
		sb.append("► endtime = ").append(DateUtil.timestamp(xperf.p.endTime)).append("\n");
		sb.append("► elapsed = ").append(FormatUtil.print(xperf.p.elapsed, "#,##0")).append(" ms\n");
		sb.append("► service = ").append(TextProxy.service.getText(xperf.p.service)).append("\n");
		if (error != null) {
			sb.append("► error   = ");
			slen = sb.length();
			sb.append(error).append("\n");
			sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
		}
		
		sb.append("► ipaddr=" + IPUtil.toString(xperf.p.ipaddr) + ", ");
		sb.append("userid=" + xperf.p.userid);
		sb.append("\n► cpu=" + FormatUtil.print(xperf.p.cpu, "#,##0") + " ms, ");
		sb.append("bytes=" + xperf.p.bytes + ", ");
		sb.append("status=" + xperf.p.status);
		if (xperf.p.sqlCount > 0) {
			sb.append("\n► sqlCount=" + xperf.p.sqlCount + ", ");
			sb.append("sqlTime=" + FormatUtil.print(xperf.p.sqlTime, "#,##0") + " ms");
		}
		if (xperf.p.apicallCount > 0) {
			sb.append("\n► ApiCallCount=" + xperf.p.apicallCount + ", ");
			sb.append("ApiCallTime=" + FormatUtil.print(xperf.p.apicallTime, "#,##0") + " ms");
		}
		
		String t = TextProxy.userAgent.getLoadText(date, xperf.p.userAgent, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► userAgent=" + t);
		}
		
		t = TextProxy.referer.getLoadText(date, xperf.p.referer, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► referer=" + t);
		}
		
		t = TextProxy.group.getLoadText(date, xperf.p.group, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► group=" + t);
		}
		if (StringUtil.isNotEmpty(xperf.p.countryCode)) {
			sb.append("\n► country=" + CountryCode.getCountryName(xperf.p.countryCode));
		}
		t = TextProxy.city.getLoadText(date, xperf.p.city, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► city=" + t);
		}
		t = TextProxy.web.getLoadText(date, xperf.p.webHash, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► webName=" + t).append("  webTime=" + xperf.p.webTime+ " ms");
		}
		sb.append("\n");

		sb.append("------------------------------------------------------------------------------------------\n");
		sb.append("    p#      #    	  TIME         T-GAP   CPU          CONTENTS\n");
		sb.append("------------------------------------------------------------------------------------------\n");
		if (profiles.length == 0) {
			sb.append("\n                     ( No xlog profile collected ) ");
			text.setText(sb.toString());
			// for (int i = 0; i < sr.size(); i++) {
			text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
			// }
			return;
		}
		
		long stime = xperf.p.endTime - xperf.p.elapsed;
		long prev_tm = stime;
		long prev_cpu = 0;

		sb.append("        ");
		sb.append(" ");
		sb.append("[******]");
		sb.append(" ");
		sb.append(DateUtil.getLogTime(stime));
		sb.append("   ");
		sb.append(String.format("%6s", "0"));
		sb.append(" ");
		sb.append(String.format("%6s", "0"));
		sb.append("  start transaction \n");
		// sr.add(style(slen, sb.length() - slen, dblue, SWT.NORMAL));

		long tm = xperf.p.endTime;
		long cpu = xperf.p.cpu;
		int sumCnt = 1;
		HashMap<Integer, Integer> indent = new HashMap<Integer, Integer>();
		for (int i = 0; i < profiles.length; i++) {
			
			if(truncated)
				break;
			
			if (profiles[i] instanceof StepSummary) {
				sb.append("        ").append(" ");
				sb.append(String.format("[%06d]", sumCnt++));
				sb.append(" ");
				
				StepSummary sum = (StepSummary) profiles[i];
				switch (sum.getStepType()) {
				case StepEnum.METHOD_SUM:
					XLogProfileView.isSummary = true;
					
					MethodSum p= (MethodSum) sum;
					slen = sb.length();
					
					String m = TextProxy.method.getText(p.hash);
					if (m == null)
						m = Hexa32.toString32(p.hash);
					sb.append(m).append(" ");
					
					sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
					
					sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
					sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
					sb.append(" cpu=").append(FormatUtil.print(p.cputime, "#,##0"));
					
					sb.append("\n");
					break;
				case StepEnum.SQL_SUM:
					XLogProfileView.isSummary = true;
					SqlSum sql = (SqlSum) sum;
					slen = sb.length();
					toString(sb, sql, serverId);
					sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.APICALL_SUM:
					XLogProfileView.isSummary = true;
					ApiCallSum apicall = (ApiCallSum) sum;
					slen = sb.length();
					toString(sb, apicall);
					sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.SOCKET_SUM:
					XLogProfileView.isSummary = true;
					SocketSum socketSum = (SocketSum) sum;
					slen = sb.length();
					toString(sb, socketSum);
					sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.CONTROL:
					
					sb.delete(sb.length() - 9, sb.length());
					
					sb.append("[******]");
					sb.append(" ");
					sb.append(DateUtil.getLogTime(tm));
					sb.append("   ");
					sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
					sb.append(" ");
					sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
					sb.append("  ");
					slen = sb.length();
					toString(sb, (StepControl) sum);
					sr.add(style(slen, sb.length() - slen, dred, SWT.NORMAL));
					sb.append("\n");
					
					truncated = true;
					
					break;
				}
				continue;
			}
			
			
			
			StepSingle stepSingle = (StepSingle) profiles[i];
			tm = stepSingle.start_time + stime;
			cpu = stepSingle.start_cpu;

			// sr.add(style(sb.length(), 6, blue, SWT.NORMAL));
			int p1 = sb.length();
			String pid = String.format("[%06d]", stepSingle.parent);
			sb.append((stepSingle.parent == -1) ? "    -   " : pid);
			sb.append(" ");
			sb.append(String.format("[%06d]", stepSingle.index));
			sb.append(" ");
			sb.append(DateUtil.getLogTime(tm));
			sb.append("   ");
			sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
			sb.append(" ");
			sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
			sb.append("  ");
            int lineHead=sb.length()-p1;
			
			int space = 0;
			if (indent.containsKey(stepSingle.parent)) {
				space = indent.get(stepSingle.parent) + spaceCnt;
			}
			indent.put(stepSingle.index, space);
			while (space > 0) {
				sb.append(" ");
				space--;
			}

			switch (stepSingle.getStepType()) {
			case StepEnum.METHOD:
				toString(sb, (MethodStep) stepSingle);
				break;
			case StepEnum.SQL:
			case StepEnum.SQL2:
				SqlStep sql = (SqlStep) stepSingle;
				slen = sb.length();
				toString(sb, sql, serverId,lineHead);
				sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
				if (sql.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(sql.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.MESSAGE:
				slen = sb.length();
				toString(sb, (MessageStep) stepSingle);
				sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
				break;
			case StepEnum.APICALL:
				ApiCallStep apicall = (ApiCallStep) stepSingle;
				slen = sb.length();
				toString(sb, apicall);
				sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
				if (apicall.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(apicall.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.THREAD_SUBMIT:
				ThreadSubmitStep threadSubmit = (ThreadSubmitStep) stepSingle;
				slen = sb.length();
				toString(sb, threadSubmit);
				sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
				if (threadSubmit.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(threadSubmit.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.SOCKET:
				SocketStep socket = (SocketStep) stepSingle;
				slen = sb.length();
				toString(sb, socket);
				sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				if (socket.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(socket.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			}
			sb.append("\n");
			prev_cpu = cpu;
			prev_tm = tm;
		}

		if(!truncated){
		
			tm = xperf.p.endTime;
			cpu = xperf.p.cpu;
			sb.append("        ");
			sb.append(" ");
			sb.append("[******]");
			sb.append(" ");
			sb.append(DateUtil.getLogTime(tm));
			sb.append("   ");
			sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
			sb.append(" ");
			sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
			sb.append("  end of transaction \n");
	
		}
		sb.append("------------------------------------------------------------------------------------------\n");

		text.setText(sb.toString());
		text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
		
	}
	
	public static void buildThreadProfile(XLogData data, StyledText text, Step[] profiles) {
		if (profiles == null) {
			profiles = new Step[0];
		}
		int serverId = data.serverId;
		String date = DateUtil.yyyymmdd(data.p.endTime);
		profiles = SortUtil.sort(profiles);
		XLogUtil.loadStepText(serverId, date, profiles);
		Color blue = text.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		Color dmagenta = text.getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
		Color dred = text.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
		Color dgreen = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		java.util.List<StyleRange> sr = new ArrayList<StyleRange>();
		int slen = 0;
		final StringBuffer sb = new StringBuffer();
		
		sb.append("------------------------------------------------------------------------------------------\n");
		sb.append("    p#      #    	  TIME         T-GAP   CPU          CONTENTS\n");
		sb.append("------------------------------------------------------------------------------------------\n");
		if (profiles.length == 0) {
			sb.append("\n                     ( No xlog profile collected ) ");
			text.setText(sb.toString());
			text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
			return;
		}
		
		long stime = data.p.endTime - data.p.elapsed;
		long prev_tm = stime;
		long tm = stime;
		int prev_cpu = -1;
		int cpu = 0;
		int sumCnt = 1;
		HashMap<Integer, Integer> indent = new HashMap<Integer, Integer>();
		for (int i = 0; i < profiles.length; i++) {
			if (profiles[i] instanceof StepSummary) {
				sb.append("        ").append(" ");
				sb.append(String.format("[%06d]", sumCnt++));
				sb.append(" ");
				
				StepSummary sum = (StepSummary) profiles[i];
				switch (sum.getStepType()) {
				case StepEnum.METHOD_SUM:
					MethodSum p= (MethodSum) sum;
					slen = sb.length();
					String m = TextProxy.method.getText(p.hash);
					if (m == null)
						m = Hexa32.toString32(p.hash);
					sb.append(m).append(" ");
					
					sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
					
					sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
					sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
					sb.append(" cpu=").append(FormatUtil.print(p.cputime, "#,##0"));
					
					sb.append("\n");
					break;
				case StepEnum.SQL_SUM:
					SqlSum sql = (SqlSum) sum;
					slen = sb.length();
					toString(sb, sql, serverId);
					sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.APICALL_SUM:
					ApiCallSum apicall = (ApiCallSum) sum;
					slen = sb.length();
					toString(sb, apicall);
					sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.SOCKET_SUM:
					SocketSum socketSum = (SocketSum) sum;
					slen = sb.length();
					toString(sb, socketSum);
					sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					sb.append("\n");
					break;
				case StepEnum.CONTROL:
					sb.delete(sb.length() - 9, sb.length());
					sb.append("[******]");
					sb.append(" ");
					sb.append(DateUtil.getLogTime(data.p.endTime));
					sb.append("   ");
					sb.append(String.format("%6s", FormatUtil.print(data.p.elapsed, "#,##0")));
					sb.append(" ");
					sb.append(String.format("%6s", FormatUtil.print(data.p.cpu, "#,##0")));
					sb.append("  ");
					slen = sb.length();
					toString(sb, (StepControl) sum);
					sr.add(style(slen, sb.length() - slen, dred, SWT.NORMAL));
					sb.append("\n");
					break;
				}
				continue;
			}
			
			StepSingle stepSingle = (StepSingle) profiles[i];
			tm = stime + stepSingle.start_time;
			cpu = stepSingle.start_cpu;

			// sr.add(style(sb.length(), 6, blue, SWT.NORMAL));
			int p1 = sb.length();
			String pid = String.format("[%06d]", stepSingle.parent);
			sb.append((stepSingle.parent == -1) ? "    -   " : pid);
			sb.append(" ");
			sb.append(String.format("[%06d]", stepSingle.index));
			sb.append(" ");
			sb.append(DateUtil.getLogTime(tm));
			sb.append("   ");
			sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
			sb.append(" ");
			if (prev_cpu == -1) {
				sb.append(String.format("%6s", FormatUtil.print(0, "#,##0")));
			} else {
				sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
			}
			
			sb.append("  ");
            int lineHead=sb.length()-p1;
			
			int space = 0;
			if (indent.containsKey(stepSingle.parent)) {
				space = indent.get(stepSingle.parent) + 1;
			}
			indent.put(stepSingle.index, space);
			while (space > 0) {
				sb.append(" ");
				space--;
			}

			switch (stepSingle.getStepType()) {
			case StepEnum.METHOD:
				toString(sb, (MethodStep) stepSingle);
				break;
			case StepEnum.SQL:
			case StepEnum.SQL2:
				SqlStep sql = (SqlStep) stepSingle;
				slen = sb.length();
				toString(sb, sql, serverId,lineHead);
				sr.add(style(slen, sb.length() - slen, blue, SWT.NORMAL));
				if (sql.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(sql.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.MESSAGE:
				slen = sb.length();
				toString(sb, (MessageStep) stepSingle);
				sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
				break;
			case StepEnum.APICALL:
				ApiCallStep apicall = (ApiCallStep) stepSingle;
				slen = sb.length();
				toString(sb, apicall);
				sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
				if (apicall.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(apicall.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.THREAD_SUBMIT:
				ThreadSubmitStep threadSubmit = (ThreadSubmitStep) stepSingle;
				slen = sb.length();
				toString(sb, threadSubmit);
				sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
				if (threadSubmit.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(threadSubmit.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.SOCKET:
				SocketStep socket = (SocketStep) stepSingle;
				slen = sb.length();
				toString(sb, socket);
				sr.add(style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				if (socket.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(socket.error));
					sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			}
			sb.append("\n");
			prev_cpu = cpu;
			prev_tm = tm;
		}

		sb.append("------------------------------------------------------------------------------------------\n");

		text.setText(sb.toString());
		text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
	}
	
	public static void toString(StringBuffer sb, ApiCallStep p) {
		String m = TextProxy.apicall.getText(p.hash);
		if (m == null)
			m = Hexa32.toString32(p.hash);
		sb.append("call: ").append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
		if (p.txid != 0) {
			sb.append(" <" + Hexa32.toString32(p.txid) + ">");
		}
	}
	public static void toString(StringBuffer sb, ThreadSubmitStep p) {
		String m = TextProxy.apicall.getText(p.hash);
		if (m == null)
			m = Hexa32.toString32(p.hash);
		sb.append("thread: ").append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
		if (p.txid != 0) {
			sb.append(" <" + Hexa32.toString32(p.txid) + ">");
		}
	}
	public static void toString(StringBuffer sb, SocketStep p) {
		String ip = IPUtil.toString(p.ipaddr);
		sb.append("socket: ").append(ip == null ? "unknown" : ip).append(":").append(p.port + " ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
	}


	public static void toString(StringBuffer sb, ApiCallSum p) {
		String m = TextProxy.apicall.getText(p.hash);
		if (m == null)
			m = Hexa32.toString32(p.hash);
		sb.append("call: ").append(m).append(" ");
		sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
		sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
		sb.append(" cpu=").append(FormatUtil.print(p.cputime, "#,##0"));
		if (p.error > 0) {
			sb.append(" error=").append(FormatUtil.print(p.error, "#,##0"));
		}
	}
	
	public static void toString(StringBuffer sb, SocketSum p) {
		String ip = IPUtil.toString(p.ipaddr);
		sb.append("socket: ").append(ip == null ? "unknown" : ip).append(":").append(p.port + " ");
		sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
		sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
		if (p.error > 0) {
			sb.append(" error=").append(FormatUtil.print(p.error, "#,##0"));
		}
	}

	public static void toString(StringBuffer sb, SqlStep p, int serverId, int lineHead) {
		if(p instanceof SqlStep2){
			sb.append(SqlXType.toString(((SqlStep2)p).xtype));
		}
		String m = TextProxy.sql.getText(p.hash);
		m=spacing(m,lineHead);
		if (m == null)
			m = Hexa32.toString32(p.hash);
		sb.append(m);
		if (StringUtil.isEmpty(p.param) == false) {
			sb.append("\n").append( StringUtil.leftPad("", lineHead));
			Server server = ServerManager.getInstance().getServer(serverId);
			boolean showParam = server.isAllowAction(GroupPolicyConstants.ALLOW_SQLPARAMETER);
			sb.append("[").append(showParam ? p.param : "******").append("]");
		}
		sb.append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
	}
	
	public static String spacing(String m, int lineHead) {
		if(m==null)
			return m;
		String dummy = StringUtil.leftPad("", lineHead);
		StringBuffer sb = new StringBuffer();
		try{
			BufferedReader sr = new BufferedReader(new StringReader(m));
			String s =null;
			while((s = sr.readLine())!=null){
				s = StringUtil.trim(s);
				if(s.length()>0)
				{
					if(sb.length()>0){
						sb.append("\n").append(dummy);
					}
					sb.append(s);
				}
				
			}
		}catch(Exception e){}
		return sb.toString();
	}

	public static void toString(StringBuffer sb, SqlSum p, int serverId) {
		String m = TextProxy.sql.getText(p.hash);
		if (m == null)
			m = Hexa32.toString32(p.hash);
		sb.append(m);
		if (StringUtil.isEmpty(p.param) == false) {
			Server server = ServerManager.getInstance().getServer(serverId);
			boolean showParam = server.isAllowAction(GroupPolicyConstants.ALLOW_SQLPARAMETER);
			sb.append(" [").append(showParam ? p.param : "******").append("]");
		}
		sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
		sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
		sb.append(" cpu=").append(FormatUtil.print(p.cputime, "#,##0"));
		if (p.error > 0) {
			sb.append(" error=").append(FormatUtil.print(p.error, "#,##0"));
		}
	}
	
	public static void toString(StringBuffer sb, MessageStep p) {
		sb.append(p.message);
	}
	
	public static void toString(StringBuffer sb, StepControl p) {
		sb.append(p.message);
	}

	public static void toString(StringBuffer sb, MethodStep p) {
		String m = TextProxy.method.getText(p.hash);
		if (m == null){
			m = Hexa32.toString32(p.hash);
		}
		sb.append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
	}
	
	public static StyleRange style(int start, int length, Color c, int f) {
		StyleRange t = new StyleRange();
		t.start = start;
		t.length = length;
		t.foreground = c;
		t.fontStyle = f;
		return t;
	}
	
	public static StyleRange style(int start, int length, Color c, int f, Color backc) {
		StyleRange t = new StyleRange();
		t.start = start;
		t.length = length;
		t.foreground = c;
		t.fontStyle = f;
		t.background = backc;
		return t;
	}
	
	public static StyleRange underlineStyle(int start, int length, Color c, int fontStyle, int underlineStyle) {
		StyleRange t = new StyleRange();
		t.start = start;
		t.length = length;
		t.foreground = c;
		t.fontStyle = fontStyle;
		t.underline = true;
		t.underlineStyle = underlineStyle;
		return t;
	}
}
