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
 */
package scouter.agent.summary;

import java.util.Enumeration;

import scouter.agent.Configure;
import scouter.lang.SummaryEnum;
import scouter.lang.pack.SummaryPack;
import scouter.lang.value.ListValue;
import scouter.util.BitUtil;
import scouter.util.LongKeyLinkedMap;

public class EndUserSummary {

	private static EndUserSummary instance = null;

	public final static synchronized EndUserSummary getInstance() {
		if (instance == null) {
			instance = new EndUserSummary();
		}
		return instance;
	}

	private Configure conf = Configure.getInstance();

	public void process(EndUserNavigationData p) {
		if (conf.summary_enabled == false)
			return;
		// service summary
		long key = BitUtil.composite(p.uri, p.ip);
		EndUserNavigationData d = navTable.get(key);
		if (d == null) {
			navTable.put(key, p);
			return;
		}
		d.count += p.count;
		d.unloadEventStart += p.unloadEventStart;
		d.unloadEventEnd += p.unloadEventEnd;
		d.fetchStart += p.fetchStart;
		d.domainLookupStart += p.domainLookupStart;
		d.domainLookupEnd += p.domainLookupEnd;
		d.connectStart += p.connectStart;
		d.connectEnd += p.connectEnd;
		d.requestStart += p.requestStart;
		d.responseStart += p.responseStart;
		d.responseEnd += p.responseEnd;
		d.domLoading += p.domLoading;
		d.domInteractive += p.domInteractive;
		d.domContentLoadedEventStart += p.domContentLoadedEventStart;
		d.domContentLoadedEventEnd += p.domContentLoadedEventEnd;
		d.domComplete += p.domComplete;
		d.loadEventStart += p.loadEventStart;
		d.loadEventEnd += p.loadEventEnd;
	}

	public void process(EndUserErrorData p) {
		if (conf.summary_enabled == false)
			return;
		long key = BitUtil.composite(p.stacktrace, p.userAgent);
		EndUserErrorData d = errorTable.get(key);
		if (d == null) {
			errorTable.put(key, p);
			return;
		}
		d.count += p.count;
	}

	public void process(EndUserAjaxData p) {
		if (conf.summary_enabled == false)
			return;
		long key = BitUtil.composite(p.uri, p.ip);
		EndUserAjaxData d = ajaxTable.get(key);
		if (d == null) {
			ajaxTable.put(key, p);
			return;
		}
		d.count += p.count;
		d.duration += p.duration;
	}

	private LongKeyLinkedMap<EndUserNavigationData> navTable = new LongKeyLinkedMap<EndUserNavigationData>()
			.setMax(conf.summary_enduser_nav_max_count);
	private LongKeyLinkedMap<EndUserAjaxData> ajaxTable = new LongKeyLinkedMap<EndUserAjaxData>()
			.setMax(conf.summary_enduser_ajax_max_count);
	private LongKeyLinkedMap<EndUserErrorData> errorTable = new LongKeyLinkedMap<EndUserErrorData>()
			.setMax(conf.summary_enduser_error_max_count);

