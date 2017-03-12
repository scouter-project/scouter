package scouter.bytebuddy.implementation.bind.annotation;

import scouter.bytebuddy.description.annotation.AnnotationDescription;
import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.bind.MethodDelegationBinder;
import scouter.bytebuddy.implementation.bytecode.StackManipulation;
import scouter.bytebuddy.implementation.bytecode.assign.Assigner;
import scouter.bytebuddy.implementation.bytecode.constant.NullConstant;
import scouter.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import scouter.bytebuddy.implementation.MethodDelegation;

import java.lang.annotation.*;

/**
 * Parameters that are annotated with this annotation will be assigned a reference to the instrumented object, if
 * the instrumented method is not static. Otherwise, the method with this parameter annotation will be excluded from
 * the list of possible binding candidates of the static source method.
 *
 * @see MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface This {

    /**
     * Determines if the annotated parameter should be bound to {@code null} when intercepting a {@code static} method.
     *
     * @return {@code true} if the annotated parameter should be bound to {@code null} as a fallback.
     */
    boolean optional() default false;

    /**
     * A binder for handling the
     * {@link This}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<This> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<This> getHandledType() {
            return This.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<This> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (target.getType().isPrimitive()) {
                throw new IllegalStateException(target + " uses a primitive type with a @This annotation");
            } else if (target.getType().isArray()) {
                throw new IllegalStateException(target + " uses an array type with a @This annotation");
            } else if (source.isStatic() && !annotation.loadSilent().optional()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(source.isStatic()
                    ? NullConstant.INSTANCE
                    : new StackManipulation.Compound(MethodVariableAccess.loadThis(),
                    assigner.assign(implementationTarget.getInstrumentedType().asGenericType(), target.getType(), typing)));
        }
    }
}

