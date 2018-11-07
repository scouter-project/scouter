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

package scouter.lang;

import scouter.lang.pack.SpanTypes;
import scouter.lang.step.StepSingle;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 02/11/2018
 */
public class SpanStepLink {
    private StepSingle step;
    private StepSingle next;
    private StepSingle child;
    private SpanTypes.Type spanType;

    public StepSingle getStep() {
        return step;
    }

    public void setStep(StepSingle step) {
        this.step = step;
    }

    public StepSingle getNext() {
        return next;
    }

    public void setNext(StepSingle next) {
        this.next = next;
    }

    public StepSingle getChild() {
        return child;
    }

    public void setChild(StepSingle child) {
        this.child = child;
    }

    public SpanTypes.Type getSpanType() {

        return spanType;
    }

    public void setSpanType(SpanTypes.Type spanType) {
        this.spanType = spanType;
    }
}
