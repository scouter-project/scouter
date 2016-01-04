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

package scouter.agent.trace;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import scouter.agent.netio.data.DataProxy;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallSum;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodSum;
import scouter.lang.step.SocketStep;
import scouter.lang.step.SocketSum;
import scouter.lang.step.SqlStep;
import scouter.lang.step.SqlSum;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.step.StepSingle;
import scouter.util.IntKeyMap;
import scouter.util.LongKeyMap;

public class ProfileSummary implements IProfileCollector {

	private TraceContext context;

	protected IntKeyMap<Step> methods;
	protected IntKeyMap<Step> sqls;
	protected IntKeyMap<Step> apicalls;
	protected LongKeyMap<Step> sockets;
	protected int magindex = 0;
	protected int totalCount;

	public ProfileSummary(TraceContext context) {
		this.context = context;
	}

	protected void process() {
		List<Step> steps = new ArrayList<Step>(totalCount);
		toArray(methods, steps);
		toArray(sqls, steps);
		toArray(apicalls, steps);
		toArray(sockets, steps);
		totalCount = 0;

		DataProxy.sendProfile(steps, context);

	}

	private void toArray(IntKeyMap<Step> src, List<Step> out) {
		if (src == null)
			return;
		Enumeration<Step> en = src.values();
		for (int i = 0, max = src.size(); i < max; i++) {
			out.add(en.nextElement());
		}
		src.clear();
	}

	private void toArray(LongKeyMap<Step> src, List<Step> out) {
		if (src == null)
			return;
		Enumeration<Step> en = src.values();
		for (int i = 0, max = src.size(); i < max; i++) {
			out.add(en.nextElement());
		}
		src.clear();
	}

	private void toArray(List<Step> src, List<Step> out) {
		if (src == null)
			return;
		for (int i = 0, max = src.size(); i < max; i++) {
			out.add(src.get(i));
		}
		src.clear();
	}

	public void push(StepSingle ss) {

	}

	public void add(StepSingle ss) {
		switch (ss.getStepType()) {
		case StepEnum.METHOD:
			add((MethodStep) ss);
			break;
		case StepEnum.SQL:
			add((SqlStep) ss);
			break;
		case StepEnum.APICALL:
			add((ApiCallStep) ss);
			break;
		case StepEnum.SOCKET:
			add((SocketStep) ss);
			break;
		}
	}

	public void pop(StepSingle ss) {
		switch (ss.getStepType()) {
		case StepEnum.METHOD:
		case StepEnum.METHOD2:
			add((MethodStep) ss);
			break;
		case StepEnum.SQL:
		case StepEnum.SQL2:
		case StepEnum.SQL3:
			add((SqlStep) ss);
			break;
		case StepEnum.APICALL:
			add((ApiCallStep) ss);
			break;
		}
	}

	protected void add(SocketStep m) {

		if (sockets == null)
			sockets = new LongKeyMap<Step>();

		long skid = m.getSocketId();
		SocketSum sksum = (SocketSum) sockets.get(skid);
		if (sksum != null) {
			sksum.add(m.elapsed, sksum.error != 0);
			return;
		}
		if (totalCount >= BUFFER_SIZE) {
			process();
		}
		sksum = new SocketSum();
		sksum.ipaddr = m.ipaddr;
		sksum.port = m.port;
		sksum.add(m.elapsed, sksum.error != 0);
		sockets.put(skid, sksum);
		totalCount++;
	}

	protected void add(MethodStep m) {
		if (methods == null)
			methods = new IntKeyMap<Step>();

		MethodSum msum = (MethodSum) methods.get(m.hash);
		if (msum != null) {
			msum.hash = m.hash;
			msum.add(m.elapsed, m.cputime);
			return;
		}
		if (totalCount >= BUFFER_SIZE) {
			process();
		}
		msum = new MethodSum();
		msum.hash = m.hash;
		msum.add(m.elapsed, m.cputime);
		methods.put(m.hash, msum);
		totalCount++;
	}

	protected void add(SqlStep ss) {
		if (sqls == null)
			sqls = new IntKeyMap<Step>();

		SqlSum ssum = (SqlSum) sqls.get(ss.hash);
		if (ssum != null) {
			ssum.hash = ss.hash;
			if (ss.error == 0) {
				ssum.add(ss.elapsed, ss.cputime, ss.param);
			} else {
				ssum.addError(ss.elapsed, ss.cputime, ss.param);
			}
			return;
		}
		if (totalCount >= BUFFER_SIZE) {
			process();
		}
		ssum = new SqlSum();
		ssum.hash = ss.hash;
		if (ss.error == 0) {
			ssum.add(ss.elapsed, ss.cputime, ss.param);
		} else {
			ssum.addError(ss.elapsed, ss.cputime, ss.param);
		}
		sqls.put(ss.hash, ssum);
		totalCount++;
	}

	protected void add(ApiCallStep sc) {
		if (apicalls == null)
			apicalls = new IntKeyMap<Step>();

		ApiCallSum scs = (ApiCallSum) apicalls.get(sc.hash);
		if (scs != null) {
			scs.hash = sc.hash;
			scs.count++;
			scs.elapsed += sc.elapsed;
			scs.cputime += sc.cputime;
			if (sc.error != 0) {
				scs.error++;
			}
			return;
		}
		if (totalCount >= BUFFER_SIZE) {
			process();
		}
		scs = new ApiCallSum();
		scs.hash = sc.hash;
		scs.count = 1;
		scs.elapsed = sc.elapsed;
		scs.cputime = sc.cputime;
		if (sc.error != 0) {
			scs.error = 1;
		}
		apicalls.put(sc.hash, scs);
		totalCount++;
	}

	public void close(boolean ok) {
		if (ok && totalCount > 0) {
			process();
		}
	}
}