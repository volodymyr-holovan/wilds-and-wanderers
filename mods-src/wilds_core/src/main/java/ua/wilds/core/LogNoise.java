package ua.wilds.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.List;

/**
 * Lines the game writes for nothing a player or an admin can act on, kept out of the log so real errors stand out.
 * <ul>
 * <li>"Block-attached entity at invalid position": every item frame or painting of a structure template from another
 * mod. The template keeps the frame's wall position where the structure was built; the game logs it while reading the
 * frame and then puts the frame on the right wall anyway.</li>
 * </ul>
 * Everything else, the same logger's other lines included, stays.
 */
final class LogNoise {
    private static final List<String> QUIET = List.of("Block-attached entity at invalid position");

    private LogNoise() {}

    static void install() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.getConfiguration().addFilter(new AbstractFilter(Filter.Result.DENY, Filter.Result.NEUTRAL) {
                @Override
                public Result filter(LogEvent event) {
                    String m = event.getMessage() == null ? null : event.getMessage().getFormat();
                    if (m != null) for (String q : QUIET) if (m.startsWith(q)) return Result.DENY;
                    return Result.NEUTRAL;
                }
            });
            ctx.updateLoggers();
        } catch (RuntimeException | LinkageError e) {
            WildsCore.LOG.debug("Wilds Core: log filter not installed", e);
        }
    }
}
