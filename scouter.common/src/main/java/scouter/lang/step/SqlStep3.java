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

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

import java.io.IOException;

/**
 * SqlStep version 3 (for barward compatibility)
 * the local variable 'updated' added
 * @author Gun Lee (gunlee01@gmail.com)
 * @author Eunsu Kim
 */
public class SqlStep3 extends SqlStep2 {

	/**
	 * zero & positive : affected row count
	 * -1 : return type of stmt.execute is a resultset.
     * -2 : return type of stmt.execute is a udpate count but 'getUpdateCount' have not been triggered.
     * -3 : SQL exception
	 */
	
	public static final int EXECUTE_RESULT_SET = -1;
	public static final int EXECUTE_UNKNOWN_COUNT = -2;
	
	public int updated;

	public byte getStepType() {
		return StepEnum.SQL3;
	}

	public int getUpdated() {
		return updated;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeDecimal(updated);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.updated = (int) in.readDecimal();
		return this;
	}

}