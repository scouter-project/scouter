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
import scouter.agent.util.Tuple;

import java.lang.reflect.Method;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2020/08/08
 */
public class ScouterOptimizableOperatorProxy {

    public static final String EMPTY = "";
    public static final Tuple.StringLongPair EMPTYOBJ = new Tuple.StringLongPair("", 0);
    public static boolean accessible = false;
    public static boolean first = true;

    public static Tuple.StringLongPair nameOnCheckpoint(Object candidate, int maxScanDepth, boolean isReactor34,
                                                        Method isCheckpoint) {
        try {
            if (!accessible && first) {
                try {
                    Class checker = Class.forName("reactor.core.publisher.OptimizableOperator");
                    accessible = true;
                } catch (ClassNotFoundException e) {
                    accessible = false;
                    Logger.println("reactor.core.publisher.OptimizableOperator not accessible. reactor checkpoint processing will be disabled.");
                }
                first = false;
            }
            if (!accessible) {
                return EMPTYOBJ;
            }

            if (candidate instanceof OptimizableOperator) {
                OptimizableOperator<?, ?> closeAssembly = findCloseAssembly((OptimizableOperator<?, ?>) candidate, maxScanDepth);
                if (closeAssembly == null) {
                    return EMPTYOBJ;
                }
                if (closeAssembly instanceof MonoOnAssembly) {
                    FluxOnAssembly.AssemblySnapshot snapshot = ((MonoOnAssembly) closeAssembly).stacktrace;
                    boolean cp = isReactor34 ? (Boolean) isCheckpoint.invoke(snapshot) : snapshot.checkpointed;
                    if (snapshot != null && cp) {
                        return new Tuple.StringLongPair(snapshot.cached, snapshot.hashCode());
                    }
                } else if (closeAssembly instanceof FluxOnAssembly) {
                    FluxOnAssembly.AssemblySnapshot snapshot = ((FluxOnAssembly) closeAssembly).snapshotStack;
                    boolean cp = isReactor34 ? (Boolean) isCheckpoint.invoke(snapshot) : snapshot.checkpointed;
                    if (snapshot != null && cp) {
                        return new Tuple.StringLongPair(snapshot.cached, snapshot.hashCode());
                    }
                }
            }
            return EMPTYOBJ;
        } catch (Throwable e) {

            return EMPTYOBJ;
        }
    }

    public static OptimizableOperator<?, ?> findCloseAssembly(OptimizableOperator<?, ?> candidate, int maxScanDepth) {
        OptimizableOperator<?, ?> operator = candidate;
        for (int i = 0; i < maxScanDepth; i++) {
            operator = operator.nextOptimizableSource();
            if (operator == null) {
                return null;
            } else if (operator instanceof MonoOnAssembly || operator instanceof FluxOnAssembly) {
                return operator;
            }
        }
        return null;
    }

    public static void appendSources4Dump(Object candidate, StringBuilder builder, int maxScanDepth) {
        try {
            if (!accessible && first) {
                try {
                    Class checker = Class.forName("reactor.core.publisher.OptimizableOperator");
                    accessible = true;
                } catch (ClassNotFoundException e) {
                    accessible = false;
                    Logger.println("reactor.core.publisher.OptimizableOperator not accessible. reactor checkpoint processing will be disabled.");
                }
                first = false;
            }
            if (!accessible) {
                return;
            }

            if (candidate instanceof OptimizableOperator) {
                OptimizableOperator<?, ?> closeAssembly = findCloseAssembly((OptimizableOperator<?, ?>) candidate, maxScanDepth);
                if (closeAssembly == null) {
                    return;
                }
                String p1 = closeAssembly.toString();
                builder.append(" (<-) ").append(p1);
                if (p1.startsWith("checkpoint")) {
                    OptimizableOperator<?, ?> operator2 = closeAssembly.nextOptimizableSource();
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
