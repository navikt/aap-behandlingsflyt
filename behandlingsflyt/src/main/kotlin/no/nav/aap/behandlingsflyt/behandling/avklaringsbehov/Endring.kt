package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class Endring(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent? = null,
    val frist: LocalDate? = null,
    val endretAv: String,
    val årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    /**
     * Perioder som vurderingene minst må dekke
     */
    val perioderVedtaketBehøverVurdering: Set<Periode>? = null,
    /**
     * Perioder som mangler gyldige vurderinger; 
     * Enten fordi det ikke finnes en vurdering, 
     * eller fordi vurderingen av andre årsaker ikke er god nok
     */
    val perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>? = null,
) : Comparable<Endring> {

    override fun compareTo(other: Endring): Int {
        return tidsstempel.compareTo(other.tidsstempel)
    }
}
