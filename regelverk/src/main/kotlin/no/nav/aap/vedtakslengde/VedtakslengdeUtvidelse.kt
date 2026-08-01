package no.nav.aap.vedtakslengde

import java.time.LocalDate
import no.nav.aap.vilkårsresultat.Avslagsårsak

sealed class VedtakslengdeUtvidelse {
    data class Automatisk(
        val forrigeSluttdato: LocalDate,
        val nySluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    data class Manuell(
        val forrigeSluttdato: LocalDate,
        val avslagsårsaker: Set<Avslagsårsak> = emptySet(),
    ) : VedtakslengdeUtvidelse()

    data object IngenFremtidigBistandsbehovRettighet : VedtakslengdeUtvidelse()
}