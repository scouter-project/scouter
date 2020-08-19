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

package reactor.core.publisher;

import scouter.agent.Logger;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2020/08/08
 */
public class ScouterOptimizableOperatorProxy {

    public static final String EMPTY = "";

    public static String nameOnCheckpoint(Object candidate) {
        try {
            if (candidate instanceof OptimizableOperator) {
                OptimizableOperator<?, ?> operator = ((OptimizableOperator<?, ?>) candidate).nextOptimizableSource();
                if (operator == null) {
                    return EMPTY;
                }
                if (operator instanceof MonoOnAssembly) {
                    FluxOnAssembly.AssemblySnapshot snapshot = ((MonoOnAssembly) operator).stacktrace;
                    if (snapshot != null && snapshot.checkpointed) {
                        return snapshot.cached;
                    }
                } else if (operator instanceof FluxOnAssembly) {
                    FluxOnAssembly.AssemblySnapshot snapshot = ((FluxOnAssembly) operator).snapshotStack;
                    if (snapshot != null && snapshot.checkpointed) {
                        return snapshot.cached;
                    }
                }
            }
            return EMPTY;
        } catch (Throwable e) {

            return EMPTY;
        }
    }

    public static void appendSources4Dump(Object candidate, StringBuilder builder) {
        try {
            if (candidate instanceof OptimizableOperator) {
                OptimizableOperator<?, ?> operator = ((OptimizableOperator<?, ?>) candidate).nextOptimizableSource();
                if (operator == null) {
                    return;
                }
                String p1 = operator.toString();
                builder.append(" (<-) ").append(p1);
                if (p1.startsWith("checkpoint")) {
                    OptimizableOperator<?, ?> operator2 = operator.nextOptimizableSource();
                    if (operator2 != null) {
                        builder.append(" (<-) ").append(operator2.toString());
                    }
                }
            }
        } catch (Exception e) {
            Logger.println("R01o2", e.getMessage(), e);
        }
    }
}
