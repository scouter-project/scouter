package scouter.bytebuddy.matcher;

import scouter.bytebuddy.description.method.MethodDescription;
import scouter.bytebuddy.description.method.ParameterDescription;
import scouter.bytebuddy.description.method.ParameterList;

/**
 * An element matcher that matches a method's parameters.
 *
 * @param <T> The type of the matched entity.
 */
public class MethodParametersMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the parameters.
     */
    private final ElementMatcher<? super ParameterList<?>> matcher;

    /**
     * Creates a new matcher for a method's parameters.
     *
     * @param matcher The matcher to apply to the parameters.
     */
    public MethodParametersMatcher(ElementMatcher<? super ParameterList<? extends ParameterDescription>> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.getParameters());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matcher.equals(((MethodParametersMatcher<?>) other).matcher);
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public String toString() {
        return "hasParameter(" + matcher + ")";
    }
}
