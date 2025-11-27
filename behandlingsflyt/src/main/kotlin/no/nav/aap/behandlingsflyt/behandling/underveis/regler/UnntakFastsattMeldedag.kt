package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/**
 * Enkelt-perioder hvor det skal være tidligere frist for å levere meldekort.
 */
object UnntakFastsattMeldedag {
    // Fiks til enklere, bare map
    private val unntak: Map<Periode, LocalDate> = buildMap {
        put(
            Periode(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 4)),
            // Ny tidligere frist
            LocalDate.of(2025, 12, 17)
        )
        put(
            Periode(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 12)),
            // Ny tidligere frist
            LocalDate.of(2026, 3, 25)
        )
    }

    /**
     * Gitt en meldeperiode, potensielt returner tidligere frist.
     */
    fun erSpesialPeriode(periode: Periode): LocalDate? {
        return unntak[periode]
    }
}

val unntakFritaksUtbetalingDato = mapOf(
    LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
    LocalDate.of(2025, 12, 25) to LocalDate.of(2025, 12, 22),
    LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
    LocalDate.of(2026, 4, 1) to LocalDate.of(2025, 3, 30),
)