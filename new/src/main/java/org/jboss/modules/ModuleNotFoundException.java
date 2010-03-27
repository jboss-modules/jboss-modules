package org.jboss.modules;

/**
 * ModuleNotFoundException -
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class ModuleNotFoundException extends Exception {
    private static final long serialVersionUID = -1225396191255481860L;

    /**
     * Constructs a {@code ModuleNotFoundException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public ModuleNotFoundException() {
    }

    /**
     * Constructs a {@code ModuleNotFoundException} with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public ModuleNotFoundException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code ModuleNotFoundException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ModuleNotFoundException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code ModuleNotFoundException} with the specified detail message and cause.
     *
     * @param msg   the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ModuleNotFoundException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