	public SummaryPack getAndClearNavTable() {

		if (navTable.size() == 0)
			return null;

		LongKeyLinkedMap<EndUserNavigationData> temp = navTable;
		navTable = new LongKeyLinkedMap<EndUserNavigationData>().setMax(conf.summary_enduser_nav_max_count);

		SummaryPack p = new SummaryPack();
		p.stype = SummaryEnum.ENDUSER_NAVIGATION_TIME;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");
		ListValue uri = p.table.newList("uri");
		ListValue ip = p.table.newList("ip");

		ListValue unloadEventStart = p.table.newList("unloadEventStart");
		ListValue unloadEventEnd = p.table.newList("unloadEventEnd");
		ListValue fetchStart = p.table.newList("fetchStart");
		ListValue domainLookupStart = p.table.newList("domainLookupStart");
		ListValue domainLookupEnd = p.table.newList("domainLookupEnd");
		ListValue connectStart = p.table.newList("connectStart");
		ListValue connectEnd = p.table.newList("connectEnd");
		ListValue requestStart = p.table.newList("requestStart");
		ListValue responseStart = p.table.newList("responseStart");
		ListValue responseEnd = p.table.newList("responseEnd");
		ListValue domLoading = p.table.newList("domLoading");
		ListValue domInteractive = p.table.newList("domInteractive");
		ListValue domContentLoadedEventStart = p.table.newList("domContentLoadedEventStart");
		ListValue domContentLoadedEventEnd = p.table.newList("domContentLoadedEventEnd");

		ListValue domComplete = p.table.newList("domComplete");
		ListValue loadEventStart = p.table.newList("loadEventStart");
		ListValue loadEventEnd = p.table.newList("loadEventEnd");

		Enumeration<LongKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			LongKeyLinkedMap.ENTRY<EndUserNavigationData> ent = en.nextElement();
			long key = ent.getKey();
			EndUserNavigationData data = ent.getValue();
			id.add(key);
			count.add(data.count);
			uri.add(data.uri);
			ip.add(data.ip);

			unloadEventStart.add(data.unloadEventStart);
			unloadEventEnd.add(data.unloadEventEnd);
			fetchStart.add(data.fetchStart);
			domainLookupStart.add(data.domainLookupStart);
			domainLookupEnd.add(data.domainLookupEnd);
			connectStart.add(data.connectStart);
			connectEnd.add(data.connectEnd);
			requestStart.add(data.requestStart);
			responseStart.add(data.responseStart);
			responseEnd.add(data.responseEnd);
			domLoading.add(data.domLoading);
			domInteractive.add(data.domInteractive);
			domContentLoadedEventStart.add(data.domContentLoadedEventStart);
			domContentLoadedEventEnd.add(data.domContentLoadedEventEnd);
			domComplete.add(data.domComplete);
			loadEventStart.add(data.loadEventStart);
			loadEventEnd.add(data.loadEventEnd);
		}
		return p;
	}

	public SummaryPack getAndClearAjaxTable() {

		if (navTable.size() == 0)
			return null;

		LongKeyLinkedMap<EndUserAjaxData> temp = ajaxTable;
		ajaxTable = new LongKeyLinkedMap<EndUserAjaxData>().setMax(conf.summary_enduser_ajax_max_count);

		SummaryPack p = new SummaryPack();
		p.stype = SummaryEnum.ENDUSER_AJAX_TIME;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");
		ListValue uri = p.table.newList("uri");
		ListValue ip = p.table.newList("ip");

		ListValue duration = p.table.newList("duration");
		ListValue userAgent = p.table.newList("userAgent");

		Enumeration<LongKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			LongKeyLinkedMap.ENTRY<EndUserAjaxData> ent = en.nextElement();
			long key = ent.getKey();
			EndUserAjaxData data = ent.getValue();
			id.add(key);
			count.add(data.count);
			uri.add(data.uri);
			ip.add(data.ip);

			duration.add(data.duration);
			userAgent.add(data.userAgent);

		}
		return p;
	}
	public SummaryPack getAndClearErrorTable() {

		if (navTable.size() == 0)
			return null;

		LongKeyLinkedMap<EndUserErrorData> temp = errorTable;
		errorTable = new LongKeyLinkedMap<EndUserErrorData>().setMax(conf.summary_enduser_error_max_count);

		SummaryPack p = new SummaryPack();
		p.stype = SummaryEnum.ENDUSER_SCRIPT_ERROR;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");
		ListValue stacktrace = p.table.newList("stacktrace");
		ListValue userAgent = p.table.newList("userAgent");

		ListValue uri = p.table.newList("uri");
		ListValue message = p.table.newList("message");
		ListValue file = p.table.newList("file");
		ListValue lineNumber = p.table.newList("lineNumber");
		ListValue columnNumber = p.table.newList("columnNumber");
		ListValue payloadVersion = p.table.newList("payloadVersion");

		Enumeration<LongKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			LongKeyLinkedMap.ENTRY<EndUserErrorData> ent = en.nextElement();
			long key = ent.getKey();
			EndUserErrorData data = ent.getValue();
			id.add(key);
			count.add(data.count);
			stacktrace.add(data.stacktrace);
			userAgent.add(data.userAgent);

			uri.add(data.uri);
			message.add(data.message);
			file.add(data.file);
			lineNumber.add(data.lineNumber);
			columnNumber.add(data.columnNumber);
			payloadVersion.add(data.payloadVersion);

		}
		return p;
	}

}