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
import org.eclipse.swt.widgets.Button;
import scouter.client.model.TextProxy;
import scouter.client.util.StepWrapper;
import scouter.client.xlog.views.XLogProfileView;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.*;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;

public class ProfileTextFull {
	
	public static void buildXLogData(final String date, StyledText text, XLogPack p, int serverId) {
		
		final int servId = serverId;
		
		String error = TextProxy.error.getLoadText(date, p.error, servId);
		String objName = TextProxy.object.getLoadText(date, p.objHash, servId);
		
		int slen = 0;
		java.util.List<StyleRange> sr = new ArrayList<StyleRange>();
		
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
		
		final StringBuffer sb = new StringBuffer();
		sb.append("► txid    = ").append(Hexa32.toString32(p.txid)).append("\n");
		sb.append("► objName = ").append(objName).append("\n");
		sb.append("► endtime = ").append(FormatUtil.print(new Date(p.endTime), "yyyyMMdd HH:mm:ss.SSS")).append("\n");
		sb.append("► elapsed = ").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms\n");
		sb.append("► service = ").append(TextProxy.service.getText(p.service)).append("\n");
		if (error != null) {
			sb.append("► error   = ");
			slen = sb.length();
			sb.append(error).append("\n");
			sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
		}
		
		sb.append("► ipaddr=" + IPUtil.toString(p.ipaddr) + ", ");
		sb.append("userid=" + p.userid);
		sb.append("\n► cpu=" + FormatUtil.print(p.cpu, "#,##0") + " ms, ");
		sb.append("kbytes=" + p.kbytes + ", ");
		sb.append("status=" + p.status);
		if (p.sqlCount > 0) {
			sb.append("\n► sqlCount=" + p.sqlCount + ", ");
			sb.append("sqlTime=" + FormatUtil.print(p.sqlTime, "#,##0") + " ms");
		}
		if (p.apicallCount > 0) {
			sb.append("\n► ApiCallCount=" + p.apicallCount + ", ");
			sb.append("ApiCallTime=" + FormatUtil.print(p.apicallTime, "#,##0") + " ms");
		}
		
		String t = TextProxy.userAgent.getLoadText(date, p.userAgent, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► userAgent=" + t);
		}
		
		t = TextProxy.referer.getLoadText(date, p.referer, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► referer=" + t);
		}
		
		t = TextProxy.group.getLoadText(date, p.group, serverId);
		if (StringUtil.isNotEmpty(t)) {
			sb.append("\n► group=" + t);
		}

		if (p.hasDump == 1) {
			sb.append("\n► dump=Y");
		}
		
		sb.append("\n");
		
		text.setText(sb.toString());
		text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
	}
	
