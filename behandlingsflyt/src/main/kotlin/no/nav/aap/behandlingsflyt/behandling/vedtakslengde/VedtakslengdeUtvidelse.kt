package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import java.time.LocalDate

sealed class VedtakslengdeUtvidelse {
    data class Automatisk(
        val forrigeSluttdato: LocalDate,
        val nySluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    data class Manuell(
        val forrigeSluttdato: LocalDate,
        val nySluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    data object IngenFremtidigOrdinærRettighet : VedtakslengdeUtvidelse()
}

