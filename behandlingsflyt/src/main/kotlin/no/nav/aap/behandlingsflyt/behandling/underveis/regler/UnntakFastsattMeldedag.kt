package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

object UnntakFastsattMeldedag {
    private val unntak: Map<Periode, LocalDate> = buildMap {
        put(
            Periode(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 4)),
            // Ny tidligere frist
            LocalDate.of(2025, 12, 17)
        )
    }

    fun erSpesialPeriode(periode: Periode): LocalDate? {
        return unntak[periode]
    }
}