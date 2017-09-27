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

public class SqlSum extends StepSummary {

	public int hash;
	public int count;
	public long elapsed;
	public long cputime;
	public int error;

	public String param;
	public String param_error;

	public byte getStepType() {
		return StepEnum.SQL_SUM;
	}

	public int getHash() {
		return hash;
	}

	public int getCount() {
		return count;
	}

	public long getElapsed() {
		return elapsed;
	}

	public long getCputime() {
		return cputime;
	}

	public int getError() {
		return error;
	}

	public String getParam() {
		return param;
	}

	public String getParamError() {
		return param_error;
	}

	public void add(int elapsed, int cputime, String param) {
		this.count++;
		this.elapsed += elapsed;
		this.cputime += cputime;
		if (this.param == null) {
			this.param = param;
		}
	}

	public void addError(int elapsed, int cputime, String param) {
		this.count++;
		this.elapsed += elapsed;
		this.cputime += cputime;
		this.error++;
		if (this.param_error == null) {
			this.param_error = param;
		}

	}

	public void write(DataOutputX out) throws IOException {
		out.writeDecimal(hash);
		out.writeDecimal(count);
		out.writeDecimal(elapsed);
		out.writeDecimal(cputime);
		out.writeDecimal(error);
		out.writeText(param);
		out.writeText(param_error);
	}

	public Step read(DataInputX in) throws IOException {
		this.hash = (int) in.readDecimal();
		this.count = (int) in.readDecimal();
		this.elapsed = in.readDecimal();
		this.cputime = in.readDecimal();
		this.error = (int) in.readDecimal();
		this.param = in.readText();
		this.param_error = in.readText();
		return this;
	}

}