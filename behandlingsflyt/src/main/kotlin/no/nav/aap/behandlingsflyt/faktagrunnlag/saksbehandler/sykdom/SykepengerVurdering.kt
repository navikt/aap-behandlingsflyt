package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

data class SykepengerVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val vurdertIBehandling: BehandlingId,
    val grunn: SykepengerGrunn? = null,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val gjelderFra: LocalDate,
    val gjelderTil: LocalDate? = null,
)

/**
 * Se [no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak.SYKEPENGEERSTATNING].
 */
enum class SykepengerGrunn {
    /**
     * § 11-13a: Medlemmet har tidligere mottatt arbeidsavklaringspenger og har blitt arbeidsufør som følge av en annen sykdom innen seks måneder etter at arbeidsavklaringspengene opphørte.
     */
    ANNEN_SYKDOM_INNEN_SEKS_MND,

    /**
     * § 11-13b: Medlemmet har tidligere mottatt arbeidsavklaringspenger og har blitt arbeidsufør som følge av samme sykdom innen ett år etter at arbeidsavklaringspengene opphørte.
     *
     */
    SAMME_SYKDOM_INNEN_ETT_AAR,

    /**
     * § 11-13c: Medlemmet har tidligere mottatt sykepenger etter kapittel 8 i til sammen 248, 250 eller 260 sykepengedager i løpet av de tre siste årene, se § 8-12, og har igjen blitt arbeidsufør på grunn av sykdom eller skade mens han eller hun er i arbeid.
     */
    SYKEPENGER_IGJEN_ARBEIDSUFOR,

    /**
     * § 11-13d: Medlemmet har tidligere mottatt sykepenger etter kapittel 8 i til sammen 248, 250 eller 260 sykepengedager i løpet av de tre siste årene, se § 8-12, og er fortsatt arbeidsufør på grunn av sykdom eller skade.
     */
    SYKEPENGER_FORTSATT_ARBEIDSUFOR,

    /**
     * § 11-13e: Medlemmet har mottatt arbeidsavklaringspenger og deretter foreldrepenger og har blitt arbeidsufør på grunn av sykdom eller skade, se § 8-2 andre ledd, innen seks måneder etter at foreldrepengene opphørte.
     */
    FORELDREPENGER_INNEN_SEKS_MND;
}