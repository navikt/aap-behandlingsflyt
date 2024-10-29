package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.FORELDET
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.UNNTAK_RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_MØTE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_SENDT_INN_DOKUMENTASJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/** Vurder om medlemmet kan sanksjoneres etter ftrl § 11-9 "Reduksjon av arbeidsavklaringspenger ved
 * brudd på nærmere bestemte aktivitetsplikter". Altså en implementasjon av:
 * - [Folketrygdloven § 11-9](]https://lovdata.no/lov/1997-02-28-19/§11-9)
 * - [Forkskriftens § 4](https://lovdata.no/forskrift/2017-12-13-2100/§4)
 */
class ReduksjonAktivitetspliktRegel : UnderveisRegel {
    companion object {
        val relevanteBrudd = listOf(
            IKKE_MØTT_TIL_MØTE,
            IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
            IKKE_MØTT_TIL_TILTAK,
            IKKE_SENDT_INN_DOKUMENTASJON,
        )

        val gyldigeGrunner = listOf(
            Grunn.SYKDOM_ELLER_SKADE,
            Grunn.STERKE_VELFERDSGRUNNER,
            Grunn.RIMELIG_GRUNN,
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val høyerePrioritertVurdering = resultat.filter {
            it.verdi.aktivitetspliktVurdering != null || it.verdi.fraværFastsattAktivitetVurdering != null
        }

        val vurderinger = input.aktivitetspliktGrunnlag.tidslinje(PARAGRAF_11_9)
            .kombiner(høyerePrioritertVurdering, StandardSammenslåere.minus())
            .splittOppEtter(Period.ofDays(1))
            .map { dokumentSegment ->
                val dokument = dokumentSegment.verdi

                require(dokumentSegment.periode.antallDager() == 1) {
                    "Koden er skrevet under antagelsen om at brudd-perioden er splittet opp i enkeltdager"
                }
                require(dokument.brudd.bruddType in relevanteBrudd) {
                    "Bruddtype for paragraf 11_9 må være en av ${relevanteBrudd.joinToString(", ")}, men var ${dokument.brudd.bruddType}"
                }

                val bruddRegistretDato = LocalDate.ofInstant(dokument.metadata.opprettetTid, ZoneId.of("Europe/Oslo"))
                val bruddDato = dokumentSegment.periode.fom
                val registreringsfrist = bruddDato.plusMonths(3)

                Segment(
                    Periode(bruddDato, bruddDato),
                    if (bruddRegistretDato < registreringsfrist) ReduksjonAktivitetspliktVurdering(
                        dokument = dokument,
                        vilkårsvurdering = if (dokument.grunn in gyldigeGrunner) UNNTAK_RIMELIG_GRUNN else VILKÅR_FOR_REDUKSJON_OPPFYLT,
                    ) else ReduksjonAktivitetspliktVurdering(
                        dokument = dokument,
                        vilkårsvurdering = FORELDET,
                    )
                )
            }
            .let { Tidslinje(it) }
            .komprimer()

        return resultat.leggTilVurderinger(vurderinger, Vurdering::leggTilBruddPåNærmereBestemteAktivitetsplikter)
    }
}