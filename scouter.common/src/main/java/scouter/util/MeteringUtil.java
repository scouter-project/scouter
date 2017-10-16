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

package scouter.util;

public abstract class MeteringUtil<T> {

	
	protected final int BUCKET_SIZE;
	protected final int TIME_UNIT;
	
	public MeteringUtil() {
		this(1000,301);
	}
	public MeteringUtil(int bucketSize) {
		this(1000,bucketSize);
	}
	public MeteringUtil(int timeUnit, int bucketSize) {
		this.TIME_UNIT=timeUnit;
		this.BUCKET_SIZE = bucketSize;
		this._time_ = getTime();
		this._pos_ = (int) (_time_ % BUCKET_SIZE);
		this.table = new Object[bucketSize];
		for (int i = 0; i < bucketSize; i++) {
			this.table[i] = create();
		}
	}

	private final Object[] table;
	private long _time_;
	private int _pos_;

	abstract protected T create();
	abstract protected void clear(T o);

	public synchronized T getCurrentBucket() {
		int pos = getPosition();
		return (T)table[pos];
	}

	public synchronized int getPosition() {
		long curTime = getTime();
		if (curTime != _time_) {
			for (int i = 0; i < (curTime - _time_) && i < BUCKET_SIZE; i++) {
				_pos_ = (_pos_ + 1 > BUCKET_SIZE - 1) ? 0 : _pos_ + 1;
				clear((T)table[_pos_]);
			}
			_time_ = curTime;
			_pos_ = (int) (_time_ % BUCKET_SIZE);
		}
		return _pos_;
	}

	protected int check(int period) {
		if (period >= BUCKET_SIZE)
			period = BUCKET_SIZE - 1;
		return period;
	}

	protected int stepback(int pos) {
		if (pos == 0)
			pos = BUCKET_SIZE - 1;
		else
			pos--;
		return pos;
	}

	
	public static interface Handler<T> {
		public void process(T u);
	}

	public int search(int period, Handler<T> h) {
		period = check(period);
		int pos = getPosition();

		for (int i = 0; i < period; i++, pos = stepback(pos)) {
			h.process((T)table[pos]);
		}
		return period;
	}
	public T[] search(int period) {
		period = check(period);
		int pos = getPosition();

		T[] out = (T[]) new Object[period];
		for (int i = 0; i < period; i++, pos = stepback(pos)) {
			out[i]=((T)table[pos]);
		}
		return out;
	}

	protected long getTime() {
		return System.currentTimeMillis() / TIME_UNIT;
	}

}