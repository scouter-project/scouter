/*
 *  Copyright 2015 LG CNS.
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

import scouter.agent.netio.data.DataProxy;
import scouter.lang.step.MethodStep;
import scouter.lang.step.ThreadSubmitStep;
import scouter.util.HashUtil;
import scouter.util.KeyGen;
import scouter.util.SysJMX;

public class TraceFutureTask {
	public static String CTX_FIELD = "_context_";

	public static TraceContext getContext() {
		TraceContext o= TraceContextManager.getLocalContext();
		return o;
	}

  private static int futureTaskHash;	
   static{
	   futureTaskHash=HashUtil.hash("FutureTask");
	   DataProxy.sendMethodName(futureTaskHash, "FutureTask");
   }
	public static Object start(Object callable, TraceContext o) {
		if (o == null){
			return null;
		}
		
		o=o.createChild();
     
		ThreadSubmitStep step = new ThreadSubmitStep();
		step.start_time = (int) (System.currentTimeMillis() - o.parent.startTime);
		if (o.parent.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getThreadCPU(o.parent.thread.getId()) - o.parent.startCpu);
		}
		// /////
		String name = Thread.currentThread().getName();
		step.hash = HashUtil.hash(name);
		DataProxy.sendApicall(step.hash, name);
		// /////
		o.txid=KeyGen.next();
		o.thread = Thread.currentThread();
		o.threadId = TraceContextManager.start(o.thread, o);
		//
    	MethodStep ms = new MethodStep();
    	ms.hash=futureTaskHash;
    	ms.start_time = (int) (System.currentTimeMillis() - o.startTime);
    	if (o.profile_thread_cputime) {
    		ms.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - o.startCpu);
    	}
    	o.profile.push(ms);
		step.txid=o.txid;
		
		return new LocalContext(o,step,ms);
	}

	public static void end(Object stat, Throwable t) {
		if (stat == null)
			return;
		try {
			LocalContext localCtx = (LocalContext) stat;
			TraceContext ctx = localCtx.context;
			MethodStep ms = (MethodStep)localCtx.option;
			ms.elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - ms.start_time;
			if (ctx.profile_thread_cputime) {
				ms.cputime = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu) - ms.start_cpu;
			}
	    	ctx.profile.pop(ms);
			ctx.profile.close(true);
			TraceContextManager.end(ctx.threadId);

			TraceContext parentCtx = ctx.parent;
			if (parentCtx == null)
				return;

			ThreadSubmitStep threadSubmitStep = (ThreadSubmitStep) localCtx.stepSingle;

			threadSubmitStep.elapsed = (int) (System.currentTimeMillis() - parentCtx.startTime) - threadSubmitStep.start_time;
			if (parentCtx.profile_thread_cputime) {
				threadSubmitStep.cputime = (int) (SysJMX.getCurrentThreadCPU() - parentCtx.startCpu) - threadSubmitStep.start_cpu;
			}

			if (t != null) {
				String msg = t.toString();
				int hash = StringHashCache.getErrHash(msg);

				if (parentCtx.error == 0) {
					parentCtx.error = hash;
				}
				threadSubmitStep.error=hash;
				DataProxy.sendError(hash, msg);
			}
		
			
	    	
			parentCtx.profile.add(threadSubmitStep);
			parentCtx.closeChild(ctx);

		} catch (Throwable x) {
			x.printStackTrace();
		}

	}

}
