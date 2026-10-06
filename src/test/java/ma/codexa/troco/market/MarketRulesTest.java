package ma.codexa.troco.market;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketRulesTest {

    @Test
    void cityKeyIgnoresAccentsAndCase() {
        assertThat(CityKey.of("Fès")).isEqualTo("fes");
        assertThat(CityKey.of("  CASABLANCA ")).isEqualTo("casablanca");
    }

    @Test
    void orderStatusesAcceptCallAndReturn() {
        assertThat(OrderStatuses.normalize("calling")).isEqualTo("CALLING");
        assertThat(OrderStatuses.normalize("unreachable")).isEqualTo("UNREACHABLE");
        assertThat(OrderStatuses.normalize("returned")).isEqualTo("RETURNED");
        assertThat(OrderStatuses.restoresStock("RETURNED")).isTrue();
        assertThat(OrderStatuses.restoresStock("CALLING")).isFalse();
        assertThatThrownBy(() -> OrderStatuses.normalize("SHIPPED"))
                .hasMessageContaining("inconnu");
    }
}
