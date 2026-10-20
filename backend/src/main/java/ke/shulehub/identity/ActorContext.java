package ke.shulehub.identity;

import java.util.Optional;
import java.util.function.Supplier;

import ke.shulehub.shared.web.ForbiddenException;

/** Holds the {@link Actor} for the current request thread. */
public final class ActorContext {

    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

    private ActorContext() {
    }

    public static Optional<Actor> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Actor require() {
        return current().orElseThrow(() -> new ForbiddenException("Sign in to do this"));
    }

    /** The caller's person record; platform admins without a school account are refused. */
    public static PersonRef requirePerson() {
        PersonRef person = require().person();
        if (person == null) {
            throw new ForbiddenException("Your login is not linked to a person in this school");
        }
        return person;
    }

    public static void set(Actor actor) {
        CURRENT.set(actor);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Runs {@code action} as {@code actor}; used by tests and background jobs. */
    public static <T> T callAs(Actor actor, Supplier<T> action) {
        Actor previous = CURRENT.get();
        CURRENT.set(actor);
        try {
            return action.get();
        }
        finally {
            if (previous == null) {
                CURRENT.remove();
            }
            else {
                CURRENT.set(previous);
            }
        }
    }
}
