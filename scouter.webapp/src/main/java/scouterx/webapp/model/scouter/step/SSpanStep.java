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
import scouter.lang.step.SpanStep;
import scouter.lang.step.StepEnum;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 08/11/2018
 */
@Getter
public class SSpanStep extends SCommonSpanStep {
    public byte getStepType() {
        return StepEnum.SPAN;
    }

    public static SSpanStep of(SpanStep org) {
        SSpanStep ss = new SSpanStep();
        ss.setProps(org);
        return ss;
    }
}
