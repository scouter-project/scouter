package scouter.bytebuddy.implementation.bind;

import scouter.bytebuddy.description.method.MethodDescription;

/**
 * This {@link MethodDelegationBinder.AmbiguityResolver} selects
 * the method with more arguments. If two methods have equally many arguments, the resolution is ambiguous.
 */
public enum ParameterLengthResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        int leftLength = left.getTarget().getParameters().size();
        int rightLength = right.getTarget().getParameters().size();
        if (leftLength == rightLength) {
            return Resolution.AMBIGUOUS;
        } else if (leftLength < rightLength) {
            return Resolution.RIGHT;
        } else {
            return Resolution.LEFT;
        }
    }
}
