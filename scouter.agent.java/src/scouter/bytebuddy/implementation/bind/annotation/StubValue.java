package scouter.bytebuddy.implementation.bind.annotation;

import scouter.bytebuddy.description.annotation.AnnotationDescription;
import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.description.type.TypeDescription;
import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.MethodDelegation;
import scouter.bytebuddy.implementation.bind.MethodDelegationBinder;
import scouter.bytebuddy.implementation.bytecode.StackManipulation;
import scouter.bytebuddy.implementation.bytecode.assign.Assigner;
import scouter.bytebuddy.implementation.bytecode.constant.DefaultValue;
import scouter.bytebuddy.implementation.bytecode.constant.NullConstant;

import java.lang.annotation.*;

/**
 * A stub value represents the (boxed) default value of the intercepted method's return type. This value can
 * only be assigned to a {@link java.lang.Object} parameter. This annotation is useful to conditionally return a
 * default value from a method when using an {@link java.lang.Object} return type in combination with the
 * {@link RuntimeType} annotation. The value is either representing
 * {@code null} if a method returns a reference type or {@code void} or a boxed primitive of the return type
 * representing the numeric value {@code 0}.
 *
 * @see MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface StubValue {

    /**
     * Binds the {@link StubValue} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<StubValue> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<StubValue> getHandledType() {
            return StubValue.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<StubValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().represents(Object.class)) {
                throw new IllegalStateException(target + " uses StubValue annotation on non-Object type");
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(source.getReturnType().represents(void.class)
                    ? NullConstant.INSTANCE
                    : new StackManipulation.Compound(DefaultValue.of(source.getReturnType().asErasure()),
                    assigner.assign(source.getReturnType(), TypeDescription.Generic.OBJECT, typing)));
        }
    }
}
