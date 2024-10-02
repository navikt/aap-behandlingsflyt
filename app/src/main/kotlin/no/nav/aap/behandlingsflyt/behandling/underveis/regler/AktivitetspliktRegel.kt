package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Meldeperiode
import no.nav.aap.behandlingsflyt.behandling.underveis.map
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktRegel.BruddVurderingSteg0.Companion.ekspanderPerioderTilDager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktRegel.BruddVurderingSteg1.Companion.vurderMeldeperioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import java.time.LocalDate

/**
 * Vurder om medlemmet oppfyller aktivitetsplikten. Implementasjon av
 * Folketrygdloven §§ 11-7 til 11-9 og forskriften §§ 3 og 4.
 *
 * - https://lovdata.no/lov/1997-02-28-19/§11-7
 * - https://lovdata.no/lov/1997-02-28-19/§11-8
 * - https://lovdata.no/lov/1997-02-28-19/§11-9
 * - https://lovdata.no/forskrift/2017-12-13-2100/§3
 * - https://lovdata.no/forskrift/2017-12-13-2100/§4
 */
class AktivitetspliktRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        /* TODO § 11-7 */
        /* TODO: § 11-8 stans til ... vilkårene igjen er oppfylt */
        /* TODO: skriv om til å bruke tidslinje */

        /* Antagelse: ingen perioder overlapper. Hva skal vi gjøre hvis det er to forskjellige brudd
        * registrert på én dag? */
        val vurderinger = input.bruddAktivitetsplikt
            .asSequence()
            .ekspanderPerioderTilDager()
            .vurderMeldeperioder(input)
            .vurderKalenderår()
            .map { Segment(it.periode, it) }
            .toList()

        return Tidslinje(vurderinger).kombiner(
            resultat,
            JoinStyle.OUTER_JOIN { periode, bruddSegment, vurderingSegment ->
                if (bruddSegment == null) return@OUTER_JOIN vurderingSegment
                val vurdering = (vurderingSegment?.verdi ?: Vurdering())
                    .leggTilAktivitetspliktVurdering(bruddSegment.verdi)
                Segment(periode, vurdering)
            },
        )
    }

    class BruddVurderingSteg0(
        val brudd: BruddAktivitetsplikt,
        val bruddDag: LocalDate,
    ) {
        companion object {
            fun Sequence<BruddAktivitetsplikt>.ekspanderPerioderTilDager(): Sequence<BruddVurderingSteg0> {
                return this.flatMap { brudd ->
                    brudd.periode.map { dato ->
                        BruddVurderingSteg0(brudd, dato)
                    }
                }
            }
        }
    }

    class BruddVurderingSteg1(
        val brudd: BruddAktivitetsplikt,
        val bruddDag: LocalDate,
        val inntilEnDagRegel: Boolean,
        val tellerMot10DagersKvote: Boolean,
    ) {
        fun kanReduseresEtter_11_9(): Boolean {
            if (!brudd.relevantFor_11_9) {
                return false
            }

            if (brudd.gyldigGrunnFor_11_9) {
                return false
            }

            val bruddetSkjedde = brudd.periode.fom
            val bruddetRegistrert = brudd.opprettetTid.toLocalDate()
            val treMånederEtterBruddet = bruddetSkjedde.plusMonths(3)

            /* Ftrl § 11-9 andre ledd:
             * "Reduksjonen kan ikke ilegges senere enn tre måneder etter det aktuelle pliktbruddet"
             * Ikke forenklet for å matche ordlyden i loven. */
            return !(treMånederEtterBruddet < bruddetRegistrert)
        }

        companion object {
            fun Sequence<BruddVurderingSteg0>.vurderMeldeperioder(input: UnderveisInput): Sequence<BruddVurderingSteg1> {
                /* Gjør vurdering basert på meldeperioden (regelen for inntil én dag fravær i meldeperiode) */
                return this
                    .groupBy {
                        Meldeperiode.forRettighetsperiode(
                            rettighetsperiodeFom = input.rettighetsperiode.fom,
                            dato = it.bruddDag
                        )
                    }
                    .asSequence()
                    .flatMap { (_, bruddMeldeperiode) ->
                        vurderMeldeperiode(bruddMeldeperiode)
                    }
            }

            fun vurderMeldeperiode(meldeperiodeUsortert: List<BruddVurderingSteg0>): List<BruddVurderingSteg1> {
                val meldeperiode = meldeperiodeUsortert.sortedBy { it.bruddDag }
                return meldeperiode.map { enkeltbrudd ->
                    val førsteBrudd = meldeperiode.first()
                    val inntilEnDagRegel =
                        førsteBrudd.brudd.id == enkeltbrudd.brudd.id && førsteBrudd.bruddDag == enkeltbrudd.bruddDag
                    val tellerMot10DagersKvote = !inntilEnDagRegel

                    BruddVurderingSteg1(
                        brudd = enkeltbrudd.brudd,
                        bruddDag = enkeltbrudd.bruddDag,
                        inntilEnDagRegel = inntilEnDagRegel,
                        tellerMot10DagersKvote = tellerMot10DagersKvote,
                    )
                }
            }
        }
    }

    fun Sequence<BruddVurderingSteg1>.vurderKalenderår(): Sequence<AktivitetspliktVurdering> {
        /* Gjør vurdering basert på kalenderår (regelen for inntil 10 dager fravær i kalenderår) */
        return this.groupBy { it.brudd.periode.fom.year }
            .asSequence()
            .flatMap { (_, bruddKalenderår) ->
                vurderKalenderår(bruddKalenderår)
            }
    }

    fun vurderKalenderår(kalenderårIkkeSortert: List<BruddVurderingSteg1>): List<AktivitetspliktVurdering> {
        val kalenderår = kalenderårIkkeSortert.sortedBy { it.brudd.periode.fom }
        var kvote = 0

        return kalenderår.map { steg1 ->
            if (steg1.tellerMot10DagersKvote) {
                kvote += 1
            }

            val posisjonKalenderår = if (steg1.tellerMot10DagersKvote) kvote else null

            val stansPåGrunnAvKvote = posisjonKalenderår != null && posisjonKalenderår > 10
            val stansPåGrunnAvEnDagsRegel = (!steg1.brudd.gyldigGrunnFor_11_8 && !steg1.inntilEnDagRegel)
            val kanStanses_11_8 = steg1.brudd.relevantFor_11_8 && (stansPåGrunnAvKvote || stansPåGrunnAvEnDagsRegel)

            return@map AktivitetspliktVurdering(
                periode = Periode(steg1.bruddDag, steg1.bruddDag),
                muligeSanksjoner = listOfNotNull(
                    if (kanStanses_11_8) BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8 else null,
                    if (steg1.kanReduseresEtter_11_9()) BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9 else null,
                ),
                saksbehandlersØnsketSanksjon = steg1.brudd.paragraf,
            )
        }
    }
}