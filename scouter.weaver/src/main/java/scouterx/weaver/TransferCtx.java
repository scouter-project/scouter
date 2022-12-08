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
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/22
 */
public class TransferCtx {

	protected static TransferCtx EMPTY = new TransferCtx(null, ScouterTxid.EMPTY);

	protected Object ctx; //Scouter's TraceContext or LocalContext
	protected ScouterTxid stxid;

	public TransferCtx(Object o, ScouterTxid stxid) {
		this.ctx = o;
		this.stxid = stxid;
	}

	public ScouterTxid getScouterTxid() {
		if (stxid == null) {
			return ScouterTxid.EMPTY;
		}
		return stxid;
	}

	public boolean isEmpty() {
		return ctx == null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TransferCtx that = (TransferCtx) o;

		return stxid != null ? stxid.equals(that.stxid) : that.stxid == null;
	}

	@Override
	public int hashCode() {
		int result = ctx != null ? ctx.hashCode() : 0;
		result = 31 * result + (stxid != null ? stxid.hashCode() : 0);
		return result;
	}
}
