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

package scouter.lang.step;


import scouter.io.DataInputX;
import scouter.io.DataOutputX;

import java.io.IOException;


public class DumpStep extends StepSingle {

	public int[] stacks;
    public long threadId;
    public String threadName;
    public String threadState;
    public long lockOwnerId;
    public String lockName;
    public String lockOwnerName;

	public byte getStepType() {
		return StepEnum.DUMP;
	}

    public int[] getStacks() {
        return stacks;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getThreadState() {
        return threadState;
    }

    public long getLockOwnerId() {
        return lockOwnerId;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockOwnerName() {
        return lockOwnerName;
    }

    @Override
    public String toString() {
        return "DumpStep{" +
                "threadId=" + threadId +
                ", threadName='" + threadName + '\'' +
                ", threadState='" + threadState + '\'' +
                '}';
    }

    public void write(DataOutputX out) throws IOException {
		super.write(out);
        out.writeArray(stacks);
        out.writeLong(threadId);
        out.writeText(threadName);
        out.writeText(threadState);
        out.writeLong(lockOwnerId);
        out.writeText(lockName);
        out.writeText(lockOwnerName);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
        this.stacks = in.readArray(new int[0]);
        this.threadId = in.readLong();
        this.threadName = in.readText();
        this.threadState = in.readText();
        this.lockOwnerId = in.readLong();
        this.lockName = in.readText();
        this.lockOwnerName = in.readText();
		return this;
	}
}