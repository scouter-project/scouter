package scouter.bytebuddy.implementation.bind.annotation;

import scouter.bytebuddy.description.annotation.AnnotationDescription;
import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.description.type.TypeDescription;
import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.bind.MethodDelegationBinder;
import scouter.bytebuddy.implementation.bytecode.StackManipulation;
import scouter.bytebuddy.implementation.bytecode.assign.Assigner;
import scouter.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import scouter.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import scouter.bytebuddy.utility.CompoundList;
import scouter.bytebuddy.implementation.MethodDelegation;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters that are annotated with this annotation will be assigned a collection (or an array) containing
 * all arguments of the source method. Currently, this annotation supports the following collection types:
 * <ul>
 * <li>Array</li>
 * </ul>
 * <p>&nbsp;</p>
 * By default, this annotation applies a
 * {@link AllArguments.Assignment#STRICT}
 * assignment of the source method's parameters to the array. This implies that parameters that are not assignable to
 * the annotated array's component type make the method with this parameter unbindable. To avoid this, you can
 * use a {@link AllArguments.Assignment#SLACK} assignment
 * which simply skips non-assignable values instead.
 *
 * @see MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AllArguments {

    /**
     * Defines the type of {@link AllArguments.Assignment}
     * type that is applied for filling the annotated array with values.
     *
     * @return The assignment handling to be applied for the annotated parameter.
     */
    Assignment value() default Assignment.STRICT;

    /**
     * Determines if the array should contain the instance that defines the intercepted value when intercepting
     * a non-static method.
     *
     * @return {@code true} if the instance on which the intercepted method should be invoked should be
     * included in the array containing the arguments.
     */
    boolean includeSelf() default false;

    /**
     * A directive for how an {@link AllArguments}
     * annotation on an array is to be interpreted.
     */
    enum Assignment {

        /**
         * A strict assignment attempts to include <b>all</b> parameter values of the source method. If only one of these
         * parameters is not assignable to the component type of the annotated array, the method is considered as
         * non-bindable.
         */
        STRICT(true),

        /**
         * Other than a {@link AllArguments.Assignment#STRICT}
         * assignment, a slack assignment simply ignores non-bindable parameters and does not include them in the target
         * array. In the most extreme case where no source method parameter is assignable to the component type
         * of the annotated array, the array that is assigned to the target parameter is empty.
         */
        SLACK(false);

        /**
         * Determines if this assignment is strict.
         */
        private final boolean strict;

        /**
         * Creates a new assignment type.
         *
         * @param strict {@code true} if this assignment is strict.
         */
        Assignment(boolean strict) {
            this.strict = strict;
        }

        /**
         * Returns {@code true} if this assignment is strict.
         *
         * @return {@code true} if this assignment is strict.
         */
        protected boolean isStrict() {
            return strict;
        }
    }

    /**
     * A binder for handling the
     * {@link AllArguments}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<AllArguments> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<AllArguments> getHandledType() {
            return AllArguments.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<AllArguments> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().isArray()) {
                throw new IllegalStateException("Expected an array type for all argument annotation on " + source);
            }
            ArrayFactory arrayFactory = ArrayFactory.forType(target.getType().getComponentType());
            boolean includeThis = !source.isStatic() && annotation.loadSilent().includeSelf();
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(source.getParameters().size() + (includeThis ? 1 : 0));
            int offset = source.isStatic() || includeThis ? 0 : 1;
            for (TypeDescription.Generic sourceParameter : includeThis
                    ? CompoundList.of(implementationTarget.getInstrumentedType().asGenericType(), source.getParameters().asTypeList())
                    : source.getParameters().asTypeList()) {
                StackManipulation stackManipulation = new StackManipulation.Compound(MethodVariableAccess.of(sourceParameter).loadFrom(offset),
                        assigner.assign(sourceParameter, arrayFactory.getComponentType(), typing));
                if (stackManipulation.isValid()) {
                    stackManipulations.add(stackManipulation);
                } else if (annotation.loadSilent().value().isStrict()) {
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
                offset += sourceParameter.getStackSize().getSize();
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(arrayFactory.withValues(stackManipulations));
        }
    }
}
