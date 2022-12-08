package scouterx.weaver;
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

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/23
 */
public class ScouterTxid {

	protected static ScouterTxid EMPTY = new ScouterTxid(0);

	private long txid;
	private String stxid;

	protected ScouterTxid(long txid) {
		if (txid == 0) {
			this.txid = 0;
			this.stxid = "";
		} else {
			this.txid = txid;
			this.stxid = Hexa32.toString32(txid);
		}
	}

	protected static ScouterTxid of(Object txid) {
		if (txid instanceof Long) {
			long longTxid = (long) txid;
			if (longTxid == 0) {
				return EMPTY;
			}
			return new ScouterTxid(longTxid);
		}
		return EMPTY;
	}

	public long getTxid() {
		return txid;
	}

	public String getStxid() {
		return stxid;
	}

	public boolean isEmpty() {
		return txid == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ScouterTxid that = (ScouterTxid) o;

		if (txid != that.txid) return false;
		return stxid != null ? stxid.equals(that.stxid) : that.stxid == null;
	}

	@Override
	public int hashCode() {
		int result = (int) (txid ^ txid >>> 32);
		result = 31 * result + (stxid != null ? stxid.hashCode() : 0);
		return result;
	}
}
