package ua.wilds.wiki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.wiki.search.SettingsSearch;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Reese's Sodium Options settings search: word matching with typos and synonyms instead of loose n-grams.
 * Its index type is package-private, so the session's fields are read reflectively.
 */
@Pseudo
@Mixin(targets = "me.flashyreese.mods.reeses_sodium_options.client.search.SearchSession", remap = false)
public abstract class RsoSearchSessionMixin {
    @SuppressWarnings("rawtypes")
    @Inject(method = "results", at = @At("HEAD"), cancellable = true, remap = false)
    private void wildsWiki$results(CallbackInfoReturnable<List> cir) {
        try {
            Object self = this;
            Field fi = self.getClass().getDeclaredField("index"), fq = self.getClass().getDeclaredField("query");
            fi.setAccessible(true);
            fq.setAccessible(true);
            Object index = fi.get(self);
            Method items = index.getClass().getMethod("items"), texts = index.getClass().getMethod("normalizedTexts");
            items.setAccessible(true);
            texts.setAccessible(true);
            @SuppressWarnings("unchecked")
            List r = SettingsSearch.search((List<?>) items.invoke(index), (List<String>) texts.invoke(index), (String) fq.get(self));
            if (r != null) cir.setReturnValue(r);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // anything unexpected: the original search runs
        }
    }
}
