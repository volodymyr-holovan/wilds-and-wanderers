package ua.wilds.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Lines the game writes for nothing a player or an admin can act on, kept out of the log so real errors stand out.
 * <ul>
 * <li>"Block-attached entity at invalid position": every item frame or painting of a structure template from another
 * mod. The template keeps the frame's wall position where the structure was built; the game logs it while reading the
 * frame and then puts the frame on the right wall anyway.</li>
 * </ul>
 * Everything else, the same logger's other lines included, stays.
 * <p>
 * Under NeoForge each class loader can have its own log4j context (Minecraft's classes log through another one than
 * this mod), so the filter goes into every context there is; log4j's own RegexFilter answers every way a logger asks
 * (a hand-written filter that only implements filter(LogEvent) is never asked by the loggers). Installed at mod
 * construction and again when the server starts, for contexts made in between.
 */
final class LogNoise {
    private static final String QUIET = "Block-attached entity at invalid position.*";
    private static final Set<LoggerContext> done = Collections.newSetFromMap(new WeakHashMap<>());

    private LogNoise() {}

    static void install() {
        try {
            List<LoggerContext> contexts = new ArrayList<>();
            if (LogManager.getFactory() instanceof Log4jContextFactory f) contexts.addAll(f.getSelector().getLoggerContexts());
            contexts.add((LoggerContext) LogManager.getContext(false));
            for (LoggerContext ctx : contexts) {
                if (ctx == null || !done.add(ctx)) continue;
                Filter filter = RegexFilter.createFilter(QUIET, null, true, Filter.Result.DENY, Filter.Result.NEUTRAL);
                // FML's log4j2.xml starts the global filters with ThresholdFilter(WARN, onMatch=ACCEPT): an error is
                // accepted before a filter added after it is asked. So ours goes first: [ours, the old filters].
                var cfg = (org.apache.logging.log4j.core.config.AbstractConfiguration) ctx.getConfiguration();
                Filter old = cfg.getFilter();
                if (old != null) cfg.removeFilter(old);
                cfg.addFilter(filter);
                if (old != null) cfg.addFilter(old);
                ctx.updateLoggers();
            }
        } catch (Exception | LinkageError e) {
            WildsCore.LOG.debug("Wilds Core: log filter not installed", e);
        }
    }
}