	public static void buildProfile(final String date, StyledText text, XLogPack pack, StepWrapper[] orgProfiles, int pageNum, int rowPerPage, Button prevBtn, Button nextBtn, Button startBtn, Button endBtn, int length, int serverId, int searchLineIndex, boolean isSummary) {
		
		String spaceStr = "";
		String astarStr = "";
		for(int j = 0 ; j < length ; j++){
			spaceStr += " ";
			astarStr += "*";
		}
		
		if (orgProfiles == null) {
			orgProfiles = new StepWrapper[0];
		}
		
		boolean lastPage = false;
		int startIdx = pageNum*rowPerPage;
		int lastIdx = (pageNum*rowPerPage)+rowPerPage;
		if(lastIdx >= orgProfiles.length){
			lastIdx = orgProfiles.length;
			lastPage = true;
		}
		
		int ix = 0;
		StepWrapper[] profiles = new StepWrapper[lastIdx - startIdx];
		for(int inx = startIdx ; inx < lastIdx; inx++){
			profiles[ix] = orgProfiles[inx];
			ix++;
		}
		
		int slen = 0;
		java.util.List<StyleRange> sr = new ArrayList<StyleRange>();
		
		Color blue = text.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		Color dmagenta = text.getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
		Color yellow = text.getDisplay().getSystemColor(SWT.COLOR_YELLOW);

		Color dgreen = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		
		final StringBuffer sb = new StringBuffer();
		
		sb.append("------------------------------------------------------------------------------------------\n");
		sb.append("  p# "+spaceStr+"# "+spaceStr+" TIME         T-GAP   CPU          CONTENTS\n");
		sb.append("------------------------------------------------------------------------------------------\n");
		
		if (profiles.length == 0) {
			sb.append("\n                     ( No xlog profile collected ) ");
			text.setText(sb.toString());
			text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
			return;
		}
		long stime = pack.endTime - pack.elapsed;
		long prev_tm = stime;
		if(pageNum > 0){
			prev_tm = orgProfiles[startIdx - 1].time;
		}
		long prev_cpu = 0;
		if(pageNum > 0){
			prev_cpu = orgProfiles[startIdx - 1].cpu;
		}

		if(pageNum == 0){
			sb.append(" "+spaceStr+" ");
			sb.append(" ");
			sb.append("["+astarStr+"]");
			sb.append(" ");
			sb.append(FormatUtil.print(new Date(stime), "HH:mm:ss.SSS"));
			sb.append("   ");
			sb.append(String.format("%6s", "0"));
			sb.append(" ");
			sb.append(String.format("%6s", "0"));
			sb.append("  start transaction \n");
		}
		
		long tm = pack.endTime;
		long cpu = pack.cpu;
		
		for (int i = 0; i < profiles.length; i++) {
			
			if (profiles[i].step instanceof StepSummary) {
				int p1=sb.length();
				sb.append(spaceStr).append("   ");
				sb.append(String.format("[%0"+length+"d]", profiles[i].sSummaryIdx));
				sb.append(" ");
				int lineHead=sb.length()-p1;
				
				StepSummary sum = (StepSummary) profiles[i].step;
				switch (sum.getStepType()) {
				case StepEnum.METHOD_SUM:
					MethodSum p= (MethodSum) sum;
					slen = sb.length();
					
					String m = TextProxy.method.getText(p.hash);
					if (m == null)
						m = Hexa32.toString32(p.hash);
					sb.append(m).append(" ");
					
					if(searchLineIndex == profiles[i].sSummaryIdx){
						sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
					}else{
						sr.add(ProfileText.style(slen, sb.length() - slen, blue, SWT.NORMAL));
					}
					
					sb.append(" count=").append(FormatUtil.print(p.count, "#,##0"));
					sb.append(" time=").append(FormatUtil.print(p.elapsed, "#,##0")).append(" ms");
					sb.append(" cpu=").append(FormatUtil.print(p.cputime, "#,##0"));
					
					sb.append("\n");
					break;
				case StepEnum.SQL_SUM:
					SqlSum sql = (SqlSum) sum;
					slen = sb.length();
					ProfileText.toString(sb, sql, serverId);
					if(searchLineIndex == profiles[i].sSummaryIdx){
						sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
					}else{
						sr.add(ProfileText.style(slen, sb.length() - slen, blue, SWT.NORMAL));
					}
					sb.append("\n");
					break;
				case StepEnum.APICALL_SUM:
					ApiCallSum apicall = (ApiCallSum) sum;
					slen = sb.length();
					ProfileText.toString(sb, apicall);
					if(searchLineIndex == profiles[i].sSummaryIdx){
						sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
					}else{
						sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					}
					sb.append("\n");
					break;
				case StepEnum.SOCKET_SUM:
					XLogProfileView.isSummary = true;
					SocketSum socketSum = (SocketSum) sum;
					slen = sb.length();
					ProfileText.toString(sb, socketSum);
					sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
					sb.append("\n");
					break;
				}
				continue;
			}
			
			StepSingle stepSingle = (StepSingle) profiles[i].step;

			if (stepSingle.getStepType() == StepEnum.THREAD_CALL_POSSIBLE) {
				if(((ThreadCallPossibleStep)stepSingle).threaded == 0) continue;
			}

			tm = profiles[i].time;
			cpu = profiles[i].cpu;

			int p1=sb.length();
			String pid = String.format("[%0"+length+"d]", stepSingle.parent);
			sb.append((stepSingle.parent == -1) ? spaceStr+"  " : pid);
			sb.append(" ");
			sb.append(String.format("[%0"+length+"d]", stepSingle.index));
			sb.append(" ");
			sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
			sb.append("   ");
			sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
			sb.append(" ");
			sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
			sb.append("  ");
			int lineHead=sb.length()-p1;

			int space = profiles[i].indent;
			while (space > 0) {
				sb.append(" ");
				space--;
			}

			switch (stepSingle.getStepType()) {
			case StepEnum.METHOD:
				slen = sb.length();
				ProfileText.toString(sb, (MethodStep) stepSingle, false);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}
				break;
			case StepEnum.METHOD2:
				slen = sb.length();
				ProfileText.toString(sb, (MethodStep) stepSingle, false);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}
				MethodStep2 m2 =  (MethodStep2) stepSingle;
				if (m2.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(m2.error));
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;			
			case StepEnum.SQL:
			case StepEnum.SQL2:
			case StepEnum.SQL3:
				SqlStep sql = (SqlStep) stepSingle;
				slen = sb.length();
				ProfileText.toString(sb, sql, serverId, lineHead, false);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, blue, SWT.NORMAL));
				}
				if (sql.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(sql.error));
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.MESSAGE:
				slen = sb.length();
				ProfileText.toString(sb, (MessageStep) stepSingle);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
				}
				break;
			case StepEnum.HASHED_MESSAGE:
				slen = sb.length();
				ProfileText.toString(sb, (HashedMessageStep) stepSingle);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, dgreen, SWT.NORMAL));
				}
				break;
			case StepEnum.PARAMETERIZED_MESSAGE:
				slen = sb.length();
				ParameterizedMessageStep pmStep = (ParameterizedMessageStep) stepSingle;
				ProfileText.toString(sb, pmStep);
				if(searchLineIndex == stepSingle.index) {
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				} else {
					sr.add(ProfileText.style(slen, sb.length() - slen, ProfileText.getColor(pmStep.getLevel()), SWT.NORMAL));
				}
				break;
			case StepEnum.APICALL:
			case StepEnum.APICALL2:
				ApiCallStep apicall = (ApiCallStep) stepSingle;
				slen = sb.length();
				ProfileText.toString(sb, apicall);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				}
				if (apicall.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(apicall.error));
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.DISPATCH:
				DispatchStep step = (DispatchStep) stepSingle;
				slen = sb.length();
				ProfileText.toString(sb, step);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				}
				if (step.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(step.error));
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			case StepEnum.THREAD_CALL_POSSIBLE:
				ThreadCallPossibleStep tcStep = (ThreadCallPossibleStep) stepSingle;
				slen = sb.length();
				ProfileText.toString(sb, tcStep);
				if(searchLineIndex == stepSingle.index){
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.BOLD, yellow));
				}else{
					sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				}
				break;
			case StepEnum.SOCKET:
				SocketStep socket = (SocketStep) stepSingle;
				slen = sb.length();
				ProfileText.toString(sb, socket);
				sr.add(ProfileText.style(slen, sb.length() - slen, dmagenta, SWT.NORMAL));
				if (socket.error != 0) {
					slen = sb.length();
					sb.append("\n").append(TextProxy.error.getText(socket.error));
					sr.add(ProfileText.style(slen, sb.length() - slen, red, SWT.NORMAL));
				}
				break;
			}
			sb.append("\n");
			prev_cpu = cpu;
			prev_tm = tm;
			
		}

		if(lastPage){
		
			nextBtn.setEnabled(false);
			endBtn.setEnabled(false);
			
			tm = pack.endTime;
			cpu = pack.cpu;
			// slen = sb.length();
			sb.append(" "+spaceStr+" ");
			sb.append(" ");
			sb.append("["+astarStr+"]");
			sb.append(" ");
			sb.append(FormatUtil.print(new Date(tm), "HH:mm:ss.SSS"));
			sb.append("   ");
			if(!isSummary){
				sb.append(String.format("%6s", FormatUtil.print(tm - prev_tm, "#,##0")));
			}else{
				sb.append(String.format("%6s",FormatUtil.print(pack.elapsed, "#,##0")));
			}
			sb.append(" ");
			sb.append(String.format("%6s", FormatUtil.print(cpu - prev_cpu, "#,##0")));
			sb.append("  end of transaction \n");
		
			// sr.add(ProfileText.style(slen, sb.length() - slen, dblue, SWT.NORMAL));
			sb.append("------------------------------------------------------------------------------------------\n");
		}else{
			nextBtn.setEnabled(true);
			endBtn.setEnabled(true);
		}
		
		if(pageNum == 0){
			prevBtn.setEnabled(false);
			startBtn.setEnabled(false);
		}else{
			prevBtn.setEnabled(true);
			startBtn.setEnabled(true);
		}
		text.setText(sb.toString());
		text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
		
	}
}
