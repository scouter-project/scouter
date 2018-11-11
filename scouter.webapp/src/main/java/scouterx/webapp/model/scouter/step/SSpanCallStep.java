/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.model.scouter.step;

import lombok.Getter;
import scouter.lang.step.SpanCallStep;
import scouter.lang.step.StepEnum;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 08/11/2018
 */
@Getter
public class SSpanCallStep extends SCommonSpanStep {
    public byte getStepType() {
        return StepEnum.SPANCALL;
    }

    public long txid;
    transient public byte opt;
    public String address;
    public byte async;

    public static SSpanCallStep of(SpanCallStep org) {
        SSpanCallStep ss = new SSpanCallStep();
        ss.setProps(org);
        ss.txid = org.txid;
        ss.address = org.address;
        ss.async = org.async;

        return ss;
    }
}
