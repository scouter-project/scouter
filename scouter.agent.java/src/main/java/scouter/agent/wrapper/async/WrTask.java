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
package scouter.agent.wrapper.async;

import scouter.agent.trace.TraceMain;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 7. 30.
 */
public class WrTask implements Runnable {
    private Runnable inner;
    public WrTask(Runnable runnable) {
        this.inner = runnable;
    }

    @Override
    public void run() {
        Object localContext = TraceMain.callRunnableCallInvoked(inner);
        Throwable thrown = null;
        try {
            inner.run();
        } catch (RuntimeException x) {
            thrown = x; throw x;
        } catch (Error x) {
            thrown = x; throw x;
        } catch (Throwable x) {
            thrown = x; throw new Error(x);
        } finally {
            TraceMain.callRunnableCallEnd(null, localContext, thrown);
        }
    }
}
