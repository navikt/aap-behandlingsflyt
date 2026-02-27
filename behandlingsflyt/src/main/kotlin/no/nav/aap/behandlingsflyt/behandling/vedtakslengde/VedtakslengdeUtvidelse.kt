package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import java.time.LocalDate

sealed class VedtakslengdeUtvidelse {
    /** Vedtakslengde skal utvides med ett helt år automatisk */
    data class Automatisk(
        val forrigeSluttdato: LocalDate,
        val nySluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    /** Vedtakslengde skal utvides under ett år (manuell behandling) */
    data class Manuell(
        val forrigeSluttdato: LocalDate,
        val nySluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    /** Ingen periode å utvide – utvidelse er ikke aktuell */
    data object IkkeAktuell : VedtakslengdeUtvidelse()
}

