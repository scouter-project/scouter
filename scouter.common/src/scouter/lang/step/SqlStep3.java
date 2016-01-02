/*
 *  Copyright 2015 Scouter Project.
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

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class SqlStep3 extends SqlStep2 {

	public int updated;

	public byte getStepType() {
		return StepEnum.SQL3;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeInt(updated);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.updated = in.readInt();
		return this;
	}

}