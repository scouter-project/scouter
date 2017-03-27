package scouter.bytebuddy.implementation.bind.annotation;

import scouter.bytebuddy.description.annotation.AnnotationDescription;
import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.description.type.TypeDescription;
import scouter.bytebuddy.implementation.Implementation;
import scouter.bytebuddy.implementation.bind.ArgumentTypeResolver;
import scouter.bytebuddy.implementation.bind.MethodDelegationBinder;
import scouter.bytebuddy.implementation.bytecode.StackManipulation;
import scouter.bytebuddy.implementation.bytecode.assign.Assigner;
import scouter.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import scouter.bytebuddy.implementation.MethodDelegation;

import java.lang.annotation.*;

/**
 * Parameters that are annotated with this annotation will be assigned the value of the parameter of the source method
 * with the given parameter. For example, if source method {@code foo(String, Integer)} is bound to target method
 * {@code bar(@Argument(1) Integer)}, the second parameter of {@code foo} will be bound to the first argument of
 * {@code bar}.
 * <p>&nbsp;</p>
 * If a source method has less parameters than specified by {@link Argument#value()}, the method carrying this parameter
 * annotation is excluded from the list of possible binding candidates to this particular source method. The same happens,
 * if the source method parameter at the specified index is not assignable to the annotated parameter.
 *
 * @see MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 * @see RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument {

    /**
     * The index of the parameter of the source method that should be bound to this parameter.
     *
     * @return The required parameter index.
     */
    int value();

    /**
     * Determines if the argument binding is to be considered by a
     * {@link ArgumentTypeResolver}
     * for resolving ambiguous bindings of two methods. If
     * {@link Argument.BindingMechanic#UNIQUE},
     * of two bindable target methods such as for example {@code foo(String)} and {@code bar(Object)}, the {@code foo}
     * method would be considered as dominant over the {@code bar} method because of its more specific argument type. As
     * a side effect, only one parameter of any target method can be bound to a source method parameter with a given
     * index unless the {@link Argument.BindingMechanic#ANONYMOUS}
     * option is used for any other binding.
     *
     * @return The binding type that should be applied to this parameter binding.
     * @see ArgumentTypeResolver
     */
    BindingMechanic bindingMechanic() default BindingMechanic.UNIQUE;

    /**
     * Determines if a parameter binding should be considered for resolving ambiguous method bindings.
     *
     * @see Argument#bindingMechanic()
     * @see ArgumentTypeResolver
     */
    enum BindingMechanic {

        /**
         * The binding is unique, i.e. only one such binding must be present among all parameters of a method. As a
         * consequence, the binding can be latter identified by an
         * {@link MethodDelegationBinder.AmbiguityResolver}.
         */
        UNIQUE {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                             TypeDescription.Generic target,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return MethodDelegationBinder.ParameterBinding.Unique.of(
                        new StackManipulation.Compound(
                                MethodVariableAccess.of(source).loadFrom(parameterOffset),
                                assigner.assign(source, target, typing)),
                        new ArgumentTypeResolver.ParameterIndexToken(sourceParameterIndex)
                );
            }
        },

        /**
         * The binding is anonymous, i.e. it can be present on several parameters of the same method.
         */
        ANONYMOUS {
            @Override
            protected MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                             TypeDescription.Generic target,
                                                                             int sourceParameterIndex,
                                                                             Assigner assigner,
                                                                             Assigner.Typing typing,
                                                                             int parameterOffset) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(
                        new StackManipulation.Compound(MethodVariableAccess.of(source).loadFrom(parameterOffset), assigner.assign(source, target, typing))
                );
            }
        };

        /**
         * Creates a binding that corresponds to this binding mechanic.
         *
         * @param source               The source type to be bound.
         * @param target               The target type the {@code sourceType} is to be bound to.
         * @param sourceParameterIndex The index of the source parameter.
         * @param assigner             The assigner that is used to perform the assignment.
         * @param typing               Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param parameterOffset      The offset of the source method's parameter.
         * @return A binding considering the chosen binding mechanic.
         */
        protected abstract MethodDelegationBinder.ParameterBinding<?> makeBinding(TypeDescription.Generic source,
                                                                                  TypeDescription.Generic target,
                                                                                  int sourceParameterIndex,
                                                                                  Assigner assigner,
                                                                                  Assigner.Typing typing,
                                                                                  int parameterOffset);
    }

    /**
     * A binder for handling the
     * {@link Argument}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Argument> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            Argument argument = annotation.loadSilent();
            if (argument.value() < 0) {
                throw new IllegalArgumentException("@Argument annotation on " + target + " specifies negative index");
            } else if (source.getParameters().size() <= argument.value()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return argument.bindingMechanic().makeBinding(source.getParameters().get(argument.value()).getType(),
                    target.getType(),
                    argument.value(),
                    assigner,
                    typing,
                    source.getParameters().get(argument.value()).getOffset());
        }
    }
}
