package scouter.org.objectweb.asm.tree;

/**
 * Exception thrown in {@link AnnotationNode#check}, {@link ClassNode#check}, {@link
 * FieldNode#check} and {@link MethodNode#check} when these nodes (or their children, recursively)
 * contain elements that were introduced in more recent versions of the ASM API than version passed
 * to these methods.
 *
 * @author Eric Bruneton
 */
public class UnsupportedClassVersionException extends RuntimeException {

  private static final long serialVersionUID = -3502347765891805831L;
}
