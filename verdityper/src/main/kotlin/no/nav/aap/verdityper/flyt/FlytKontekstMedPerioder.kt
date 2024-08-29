package no.nav.aap.verdityper.flyt

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.util.*

/**
 * Kontekst for behandlingen som inneholder hvilke perioder som er til vurdering for det enkelte steget som skal vurderes
 * Orkestratoren beriker objektet med periodene slik at det følger av reglene for periodisering for de enkelte typene behandlingene
 */
data class FlytKontekstMedPerioder(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val behandlingType: TypeBehandling,
    val perioderTilVurdering: Set<Vurdering>
) {
    fun perioder(): NavigableSet<Periode> {
        return perioderTilVurdering.map { it.periode }.toCollection(TreeSet())
    }
}