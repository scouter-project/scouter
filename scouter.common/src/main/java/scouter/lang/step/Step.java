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
import java.util.ArrayList;
import java.util.List;

abstract public class Step implements Comparable<Step> {

	@Override
	public int compareTo(Step o) {
		return this.getOrder() - o.getOrder();
	}

	abstract public int getOrder();

	abstract public byte getStepType();

	/**
	 * for web app client (json parsing)
	 */
	public String getStepTypeName() {
		return StepEnum.Type.of(getStepType()).name();
	}

	abstract public void write(DataOutputX out) throws IOException;

	abstract public Step read(DataInputX in) throws IOException;

	public static byte[] toBytes(Step[] p) {
		if (p == null)
			return null;
		try {
			DataOutputX dout = new DataOutputX(p.length * 30);
			for (int i = 0; i < p.length; i++) {
				if (p[i] instanceof ThreadCallPossibleStep) {
					ThreadCallPossibleStep tstep = (ThreadCallPossibleStep) p[i];
					if (tstep.threaded != 1 && tstep.isIgnoreIfNoThreaded) {
						//skip
					} else {
						dout.writeStep(p[i]);
					}
				} else {
					dout.writeStep(p[i]);
				}
			}
			return dout.toByteArray();
		} catch (IOException e) {
		}
		return null;
	}

	public static byte[] toBytes(List<Step> p) {
		if (p == null)
			return null;
		try {
			int size = p.size();
			DataOutputX dout = new DataOutputX(size * 30);
			for (int i = 0; i < size; i++) {
				dout.writeStep(p.get(i));
			}
			return dout.toByteArray();
		} catch (IOException e) {
		}
		return null;
	}

	public static Step[] toObjects(byte[] buff) throws IOException {
		if (buff == null)
			return null;
		ArrayList<Step> arr = new ArrayList<Step>();
		DataInputX din = new DataInputX(buff);
		while (din.available() > 0) {
			arr.add(din.readStep());
		}
		return (Step[]) arr.toArray(new Step[arr.size()]);
	}

	public static List<Step> toObjectList(byte[] buff) {
		if (buff == null)
			return null;
		ArrayList<Step> arr = new ArrayList<Step>();
		DataInputX din = new DataInputX(buff);
		try {
			while (din.available() > 0) {
                arr.add(din.readStep());
            }
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return arr;
	}

	public static List<Step> toDecodedObjectList(byte[] buff) {
		if (buff == null)
			return null;
		ArrayList<Step> arr = new ArrayList<Step>();
		DataInputX din = new DataInputX(buff);
		try {
			while (din.available() > 0) {
				arr.add(din.readStep());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return arr;
	}
}