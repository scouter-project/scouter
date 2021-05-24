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

import scouter.agent.Configure;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.step.DumpStep;
import scouter.lang.step.Step;
import scouter.lang.step.StepSingle;

public class ProfileCollector implements IProfileCollector {
    private Configure conf = Configure.getInstance();
    private TraceContext context;
    protected Step[] steps = new Step[conf.profile_step_max_keep_in_memory_count];
    protected int pos = 0;
    private boolean doingDumpStepJob = false;

    public int currentLevel = 0;
    public int parentLevel = -1;

    public ProfileCollector(TraceContext context) {
        this.context = context;
    }

    /**
     * (syn: pushAndSetLevel)
     * set the step's level as the current level and increase one level(to deeper level).
     * @param stepSingle
     */
    public void push(StepSingle stepSingle) {
        checkDumpStep();
        stepSingle.index = currentLevel;
        stepSingle.parent = parentLevel;
        parentLevel = currentLevel;
        currentLevel++;
    }

    /**
     * (syn: addStep)
     * assign a given step to steps[pos++]
     * @param stepSingle
     */
    protected void process(StepSingle stepSingle) {
        checkDumpStep();
        steps[pos++] = stepSingle;
        if (pos >= steps.length) {
            Step[] o = steps;
            steps = new Step[conf.profile_step_max_keep_in_memory_count];
            pos = 0;
            DataProxy.sendProfile(o, context);
        }
    }

    /**
     * (syn: setLevelAndAddStep)
     * add a step
     * @param stepSingle
     */
    public void add(StepSingle stepSingle) {
        checkDumpStep();
        stepSingle.index = currentLevel;
        stepSingle.parent = parentLevel;
        currentLevel++;
        process(stepSingle);
    }

    /**
     * (syn: pullAndAddStep)
     * add the step already leveled and decrease one level(to lower level).
     * @param stepSingle
     */
    public void pop(StepSingle stepSingle) {
        checkDumpStep();
        parentLevel = stepSingle.parent;
        process(stepSingle);
    }

    /**
     * send the Steps[] data
     * @param ok : send the data or not
     */
    public void close(boolean ok) {
        checkDumpStep();

        if (pos > 0 && ok) {
            StepSingle[] newSteps = new StepSingle[pos];
            System.arraycopy(steps, 0, newSteps, 0, pos);
            DataProxy.sendProfile(newSteps, context);
        }
    }

    private void checkDumpStep() {
        if(doingDumpStepJob) {
            return;
        }

        DumpStep dumpStep;
        doingDumpStepJob = true;
        while(true) {
            dumpStep = context.temporaryDumpSteps.poll();
            if(dumpStep == null) {
                break;
            }
            add(dumpStep);
        }
        doingDumpStepJob = false;
    }
}
