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
package scouter.client.xlog;

import scouter.client.model.TextProxy;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.*;

import java.util.HashSet;

public class XLogUtil {

	public static void loadStepText(int serverId, String yyyymmdd, Step[] p) {
		HashSet<Integer> methodSet = new HashSet<Integer>();
		HashSet<Integer> sqlSet = new HashSet<Integer>();
		HashSet<Integer> subcallSet = new HashSet<Integer>();
		HashSet<Integer> errorSet = new HashSet<Integer>();
		HashSet<Integer> hashedMsgSet = new HashSet<Integer>();
        HashSet<Integer> stackElementSet = new HashSet<Integer>();
		for (int i = 0; i < p.length; i++) {
			switch (p[i].getStepType()) {
			case StepEnum.SQL:
			case StepEnum.SQL2:
			case StepEnum.SQL3:
				if (TextProxy.sql.getText(((SqlStep) p[i]).hash) == null) {
					sqlSet.add(((SqlStep) p[i]).hash);
				}
				if (((SqlStep) p[i]).error != 0 && TextProxy.error.getText(((SqlStep) p[i]).error) == null) {
					errorSet.add(((SqlStep) p[i]).error);
				}
				break;
			case StepEnum.SQL_SUM:
				if (TextProxy.sql.getText(((SqlSum) p[i]).hash) == null) {
					sqlSet.add(((SqlStep) p[i]).hash);
				}
				break;
			case StepEnum.HASHED_MESSAGE:
				if (TextProxy.hashMessage.getText(((HashedMessageStep) p[i]).hash) == null) {
					hashedMsgSet.add(((HashedMessageStep) p[i]).hash);
				}
				break;
			case StepEnum.PARAMETERIZED_MESSAGE:
				if (TextProxy.hashMessage.getText(((ParameterizedMessageStep) p[i]).getHash()) == null) {
					hashedMsgSet.add(((ParameterizedMessageStep) p[i]).getHash());
				}
				break;
            case StepEnum.DUMP:
                DumpStep dumpStep = (DumpStep) p[i];
                for(int stackElementHash : dumpStep.stacks) {
                    if(TextProxy.stackElement.getText(stackElementHash) == null) {
                        stackElementSet.add(stackElementHash);
                    }
                }
                break;
			case StepEnum.METHOD:
			case StepEnum.METHOD2:
				if (TextProxy.method.getText(((MethodStep) p[i]).hash) == null) {
					methodSet.add(((MethodStep) p[i]).hash);
				}
				break;
			case StepEnum.METHOD_SUM:
				if (TextProxy.method.getText(((MethodSum) p[i]).hash) == null) {
					methodSet.add(((MethodSum) p[i]).hash);
				}
				break;
			case StepEnum.SOCKET:
				if (((SocketStep) p[i]).error != 0 && TextProxy.error.getText(((SocketStep) p[i]).error) == null) {
					errorSet.add(((SocketStep) p[i]).error);
				}
				break;
			case StepEnum.APICALL:
			case StepEnum.APICALL2:
				if (TextProxy.apicall.getText(((ApiCallStep) p[i]).hash) == null) {
					subcallSet.add(((ApiCallStep) p[i]).hash);
				}
				if (((ApiCallStep) p[i]).error != 0 && TextProxy.error.getText(((ApiCallStep) p[i]).error) == null) {
					errorSet.add(((ApiCallStep) p[i]).error);
				}
				break;
			case StepEnum.DISPATCH:
				if (TextProxy.apicall.getText(((DispatchStep) p[i]).hash) == null) {
					subcallSet.add(((DispatchStep) p[i]).hash);
				}
				if (((DispatchStep) p[i]).error != 0 && TextProxy.error.getText(((DispatchStep) p[i]).error) == null) {
					errorSet.add(((DispatchStep) p[i]).error);
				}
				break;
			case StepEnum.THREAD_CALL_POSSIBLE:
				if (TextProxy.apicall.getText(((ThreadCallPossibleStep) p[i]).hash) == null) {
					subcallSet.add(((ThreadCallPossibleStep) p[i]).hash);
				}
				break;
			case StepEnum.APICALL_SUM:
				if (TextProxy.apicall.getText(((ApiCallSum) p[i]).hash) == null) {
					subcallSet.add(((ApiCallSum) p[i]).hash);
				}
				break;
			case StepEnum.THREAD_SUBMIT:
				if (TextProxy.apicall.getText(((ThreadSubmitStep) p[i]).hash) == null) {
					subcallSet.add(((ThreadSubmitStep) p[i]).hash);
				}
				if (((ThreadSubmitStep) p[i]).error != 0
						&& TextProxy.error.getText(((ThreadSubmitStep) p[i]).error) == null) {
					errorSet.add(((ThreadSubmitStep) p[i]).error);
				}
				break;
			}
		}

		TextProxy.method.load(yyyymmdd, methodSet, serverId);
		TextProxy.sql.load(yyyymmdd, sqlSet, serverId);
		TextProxy.apicall.load(yyyymmdd, subcallSet, serverId);
		TextProxy.error.load(yyyymmdd, errorSet, serverId);
		TextProxy.hashMessage.load(yyyymmdd, hashedMsgSet, serverId);
        TextProxy.stackElement.load(yyyymmdd, stackElementSet, serverId);
	}

//	public static int getStepElaspedTime(Step p) {
//		switch (p.getStepType()) {
//		case StepEnum.SQL:
//		case StepEnum.SQL2:
//		case StepEnum.SQL3:
//			SqlStep ss = (SqlStep) p;
//			return ss.elapsed;
//		case StepEnum.SOCKET:
//			SocketStep sk = (SocketStep) p;
//			return sk.elapsed;
//		case StepEnum.METHOD:
//		case StepEnum.METHOD2:
//			MethodStep ms = (MethodStep) p;
//			return ms.elapsed;
//		case StepEnum.APICALL:
//		case StepEnum.APICALL2:
//			ApiCallStep acs = (ApiCallStep) p;
//			return acs.elapsed;
//		case StepEnum.THREAD_SUBMIT:
//			ThreadSubmitStep tss = (ThreadSubmitStep) p;
//			return tss.elapsed;
//		}
//		return 0;
//	}
	
