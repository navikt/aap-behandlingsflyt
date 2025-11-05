package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.util.UUID

interface VurderingDto {
    /** id som brukes frontend. */
    val id: UUID

    /** Fra og med. */
    val fom: LocalDate

    /** Til og med. Hvis null er slutt-datoen implisitt. I rekkefølge regnes `null` som senere enn konkrete datoer. */
    val tom: LocalDate?

    /** Ident til den som har gjort vurderingen. */
    val vurdertAv: VurdertAvResponse?

    /** Hvem som har kvalitetssikret vurderinge. `null` hvis ikke kvalitetssikret. */
    val kvalitetssikretAv: VurdertAvResponse?

    /** Hvem som har besluttet vurderinge. `null` hvis ikke besluttet. */
    val besluttetAv: VurdertAvResponse?
}

/** Oversikt over en pågående vurdering av et vilkår.
 *
 * Interface, og ikke klasse, for å flate ut datastrukturen og få konkret openapi-type.
 */
interface PeriodiserteVurderingerDto<T: VurderingDto> {
    /** Er `true` hvis innlogget bruker har lov til å løse avklaringsbehovet. */
    val harTilgangTilÅSaksbehandle: Boolean

    /** Vurderinger som har vært vedtatt, og som i forrige vedtak hadde en effekt.
     *
     * De fleste implisitte start- og slutt-datoer skal være satt.
     *
     * Sorter etter [vurdertTidspunkt] og så [fom].
     **/
    val sisteVedtatteVurderinger: List<T>

    /** Vurderinger som er introdusert i denne behandlingen.
     *
     * De fleste implisitte start- og slutt-datoer skal være satt.
     *
     * Sorter etter [vurdertTidspunkt] og så [fom].
     **/
    val nyeVurderinger: List<T>

    /** Perioder hvor vurderinger kan ha en effekt.
     *
     * Periodene er sortert på [fom] og overlapper ikke.
     **/
    val kanVurderes: List<Periode>

    /** Nye perioder for denne behandlingen i forhold til forrige behandling hvor
     * Kelvin må ha vurderinger for å fatte et vedtak. Hvis Kelvin mottar nye vurderinger
     * for disse periodene, så er denne verdien uendret. For behovet for vurderingene er der fortsatt
     * for å fatte vedtaket, men behovet er tilfredsstilt. Jf. OPPRETTET og AVSLUTTET avklaringsbehov.
     *
     * Periodene er sortert på [fom] og overlapper ikke.
     **/
    val behøverVurderinger: List<Periode>
}