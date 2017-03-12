package scouter.bytebuddy.implementation.bind.annotation;

import scouter.bytebuddy.description.annotation.AnnotationDescription;
import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.bind.MethodDelegationBinder;
import scouter.bytebuddy.implementation.bytecode.assign.Assigner;
import scouter.bytebuddy.implementation.bytecode.constant.DefaultValue;
import scouter.bytebuddy.implementation.MethodDelegation;

import java.lang.annotation.*;

/**
 * Binds the parameter type's default value to the annotated parameter, i.e. {@code null} or a numeric value
 * representing zero.
 *
 * @see MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Empty {

    /**
     * A binder for the {@link Empty} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Empty> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Empty> getHandledType() {
            return Empty.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Empty> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            return new MethodDelegationBinder.ParameterBinding.Anonymous(DefaultValue.of(target.getType().asErasure()));
        }
    }
}
