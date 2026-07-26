package ma.codexa.troco.unit;

import ma.codexa.troco.util.PathTargetMatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathTargetMatcherTest {

    @Test
    void emptyTargetsMatchAll() {
        assertThat(PathTargetMatcher.matches(null, "/boutique")).isTrue();
        assertThat(PathTargetMatcher.matches("", "/")).isTrue();
        assertThat(PathTargetMatcher.matches("  ", "/page/x")).isTrue();
    }

    @Test
    void matchesExactAndPrefix() {
        assertThat(PathTargetMatcher.matches("/boutique\n/contact", "/boutique")).isTrue();
        assertThat(PathTargetMatcher.matches("/boutique", "/boutique?x=1")).isTrue();
        assertThat(PathTargetMatcher.matches("/page/promo", "/contact")).isFalse();
    }

    @Test
    void matchesWildcardSuffix() {
        assertThat(PathTargetMatcher.matches("/blog/*", "/blog/mon-article")).isTrue();
        assertThat(PathTargetMatcher.matches("/blog/*", "/boutique")).isFalse();
    }
}
