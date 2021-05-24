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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.model.XLogProxy;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.SqlMakerUtil;
import scouter.client.xlog.views.XLogProfileView;
import scouter.lang.CountryCode;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallStep2;
import scouter.lang.step.ApiCallSum;
import scouter.lang.step.CommonSpanStep;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.DumpStep;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodStep2;
import scouter.lang.step.MethodSum;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.lang.step.SocketStep;
import scouter.lang.step.SocketSum;
import scouter.lang.step.SpanCallStep;
import scouter.lang.step.SpanStep;
import scouter.lang.step.SqlStep;
import scouter.lang.step.SqlStep2;
import scouter.lang.step.SqlStep3;
import scouter.lang.step.SqlSum;
import scouter.lang.step.SqlXType;
import scouter.lang.step.Step;
import scouter.lang.step.StepControl;
import scouter.lang.step.StepEnum;
import scouter.lang.step.StepSingle;
import scouter.lang.step.StepSummary;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.lang.step.ThreadSubmitStep;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.SortUtil;
import scouter.util.StringUtil;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ProfileText {
	
	public static void build(final String date, StyledText text, XLogData xperf, Step[] profiles, int spaceCnt,
             int serverId) {
		 build(date, text, xperf, profiles, serverId, false);
	}

    public static void build(final String date, StyledText text, XLogData xperf, Step[] profiles,
                             int serverId, boolean bindSqlParam) {
        build(date, text, xperf, profiles, serverId, bindSqlParam, false);
    }

    public static Color getColor(ParameterizedMessageLevel level) {
        switch (level) {
            case DEBUG:
                return ColorUtil.getInstance().getColor("gray3");
            case INFO:
                return ColorUtil.getInstance().getColor("gunlee2");
            case WARN:
                return ColorUtil.getInstance().getColor("dark orange");
            case ERROR:
                return ColorUtil.getInstance().getColor("light red2");
            case FATAL:
                return ColorUtil.getInstance().getColor("red");
        }
        return ColorUtil.getInstance().getColor("dark gray");
    };

    public static void build(final String date, StyledText text, XLogData xperf, Step[] profiles,
                             int serverId, boolean bindSqlParam, boolean isSimplified) {

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

        Color dblue = text.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        Color dcyan = text.getDisplay().getSystemColor(SWT.COLOR_DARK_CYAN);
        Color dyellow = text.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
        Color dgray = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

        java.util.List<StyleRange> sr = new ArrayList<StyleRange>();

        int slen = 0;

        final StringBuffer sb = new StringBuffer();
        sb.append("► txid    = ");
        slen = sb.length();
        sb.append(Hexa32.toString32(xperf.p.txid)).append("\n");
        sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
        if (xperf.p.gxid != 0) {
            sb.append("► gxid    = ");
            slen = sb.length();
            sb.append(Hexa32.toString32(xperf.p.gxid));
            sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
            slen = sb.length();
            sb.append(" <<- click to open XLog flow map").append("\n");
            sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
        }
        if (xperf.p.caller != 0) {
            sb.append("► caller  = ");
            slen = sb.length();
            sb.append(Hexa32.toString32(xperf.p.caller)).append("\n");
            sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
        }
        sb.append("► objName = ").append(xperf.objName).append("\n");
        sb.append("► thread  = ").append(TextProxy.hashMessage.getLoadText(date, xperf.p.threadNameHash, serverId)).append("\n");
        sb.append("► endtime = ").append(FormatUtil.print(new Date(xperf.p.endTime), "yyyyMMdd HH:mm:ss.SSS")).append("\n");
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
        sb.append("kbytes=" + xperf.p.kbytes);
//		sb.append("bytes=" + xperf.p.bytes + ", ");
//		sb.append("status=" + xperf.p.status);
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
            sb.append("\n► webName=" + t).append("  webTime=" + xperf.p.webTime + " ms");
        }
        t = TextProxy.web.getLoadText(date, xperf.p.queuingHostHash, serverId);
        if (StringUtil.isNotEmpty(t)) {
            sb.append("\n► queuing=" + t).append("  time=" + xperf.p.queuingTime + " ms");
        }
        t = TextProxy.web.getLoadText(date, xperf.p.queuing2ndHostHash, serverId);
        if (StringUtil.isNotEmpty(t)) {
            sb.append("\n► 2nd-queuing=" + t).append("  time=" + xperf.p.queuing2ndTime + " ms");
        }
        t = TextProxy.login.getLoadText(date, xperf.p.login, serverId);
        if (StringUtil.isNotEmpty(t)) {
            sb.append("\n► login=" + t);
        }
        t = TextProxy.desc.getLoadText(date, xperf.p.desc, serverId);
        if (StringUtil.isNotEmpty(t)) {
            sb.append("\n► desc=" + t);
        }
        if (StringUtil.isNotEmpty(xperf.p.text1)) {
            sb.append("\n► text1=" + xperf.p.text1);
        }
        if (StringUtil.isNotEmpty(xperf.p.text2)) {
            sb.append("\n► text2=" + xperf.p.text2);
        }
        if (StringUtil.isNotEmpty(xperf.p.text3)) {
            sb.append("\n► text3=" + xperf.p.text3);
        }
        if (StringUtil.isNotEmpty(xperf.p.text4)) {
            sb.append("\n► text4=" + xperf.p.text4);
        }
        if (StringUtil.isNotEmpty(xperf.p.text5)) {
            sb.append("\n► text5=" + xperf.p.text5);
        }
        sb.append("\n► profileCount=" + xperf.p.profileCount);
        sb.append("\n► profileSize=" + xperf.p.profileSize);
        if (xperf.p.hasDump == 1) {
            sb.append("\n► dump=Y");
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
        sb.append(FormatUtil.print(new Date(stime), "HH:mm:ss.SSS"));
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

            if (truncated)
                break;

            if (profiles[i] instanceof StepSummary) {
                sb.append("        ").append(" ");
                sb.append(String.format("[%06d]", sumCnt++));
                sb.append(" ");

                StepSummary sum = (StepSummary) profiles[i];
                switch (sum.getStepType()) {
                    case StepEnum.METHOD_SUM:
                        XLogProfileView.isSummary = true;

                        MethodSum p = (MethodSum) sum;
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
                        sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
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

            if (stepSingle.getStepType() == StepEnum.THREAD_CALL_POSSIBLE) {
                ThreadCallPossibleStep threadStep = (ThreadCallPossibleStep)stepSingle;
                XLogData threadStepXlog = null;
                if(threadStep.txid != 0L) {
                    threadStepXlog = XLogProxy.getXLogData(xperf.serverId, DateUtil.yyyymmdd(xperf.p.endTime), threadStep.txid);
                }
                if(threadStepXlog != null) {
                    threadStep.threaded = 1;
                    threadStep.elapsed = threadStepXlog.p.elapsed;
                }

                if(threadStep.threaded == 0) {
                    continue;
                }
            }

            tm = stepSingle.start_time + stime;
            cpu = stepSingle.start_cpu;
            boolean ignoreCpu = false;
            if(cpu < 0) ignoreCpu = true;

            // sr.add(style(sb.length(), 6, blue, SWT.NORMAL));
            int p1 = sb.length();
            String pid = String.format("[%06d]", stepSingle.parent);
            sb.append((stepSingle.parent == -1) ? "    -   " : pid);
            sb.append(" ");
            sb.append(String.format("[%06d]", stepSingle.index));
            sb.append(" ");
            sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
            sb.append("   ");

            slen = sb.length();
            sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));

            int gapTime = CastUtil.cint(tm - prev_tm);
            int elapsedRate = xperf.p.elapsed == 0 ? 0 : CastUtil.cint((gapTime / (double)xperf.p.elapsed)*100);


            if (elapsedRate > 50) {
                sr.add(style(slen, sb.length() - slen, dred, SWT.BOLD));
            } else if (elapsedRate > 20) {
                sr.add(style(slen, sb.length() - slen, dblue, SWT.BOLD));
            } else if (elapsedRate > 10) {
                sr.add(style(slen, sb.length() - slen, dgreen, SWT.BOLD));
            }

            sb.append(" ");
            if(ignoreCpu) {
            	sb.append(String.format("%6s", FormatUtil.print(0, "#,##0")));
            } else {
            	sb.append(String.format("%6s", FormatUtil.print(XLogUtil.getCpuTime(stepSingle), "#,##0")));
            }
            sb.append("  ");
            int lineHead = sb.length() - p1;

            int space = 0;
            if (indent.containsKey(stepSingle.parent)) {
                space = indent.get(stepSingle.parent) + 1;
            }
            indent.put(stepSingle.index, space);
            lineHead += space;
            while (space > 0) {
                sb.append(" ");
                space--;
            }

            int dotPos;
            switch (stepSingle.getStepType()) {
                case StepEnum.METHOD:
                    slen = sb.length();
                    dotPos = toString(sb, (MethodStep) stepSingle, isSimplified);

                    sr.add(style(slen, 1, dyellow, SWT.BOLD));
                    if(isSimplified && dotPos > 0) {
                        sr.add(style(slen+1, dotPos, dyellow, SWT.NORMAL));
                        sr.add(style(slen+dotPos+1, 1, dyellow, SWT.BOLD));
                        sr.add(style(slen+dotPos+2, sb.length() - (slen+dotPos+2), dyellow, SWT.NORMAL));
                    } else {
                        sr.add(style(slen+1, sb.length() - slen+1, dyellow, SWT.NORMAL));
                    }
                    break;
                case StepEnum.METHOD2:
                    slen = sb.length();
                    dotPos = toString(sb, (MethodStep) stepSingle, isSimplified);
                    sr.add(style(slen, 1, dyellow, SWT.BOLD));
                    sr.add(style(slen+1, sb.length() - slen+1, dyellow, SWT.NORMAL));
                    //sr.add(style(slen+dotPos, 1, dyellow, SWT.BOLD));
                    MethodStep2 m2 = (MethodStep2) stepSingle;
                    if (m2.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(m2.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    break;
                case StepEnum.SPAN:
                    slen = sb.length();
                    toString(sb, (SpanStep) stepSingle);
                    sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
                    SpanStep spanStep = (SpanStep) stepSingle;
                    if (spanStep.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(spanStep.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    if (spanStep.localEndpointServiceName != 0) {
                        slen = sb.length();
                        StringBuilder tempSb = appendSpanEndpoints(date, serverId, spanStep);
                        sb.append(spacingToNewLine(tempSb.toString(), lineHead + 4));
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
                    }
                    if (spanStep.tags.size() > 0) {
                        slen = sb.length();
                        appendSpanTags(sb, lineHead, spanStep);
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
                    }
                    if (spanStep.annotationTimestamps.size() > 0) {
                        slen = sb.length();
                        appendSpanAnnotations(sb, lineHead, spanStep);
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
                    }
                    break;
                case StepEnum.SQL:
                case StepEnum.SQL2:
                case StepEnum.SQL3:
                    SqlStep sql = (SqlStep) stepSingle;
                    slen = sb.length();
                    toString(sb, sql, serverId, lineHead, bindSqlParam);
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
                case StepEnum.HASHED_MESSAGE:
                    slen = sb.length();
                    toString(sb, (HashedMessageStep) stepSingle);
                    sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
                    break;
                case StepEnum.PARAMETERIZED_MESSAGE:
                    slen = sb.length();
                    ParameterizedMessageStep pmStep = (ParameterizedMessageStep) stepSingle;
                    toString(sb, pmStep);
                    sr.add(style(slen, sb.length() - slen, getColor(pmStep.getLevel()), SWT.NORMAL));
                    break;
                case StepEnum.DUMP:
                    slen = sb.length();
                    toString(sb, (DumpStep) stepSingle, lineHead);
                    sr.add(style(slen, sb.length() - slen, dgray, SWT.NORMAL));
                    break;
                case StepEnum.APICALL:
                case StepEnum.APICALL2:
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
                case StepEnum.DISPATCH:
                    DispatchStep dispatchStep = (DispatchStep) stepSingle;
                    slen = sb.length();
                    toString(sb, dispatchStep);
                    sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
                    if (dispatchStep.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(dispatchStep.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    break;
                case StepEnum.THREAD_CALL_POSSIBLE:
                    ThreadCallPossibleStep tcSteap = (ThreadCallPossibleStep) stepSingle;
                    slen = sb.length();
                    toString(sb, tcSteap);
                    sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
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
                case StepEnum.SPANCALL:
                    SpanCallStep spanCall = (SpanCallStep) stepSingle;
                    slen = sb.length();
                    toString(sb, spanCall);
                    sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
                    if (spanCall.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(spanCall.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    if (spanCall.localEndpointServiceName != 0) {
                        slen = sb.length();
                        StringBuilder tempSb = appendSpanEndpoints(date, serverId, spanCall);
                        sb.append(spacingToNewLine(tempSb.toString(), lineHead + 4));
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
                    }
                    if (spanCall.tags.size() > 0) {
                        slen = sb.length();
                        appendSpanTags(sb, lineHead, spanCall);
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
                    }
                    if (spanCall.annotationTimestamps.size() > 0) {
                        slen = sb.length();
                        appendSpanAnnotations(sb, lineHead, spanCall);
                        sr.add(style(slen, sb.length() - slen, dyellow, SWT.NORMAL));
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
            if(!ignoreCpu) {
            	prev_cpu = cpu;
            }
            prev_tm = tm;
        }

        if (!truncated) {

            tm = xperf.p.endTime;
            cpu = xperf.p.cpu;
            sb.append("        ");
            sb.append(" ");
            sb.append("[******]");
            sb.append(" ");
            sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
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

    private static void appendSpanAnnotations(StringBuffer sb, int lineHead, CommonSpanStep spanStep) {
        try {
            sb.append(spacingToNewLine(">[annotations]", lineHead + 4));
            for (int annotCount = 0; annotCount < spanStep.annotationTimestamps.size(); annotCount++) {
                String annotMessage = FormatUtil.print(new Date(spanStep.annotationTimestamps.getLong(annotCount)), "HH:mm:ss.SSS")
                        + " " + spanStep.annotationValues.getString(annotCount);
                sb.append(spacing(annotMessage, lineHead + 11));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void appendSpanTags(StringBuffer sb, int lineHead, CommonSpanStep spanStep) {
        String tagsString = spanStep.tags.keySet().stream()
                .map(k -> k + ": " + spanStep.tags.getText(k))
                .collect(Collectors.joining("\n"));

        sb.append(spacingToNewLine(">[tags]", lineHead + 4));
        sb.append(spacing(tagsString, lineHead + 11));
    }

    private static StringBuilder appendSpanEndpoints(String date, int serverId, CommonSpanStep spanStep) {
        StringBuilder tempSb = new StringBuilder();
        tempSb.append(">[LE]").append(TextProxy.object.getLoadText(date, spanStep.localEndpointServiceName, serverId))
                .append(":").append(IPUtil.toString(spanStep.localEndpointIp))
                .append(":").append(spanStep.localEndpointPort);
        if (spanStep.remoteEndpointServiceName != 0) {
            tempSb.append("[RE]").append(TextProxy.object.getLoadText(date, spanStep.localEndpointServiceName, serverId))
                    .append(":").append(IPUtil.toString(spanStep.remoteEndpointIp))
                    .append(":").append(spanStep.remoteEndpointPort);
        }
        return tempSb;
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
                        MethodSum p = (MethodSum) sum;
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
                        sb.append(FormatUtil.print(new Date(data.p.endTime), "HH:mm:ss.SSS"));
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

            if (stepSingle.getStepType() == StepEnum.THREAD_CALL_POSSIBLE) {
                if(((ThreadCallPossibleStep)stepSingle).threaded == 0) continue;
            }

            tm = stime + stepSingle.start_time;
            cpu = stepSingle.start_cpu;

            // sr.add(style(sb.length(), 6, blue, SWT.NORMAL));
            int p1 = sb.length();
            String pid = String.format("[%06d]", stepSingle.parent);
            sb.append((stepSingle.parent == -1) ? "    -   " : pid);
            sb.append(" ");
            sb.append(String.format("[%06d]", stepSingle.index));
            sb.append(" ");
            sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
            sb.append("   ");
            sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
            sb.append(" ");
            if (prev_cpu == -1) {
                sb.append(String.format("%6s", FormatUtil.print(0, "#,##0")));
            } else {
                sb.append(String.format("%6s", FormatUtil.print(XLogUtil.getCpuTime(stepSingle), "#,##0")));
            }

            sb.append("  ");
            int lineHead = sb.length() - p1;

            int space = 0;
            if (indent.containsKey(stepSingle.parent)) {
                space = indent.get(stepSingle.parent) + 1;
            }
            indent.put(stepSingle.index, space);
            lineHead += space;
            while (space > 0) {
                sb.append(" ");
                space--;
            }

            switch (stepSingle.getStepType()) {
                case StepEnum.METHOD:
                    toString(sb, (MethodStep) stepSingle, false);
                    break;
                case StepEnum.METHOD2:
                    toString(sb, (MethodStep) stepSingle, false);
                    MethodStep2 m2 = (MethodStep2) stepSingle;
                    if (m2.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(m2.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    break;
                case StepEnum.SQL:
                case StepEnum.SQL2:
                case StepEnum.SQL3:
                    SqlStep sql = (SqlStep) stepSingle;
                    slen = sb.length();
                    toString(sb, sql, serverId, lineHead, false);
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
                case StepEnum.HASHED_MESSAGE:
                    slen = sb.length();
                    toString(sb, (HashedMessageStep) stepSingle);
                    sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
                    break;
                case StepEnum.PARAMETERIZED_MESSAGE:
                    slen = sb.length();
                    ParameterizedMessageStep pmStep = (ParameterizedMessageStep) stepSingle;
                    toString(sb, pmStep);
                    sr.add(style(slen, sb.length() - slen, getColor(pmStep.getLevel()), SWT.NORMAL));
                    break;
                case StepEnum.DUMP:
                    slen = sb.length();
                    toString(sb, (DumpStep) stepSingle, lineHead);
                    sr.add(style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
                    break;
                case StepEnum.APICALL:
                case StepEnum.APICALL2:
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
                case StepEnum.DISPATCH:
                    DispatchStep step = (DispatchStep) stepSingle;
                    slen = sb.length();
                    toString(sb, step);
                    sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
                    if (step.error != 0) {
                        slen = sb.length();
                        sb.append("\n").append(TextProxy.error.getText(step.error));
                        sr.add(style(slen, sb.length() - slen, red, SWT.NORMAL));
                    }
                    break;
                case StepEnum.THREAD_CALL_POSSIBLE:
                    ThreadCallPossibleStep tcSteap = (ThreadCallPossibleStep) stepSingle;
                    slen = sb.length();
                    toString(sb, tcSteap);
                    sr.add(underlineStyle(slen, sb.length() - slen, dmagenta, SWT.NORMAL, SWT.UNDERLINE_LINK));
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
            if(p instanceof ApiCallStep2) {
                if(((ApiCallStep2)p).async == 1) {
                    sb.append(" [async]");
                }
            }
            if(p.address != null) {
                sb.append(" [" + p.address + "]");
            }
        }
        if (p.txid != 0) {
            sb.append(" <" + Hexa32.toString32(p.txid) + ">");
        }
    }

    public static void toString(StringBuffer sb, SpanCallStep p) {
        String m = TextProxy.service.getText(p.hash);
        if (m == null)
            m = Hexa32.toString32(p.hash);
        sb.append("call: ").append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
        if (p.txid != 0) {
            if(p.address != null) {
                sb.append(" [" + p.address + "]");
            }
        }
        if (p.txid != 0) {
            sb.append(" <" + Hexa32.toString32(p.txid) + ">");
        }
    }

    public static void toString(StringBuffer sb, DispatchStep step) {
        String m = TextProxy.apicall.getText(step.hash);
        if (m == null)
            m = Hexa32.toString32(step.hash);
        sb.append("call: ").append(m).append(" ").append(FormatUtil.print(step.elapsed, "#,##0")).append(" ms");
        if (step.txid != 0) {
            sb.append(" <" + Hexa32.toString32(step.txid) + ">");
        }
    }

    public static void toString(StringBuffer sb, ThreadCallPossibleStep step) {
        String m = TextProxy.apicall.getText(step.hash);
        if (m == null)
            m = Hexa32.toString32(step.hash);
        sb.append("call:thread: ").append(m).append(" ").append(FormatUtil.print(step.elapsed, "#,##0")).append(" ms");
        if (step.txid != 0) {
            sb.append(" <" + Hexa32.toString32(step.txid) + ">");
        }
    }

    public static void toString(StringBuffer sb, HashedMessageStep p) {
        String m = TextProxy.hashMessage.getText(p.hash);
        if (m == null)
            m = Hexa32.toString32(p.hash);

        if(p.time != -1) {
            sb.append(m).append(" #").append(FormatUtil.print(p.value, "#,##0")).append(" ").append(FormatUtil.print(p.time, "#,##0")).append(" ms");
        } else {
            sb.append(m);
        }
    }

    public static void toString(StringBuffer sb, ParameterizedMessageStep pmStep) {
        String messageFormat = TextProxy.hashMessage.getText(pmStep.getHash());
        String message;
        if (messageFormat == null) {
            message = Hexa32.toString32(pmStep.getHash());
        } else {
            message = pmStep.buildMessasge(messageFormat);
        }

        sb.append(message);
        if(pmStep.getElapsed() != -1) {
            sb.append(" [").append(FormatUtil.print(pmStep.getElapsed(), "#,##0")).append(" ms]");
        }
    }

    public static void toString(StringBuffer sb, DumpStep p, int lineHead) {
        sb.append("<auto generated thread dump>:[").append(p.threadId).append("] ").append(p.threadName).append('\n');
        sb.append(StringUtil.leftPad("", lineHead)).append("   -> State : ").append(p.threadState).append('\n');
        if(StringUtil.isNotEmpty(p.lockName)) {
            sb.append(StringUtil.leftPad("", lineHead)).append("   -> Lock : ").append(p.lockName).append('\n');
        }
        if(StringUtil.isNotEmpty(p.lockOwnerName)) {
            sb.append(StringUtil.leftPad("", lineHead)).append("   -> Lock Owner : ").append(p.lockOwnerName).append('\n');
        }
        if(p.lockOwnerId > 0) {
            sb.append(StringUtil.leftPad("", lineHead)).append("   -> Lock Owner Id ").append(p.lockOwnerId).append('\n');
        }

        for(int stackElementHash : p.stacks) {
            String m = TextProxy.stackElement.getText(stackElementHash);
            if(m == null) {
                m = Hexa32.toString32(stackElementHash);
            }
            sb.append(StringUtil.leftPad("", lineHead)).append(m).append('\n');
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
        sb.append("socket: ").append(ip == null ? "unknown" : ip).append(":").append(p.port + " ")
                .append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
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

    public static void toString(StringBuffer sb, SqlStep p, int serverId, int lineHead, boolean bindParam) {
        if (p instanceof SqlStep2) {
            sb.append(SqlXType.toString(((SqlStep2) p).xtype));
        }
        String m = TextProxy.sql.getText(p.hash);
        m = spacing(m, lineHead);
        if (m == null)
            m = Hexa32.toString32(p.hash);
        Server server = ServerManager.getInstance().getServer(serverId);
        boolean showParam = true;
        if (server != null) {
            showParam = server.isAllowAction(GroupPolicyConstants.ALLOW_SQLPARAMETER);
        }
        if (showParam && bindParam) {
        	sb.append(SqlMakerUtil.replaceSQLParameter(m, p.param));
        } else {
        	sb.append(m);
        }
        if (StringUtil.isEmpty(p.param) == false) {
            if (bindParam == false) {
	            sb.append("\n").append(StringUtil.leftPad("", lineHead));
	            sb.append("[").append(showParam ? p.param : "******").append("]");
            }
        }
        if (p instanceof SqlStep3) {
            int updatedCount = ((SqlStep3) p).updated;
            if (updatedCount > SqlStep3.EXECUTE_RESULT_SET) {
                sb.append(" <Affected Rows : " + updatedCount + ">");
            } else if (updatedCount == SqlStep3.EXECUTE_UNKNOWN_COUNT) {
                sb.append(" <Affected Rows : unknown>");
            }
        }
        sb.append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
    }

    public static String spacing(String m, int lineHead) {
        if (m == null)
            return m;
        String dummy = StringUtil.leftPad("", lineHead);
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader sr = new BufferedReader(new StringReader(m));
            String s = null;
            while ((s = sr.readLine()) != null) {
                s = StringUtil.trim(s);
                if (s.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append("\n").append(dummy);
                    }
                    sb.append(s);
                }

            }
        } catch (Exception e) {
        }
        return sb.toString();
    }

    public static String spacingToNewLine(String m, int lineHead) {
        if (m == null)
            return m;
        String dummy = StringUtil.leftPad("", lineHead);
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader sr = new BufferedReader(new StringReader(m));
            String s = null;
            while ((s = sr.readLine()) != null) {
                if (s.length() > 0) {
                    sb.append("\n").append(dummy).append(s);
                }
            }
        } catch (Exception e) {
        }
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

    /**
     * @return class and method deliminator position ( Class.method -> return 5)
     */
    public static int toString(StringBuffer sb, MethodStep p, boolean isSimplified) {
        String m = TextProxy.method.getText(p.hash);
        if (m == null) {
            m = Hexa32.toString32(p.hash);
        }

        if(isSimplified) {
            String simple = simplifyMethod(m);
            sb.append(simple).append(" [").append(FormatUtil.print(p.elapsed, "#,##0")).append("ms]").append(" -- ").append(m);
            return simple.indexOf('#');
        } else {
            sb.append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
            return m.indexOf('.');
        }
    }

    public static void toString(StringBuffer sb, SpanStep p) {
        String m = TextProxy.service.getText(p.hash);
        if (m == null) {
            m = Hexa32.toString32(p.hash);
        }
        sb.append(m).append(" ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
    }

    public static String simplifyMethod(String method) {
        String[] parts = StringUtil.split(method, '.');
        if(parts.length >= 2) {
            String methodName = parts[parts.length - 1];
            int bracePos = methodName.indexOf('(');

            if(bracePos <= 0) return method;

            return parts[parts.length - 2] + "#" + methodName.substring(0, bracePos) + "()";
        } else {
            return method;
        }
    }

    public static StyleRange style(int start, int length, Color c, int f) {
        StyleRange t = new StyleRange();
        t.start = start;
        t.length = length;
        t.foreground = c;
        t.fontStyle = f;
        return t;
    }

    public static StyleRange style(int start, int length, int f) {
        StyleRange t = new StyleRange();
        t.start = start;
        t.length = length;
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
