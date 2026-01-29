package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate


/** Nye periodiserte vurderinger gjort av innlogget saksbehandler. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "behovstype", visible = true)
sealed interface PeriodisertAvklaringsbehovLøsning<LøsningPeriode: LøsningForPeriode>: AvklaringsbehovLøsning {
    /** Nye vurderinger for denne behandlingen. Disse vil legge seg "over" eksisterende vedtatte
     * vurderinger.
     *
     * Rekkefølgen på vurderingene har ingen betydning. Listen vil alltid behandles
     * som om den var sortert på [fom].
     *
     * UgyldigForespørselException hvis to vurderinger overlapper i tid.  Når overlappet sjekkes, så vil
     * implisitte [tom]-datoer anses å være dagen før neste [fom].
     **/
    val løsningerForPerioder: List<LøsningPeriode>

    /**
     * Returnerer en tidslinje med alle perioder som er løst i tidligere vurderinger. Alle perioder
     * som er definert i tidslinjen vil være regnet som løst. Dette bruke blandt annet for å validere at løsningen
     * sammen med tidligere vurderinger faktisk løser periodene som ble løftet av avklaringsbehovet.
     */
    fun hentLagredeLøstePerioder(behandlingId: BehandlingId, repositoryProvider: RepositoryProvider): Tidslinje<*>
}

/** En ny vurdering gjort av innlogget saksbehandler for en avgrenset periode. */
interface LøsningForPeriode {
    /** Fra og med. */
    val fom: LocalDate

    /** Til og med. Hvis null er slutt-datoen implisitt. I rekkefølge regnes `null` som senere enn konkrete datoer. */
    val tom: LocalDate?

    val begrunnelse: String
}