	public static int getCpuTime(Step p) {
		switch (p.getStepType()) {
		case StepEnum.SQL:
		case StepEnum.SQL2:
		case StepEnum.SQL3:
			SqlStep ss = (SqlStep) p;
			return ss.cputime;
		case StepEnum.METHOD:
		case StepEnum.METHOD2:
			MethodStep ms = (MethodStep) p;
			return ms.cputime;
		case StepEnum.APICALL:
		case StepEnum.APICALL2:
			ApiCallStep acs = (ApiCallStep) p;
			return acs.cputime;
		case StepEnum.THREAD_SUBMIT:
			ThreadSubmitStep tss = (ThreadSubmitStep) p;
			return tss.cputime;
		}
		return 0;
	}
	
//	public static String getStepContents(Step p) {
//		StringBuilder sb = new StringBuilder();
//		switch (p.getStepType()) {
//			case StepEnum.METHOD:
//			case StepEnum.METHOD2:
//				MethodStep ms = (MethodStep) p;
//				sb.append(TextProxy.method.getText(ms.hash));
//				break;
//			case StepEnum.SQL3:
//			case StepEnum.SQL2:
//			case StepEnum.SQL:
//            	SqlStep ss = (SqlStep) p;
//            	sb.append(TextProxy.sql.getText(ss.hash));
//            	break;
//			case StepEnum.MESSAGE:
//				MessageStep mms = (MessageStep) p;
//				sb.append(mms.message);
//				break;
//			case StepEnum.HASHED_MESSAGE:
//				HashedMessageStep hms = (HashedMessageStep) p;
//				sb.append(TextProxy.hashMessage.getText(hms.hash) + " #" + FormatUtil.print(hms.value, "#,##0"));
//				break;
//            case StepEnum.DUMP:
//                DumpStep dumpStep = (DumpStep) p;
//                sb.append(dumpStep.threadId).append(" - ").append(dumpStep.threadName).append('\n');
//                for(int stackElementHash : dumpStep.stacks) {
//                    sb.append(TextProxy.stackElement.getText(stackElementHash)).append('\n');
//                }
//                break;
//			case StepEnum.APICALL:
//			case StepEnum.APICALL2:
//				ApiCallStep acs = (ApiCallStep) p;
//				sb.append("call:").append(TextProxy.apicall.getText(acs.hash));
//				if (acs.txid != 0) {
//		            sb.append(" <" + Hexa32.toString32(acs.txid) + ">");
//		        }
//				break;
//			case StepEnum.SOCKET:
//				SocketStep sos = (SocketStep) p;
//				String ip = IPUtil.toString(sos.ipaddr);
//		        sb.append("socket: ").append(ip == null ? "unknown" : ip).append(":").append(sos.port);
//		        break;
//			case StepEnum.THREAD_SUBMIT:
//				ThreadSubmitStep tss = (ThreadSubmitStep) p;
//				sb.append("thread: ").append(TextProxy.apicall.getText(tss.hash));
//				if (tss.txid != 0) {
//		            sb.append(" <" + Hexa32.toString32(tss.txid) + ">");
//		        }
//				break;
//		}
//		return sb.toString();
//	}
//
//	public static String getErrorMessage(Step p) {
//		switch (p.getStepType()) {
//			case StepEnum.METHOD2:
//				MethodStep2 ms2 = (MethodStep2) p;
//				return TextProxy.error.getText(ms2.error);
//			 case StepEnum.SQL:
//             case StepEnum.SQL2:
//             case StepEnum.SQL3:
//            	 SqlStep ss = (SqlStep) p;
//            	 return TextProxy.error.getText(ss.error);
//            case StepEnum.APICALL:
//			case StepEnum.APICALL2:
//            	 ApiCallStep acs = (ApiCallStep) p;
//            	 return TextProxy.error.getText(acs.error);
//             case StepEnum.SOCKET:
//            	 SocketStep sos = (SocketStep) p;
//            	 return TextProxy.error.getText(sos.error);
//             case StepEnum.THREAD_SUBMIT:
//            	 ThreadSubmitStep tss = (ThreadSubmitStep) p;
//            	 return TextProxy.error.getText(tss.error);
//		}
//		return null;
//	}
//
//	public static String toStringStepSingleType(StepSingle step) {
//		switch (step.getStepType()) {
//		case StepEnum.METHOD:
//			return "MTD";
//		case StepEnum.METHOD2:
//			return "MTD2";
//		case StepEnum.MESSAGE:
//			return "MSG";
//		case StepEnum.HASHED_MESSAGE:
//			return "HSG";
//        case StepEnum.DUMP:
//            return "DMP";
//		case StepEnum.SQL:
//			return "SQL";
//		case StepEnum.SQL2:
//			return "SQL2";
//		case StepEnum.SQL3:
//			return "SQL3";
//		case StepEnum.SOCKET:
//			return "SCK";
//		case StepEnum.APICALL:
//			return "API";
//		case StepEnum.APICALL2:
//			return "API2";
//		case StepEnum.THREAD_SUBMIT:
//			return "THD";
//		}
//		return null;
//	}

//	private final static String HEADER_SET_FONT = "<style>" + "@font-face {font-family:'Malgun Gothic';}"
//			+ "body,td,select,input,div,form,textarea,center,option,pre,blockquote {font-size:9pt; font-family:'Malgun Gothic'; color:#333333;line-height:160%}"
//			+ "table { border-collapse:collapse; }" + "th{ font-size:10pt; border-bottom:1px dotted #C6C6C6; }"
//			+ "td{ border-bottom:1px dotted #EAEAEA; }" + "</style>";
//
//	private final static String TABLE_ROW_START = "<tr>";
//	private final static String TABLE_ROW_END = "</tr>";
//
//	private final static String TD = "<td>";
//	private final static String TDC = "<td align=center>";
//	private final static String TDE = "</td>";

//	private final static String BR = "<br>";

//	public static String xLogToHtml(XLogPack pack, Step[] profiles, final int serverId) {
//		boolean truncated = false;
//		if (profiles == null) {
//			profiles = new Step[0];
//		}
//		profiles = SortUtil.sort(profiles);
//		final String date = DateUtil.yyyymmdd(pack.endTime);
//		loadStepText(serverId, date, profiles);
//		String error = TextProxy.error.getLoadText(date, pack.error, serverId);
//		String objName = TextProxy.object.getLoadText(date, pack.objHash, serverId);
//		final StringBuffer sb = new StringBuffer();
//
//		sb.append(HEADER_SET_FONT);
//
//		sb.append("<table width=\"1000\">");
//
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>txid</b>").append(TDE).append(TD)
//				.append(Hexa32.toString32(pack.txid)).append(TDE).append(TABLE_ROW_END);
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>gxid</b>").append(TDE).append(TD)
//				.append(Hexa32.toString32(pack.gxid)).append(TDE).append(TABLE_ROW_END);
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>objName</b>").append(TDE).append(TD)
//				.append(objName).append(TDE).append(TABLE_ROW_END);
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>endtime</b>").append(TDE).append(TD)
//				.append(DateUtil.timestamp(pack.endTime)).append(TDE).append(TABLE_ROW_END);
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>elapsed</b>").append(TDE).append(TD)
//				.append(FormatUtil.print(pack.elapsed, "#,##0")).append(TDE).append(TABLE_ROW_END);
//		sb.append(TABLE_ROW_START).append("<td width=100>").append("<b>service</b>").append(TDE).append(TD)
//				.append(TextProxy.service.getText(pack.service)).append(TDE).append(TABLE_ROW_END);
//		if (error != null) {
//			sb.append(TABLE_ROW_START).append("<td width=100>").append("<font color=red><b>error</b></font>")
//					.append(TDE).append(TD).append("<font color=red>").append(error).append("</font>").append(TDE)
//					.append(TABLE_ROW_END);
//		}
//
//		sb.append("ipaddr=" + IPUtil.toString(pack.ipaddr) + ", ");
//		sb.append("visitor=" + pack.userid + ", ");
//		sb.append("cpu=" + FormatUtil.print(pack.cpu, "#,##0") + " ms, ");
//		sb.append("kbytes=" + pack.kbytes + ", ");
//		sb.append("status=" + pack.status + ", ");
//		if (pack.sqlCount > 0) {
//			sb.append("sqlCount=" + pack.sqlCount + ", ");
//			sb.append("sqlTime=" + FormatUtil.print(pack.sqlTime, "#,##0") + " ms, ");
//		}
//		if (pack.apicallCount > 0) {
//			sb.append("ApiCallCount=" + pack.apicallCount + ", ");
//			sb.append("ApiCallTime=" + FormatUtil.print(pack.apicallTime, "#,##0") + " ms, ");
//		}
//
//		String t = TextProxy.userAgent.getLoadText(date, pack.userAgent, serverId);
//		if (StringUtil.isNotEmpty(t)) {
//			sb.append("userAgent=" + t + ", ");
//		}
//
//		t = TextProxy.referer.getLoadText(date, pack.referer, serverId);
//		if (StringUtil.isNotEmpty(t)) {
//			sb.append("referer=" + t + ", ");
//		}
//
//		t = TextProxy.group.getLoadText(date, pack.group, serverId);
//		if (StringUtil.isNotEmpty(t)) {
//			sb.append("group=" + t);
//		}
//		sb.append(TDE).append(TABLE_ROW_END);
//		sb.append("</table>");
//
//		sb.append(BR);
//
//		sb.append("<table width=\"1500\">");
//
//		sb.append(TABLE_ROW_START);
//		sb.append(
//				"<font size=9px><th width=80px>p#</th><th width=80px>#</th><th width=80px>TIME</th><th width=80px>T-GAP</th><th width=80px>CPU</th><th width=1100px>CONTENTS</th></font>");
//		sb.append(TABLE_ROW_END);
//		if (profiles.length == 0) {
//			sb.append("<tr colspan=6><th>( No xlog profile collected )</th></tr>");
//			return sb.toString();
//		}
//
//		long stime = pack.endTime - pack.elapsed;
//		long prev_tm = stime;
//		long prev_cpu = 0;
//
//		sb.append(TABLE_ROW_START);
//		sb.append(TDC).append(TDE);
//		sb.append(TDC).append(TDE);
//		sb.append(TDC).append(DateUtil.getLogTime(stime)).append(TDE);
//		sb.append(TDC).append(TDE);
//		sb.append(TDC).append(TDE);
//		sb.append(TD + "<font color='#0000ff'><b>start transaction<b></font>").append(TDE);
//		sb.append(TABLE_ROW_END);
//
//		long tm = pack.endTime;
//		long cpu = pack.cpu;
//		int sumCnt = 0;
//		HashMap<Integer, Integer> indent = new HashMap<Integer, Integer>();
//		for (int i = 0; i < profiles.length; i++) {
//
//			sb.append(TABLE_ROW_START);
//
//			if (truncated)
//				break;
//
//			if (profiles[i] instanceof StepSummary) {
//				sb.append(TDC).append((sumCnt++)).append(TDE);
//
//				StepSummary sum = (StepSummary) profiles[i];
//				switch (sum.getStepType()) {
//				case StepEnum.METHOD_SUM:
//					MethodSum p = (MethodSum) sum;
//
//					String m = TextProxy.method.getText(p.hash);
//					if (m == null)
//						m = Hexa32.toString32(p.hash);
//					sb.append("<td colspan=5>").append(m).append("&nbsp;");
//
//					sb.append(" count=").append("<font color=blue><b>").append(FormatUtil.print(p.count, "#,##0"))
//							.append("</b></font>");
//					sb.append(" time=");
//					if (p.elapsed > 0) {
//						sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0"))
//								.append("</b></font>");
//					} else {
//						sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//					}
//					sb.append(" ms");
//					sb.append(" cpu=").append("<font color=blue><b>").append(FormatUtil.print(p.cputime, "#,##0"))
//							.append("</b></font>");
//
//					sb.append(TDE);
//
//					break;
//				case StepEnum.SQL_SUM:
//					SqlSum sql = (SqlSum) sum;
//					toString(sb, sql, serverId);
//					sb.append(TDE);
//					break;
//				case StepEnum.SOCKET_SUM:
//					SocketSum socketSum = (SocketSum) sum;
//					toString(sb, socketSum);
//					sb.append(TDE);
//					break;
//				case StepEnum.CONTROL:
//					sb.append(TDC).append(TDE);
//					sb.append(TDC).append(DateUtil.getLogTime(tm)).append(TDE);
//					sb.append(TDC).append(FormatUtil.print(tm - prev_tm, "#,##0")).append(TDE);
//					sb.append(TDC).append(FormatUtil.print(cpu - prev_cpu, "#,##0")).append(TDE);
//					sb.append(TD);
//					toString(sb, (StepControl) sum);
//					sb.append(TDE);
//
//					truncated = true;
//
//					break;
//				}
//				continue;
//			}
//
//			StepSingle stepSingle = (StepSingle) profiles[i];
//			tm = stepSingle.start_time + stime;
//			cpu = stepSingle.start_cpu;
//			if (stepSingle.parent == -1) {
//				sb.append(TDC).append("-").append(TDE);
//			} else {
//				sb.append(TDC).append("<a href=\"#id").append(stepSingle.parent).append("\">").append(stepSingle.parent)
//						.append("</a>").append(TDE);
//			}
//
//			sb.append(TDC).append("<a name=\"#id").append(stepSingle.index).append("\">").append(stepSingle.index)
//					.append("</a>").append(TDE);
//			sb.append(TDC).append(DateUtil.getLogTime(tm)).append(TDE);
//
//			if (tm - prev_tm > 0) {
//				sb.append(TDC).append("<font color=red><b>").append(FormatUtil.print(tm - prev_tm, "#,##0"))
//						.append("</b></font>").append(TDE);
//			} else {
//				sb.append(TDC).append(FormatUtil.print(tm - prev_tm, "#,##0")).append(TDE);
//			}
//
//			if (cpu - prev_cpu > 0) {
//				sb.append(TDC).append("<font color=red><b>").append(FormatUtil.print(cpu - prev_cpu, "#,##0"))
//						.append("</b></font>").append(TDE);
//			} else {
//				sb.append(TDC).append(FormatUtil.print(cpu - prev_cpu, "#,##0")).append(TDE);
//			}
//
//			sb.append("<td>");
//
//			int space = 0;
//			if (indent.containsKey(stepSingle.parent)) {
//				space = indent.get(stepSingle.parent) + 1;
//			}
//			indent.put(stepSingle.index, space);
//			while (space > 0) {
//				sb.append("&nbsp;&nbsp;&nbsp;");
//				space--;
//			}
//
//			switch (stepSingle.getStepType()) {
//			case StepEnum.METHOD:
//				toString(sb, (MethodStep) stepSingle);
//				break;
//			case StepEnum.METHOD2:
//				toString(sb, (MethodStep) stepSingle);
//				MethodStep2 m2 = (MethodStep2) stepSingle;
//				if (m2.error != 0) {
//					sb.append(BR).append(TextProxy.error.getText(m2.error));
//				}
//				break;
//			case StepEnum.SQL:
//			case StepEnum.SQL2:
//			case StepEnum.SQL3:
//				SqlStep sql = (SqlStep) stepSingle;
//				toString(sb, sql, serverId);
//				if (sql.error != 0) {
//					sb.append(BR).append(TextProxy.error.getText(sql.error));
//				}
//				break;
//			case StepEnum.MESSAGE:
//				toString(sb, (MessageStep) stepSingle);
//				break;
//			case StepEnum.HASHED_MESSAGE:
//				toString(sb, (HashedMessageStep) stepSingle);
//				break;
//            case StepEnum.DUMP:
//                toString(sb, (DumpStep) stepSingle);
//                break;
//			case StepEnum.SOCKET:
//				SocketStep socket = (SocketStep) stepSingle;
//				toString(sb, socket);
//				if (socket.error != 0) {
//					sb.append(BR).append(TextProxy.error.getText(socket.error));
//				}
//				break;
//			}
//			sb.append(TDE);
//			prev_cpu = cpu;
//			prev_tm = tm;
//
//			sb.append(TABLE_ROW_END);
//		}
//
//		if (!truncated) {
//
//			tm = pack.endTime;
//			cpu = pack.cpu;
//
//			sb.append(TABLE_ROW_START);
//			sb.append(TDC).append(TDE);
//			sb.append(TDC).append(TDE);
//			sb.append(TDC).append(DateUtil.getLogTime(tm)).append(TDE);
//			sb.append(TDC).append(FormatUtil.print(tm - prev_tm, "#,##0")).append(TDE);
//			sb.append(TDC).append(FormatUtil.print(cpu - prev_cpu, "#,##0")).append(TDE);
//			sb.append(TD + "<font color='#0000ff'><b>end of transaction<b></font>").append(TDE);
//			sb.append(TABLE_ROW_END);
//
//		}
//		sb.append("</table>");
//		return sb.toString();
//	}

//	private static void toString(StringBuffer sb, SocketStep p) {
//		String ip = IPUtil.toString(p.ipaddr);
//		sb.append("socket: ").append(ip == null ? "unknown" : ip).append(":").append(p.port + " ");
//		if (p.elapsed > 0) {
//			sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0")).append("</b></font>");
//		} else {
//			sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//		}
//		sb.append(" ms");
//	}
//
//	private static void toString(StringBuffer sb, SocketSum p) {
//		String ip = IPUtil.toString(p.ipaddr);
//		sb.append("<td colspan=5>").append("socket: ").append(ip == null ? "unknown" : ip).append(":")
//				.append(p.port + " ");
//
//		sb.append(" count=").append("<font color=blue><b>").append(FormatUtil.print(p.count, "#,##0"))
//				.append("</b></font>");
//		sb.append(" time=");
//		if (p.elapsed > 0) {
//			sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0")).append("</b></font>");
//		} else {
//			sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//		}
//		sb.append(" ms");
//		if (p.error > 0) {
//			sb.append(" error=" + p.error);
//		}
//	}
//
//	private static void toString(StringBuffer sb, SqlStep p, int serverId) {
//		String m = TextProxy.sql.getText(p.hash);
//		if (m == null)
//			m = Hexa32.toString32(p.hash);
//		sb.append(m);
//		if (StringUtil.isEmpty(p.param) == false) {
//			sb.append(" [").append(p.param).append("]");
//		}
//		sb.append(" ");
//		if (p.elapsed > 0) {
//			sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0")).append("</b></font>");
//		} else {
//			sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//		}
//		sb.append(" ms");
//	}
//
//	private static void toString(StringBuffer sb, SqlSum p, int serverId) {
//		String m = TextProxy.sql.getText(p.hash);
//		if (m == null)
//			m = Hexa32.toString32(p.hash);
//		sb.append("<td colspan=5>").append(m);
//		if (StringUtil.isEmpty(p.param) == false) {
//			sb.append(" [").append(p.param).append("]");
//		}
//
//		sb.append(" count=").append("<font color=blue><b>").append(FormatUtil.print(p.count, "#,##0"))
//				.append("</b></font>");
//		sb.append(" time=");
//		if (p.elapsed > 0) {
//			sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0")).append("</b></font>");
//		} else {
//			sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//		}
//		sb.append(" ms");
//		sb.append(" cpu=").append("<font color=blue><b>").append(FormatUtil.print(p.cputime, "#,##0"))
//				.append("</b></font>");
//		if (p.error > 0) {
//			sb.append(" error=" + p.error);
//		}
//	}
//
//	private static void toString(StringBuffer sb, MessageStep p) {
//		sb.append("<font color=green>").append(p.message.replaceAll("\n", BR)).append("</font>");
//	}
//
//	private static void toString(StringBuffer sb, HashedMessageStep p) {
//		String m = TextProxy.hashMessage.getText(p.hash);
//		if (m == null)
//			m = Hexa32.toString32(p.hash);
//		sb.append("<font color=green>").append(m.replaceAll("\n", BR)).append("</font>");
//	}
//
//	private static void toString(StringBuffer sb, ParameterizedMessageStep pmStep) {
//		String messageFormat = TextProxy.hashMessage.getText(pmStep.getHash());
//		String message;
//		if (messageFormat == null) {
//			message = Hexa32.toString32(pmStep.getHash());
//		} else {
//			message = pmStep.buildMessasge(messageFormat);
//		}
//		sb.append("<font color=green>").append(message.replaceAll("\n", BR)).append("</font>");
//	}
//
//    private static void toString(StringBuffer sb, DumpStep p) {
//        sb.append("<font color=green>").append("<Thread dump>").append("</font>");
//    }
//
//	private static void toString(StringBuffer sb, StepControl p) {
//		sb.append("<font color=red>").append(p.message.replaceAll("\n", BR)).append("</font>");
//	}
//
//	private static void toString(StringBuffer sb, MethodStep p) {
//		String m = TextProxy.method.getText(p.hash);
//		if (m == null)
//			m = Hexa32.toString32(p.hash);
//		sb.append(m).append(" ");
//		if (p.elapsed > 0) {
//			sb.append("<font color=red><b>").append(FormatUtil.print(p.elapsed, "#,##0")).append("</b></font>");
//		} else {
//			sb.append(FormatUtil.print(p.elapsed, "#,##0"));
//		}
//
//		sb.append(" ms");
//	}

	public static XLogPack toXLogPack(Pack p) {
		switch (p.getPackType()) {
		case PackEnum.XLOG:
			return (XLogPack) p;
		default:
			return null;
		}
	}
}
