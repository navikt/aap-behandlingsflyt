package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere.slåSammenTilListe
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.math.min

class SamordningService(
    private val samordningVurderingRepository: SamordningVurderingRepository,
    private val samordningYtelseRepository: SamordningYtelseRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningVurderingRepository = repositoryProvider.provide(),
        samordningYtelseRepository = repositoryProvider.provide(),
    )

    fun hentVurderinger(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        return samordningVurderingRepository.hentHvisEksisterer(behandlingId)
    }

    fun hentYtelser(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        return samordningYtelseRepository.hentHvisEksisterer(behandlingId)
    }

    fun vurderingTidslinje(grunnlag: SamordningVurderingGrunnlag?): Tidslinje<List<Pair<Ytelse, SamordningVurderingPeriode>>> {
        val vurderinger =
            grunnlag?.vurderinger.orEmpty().filter { it.ytelseType.type == AvklaringsType.MANUELL }
                .map { ytelse ->
                    val segmenterForYtelse =
                        ytelse.vurderingPerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) }
                    Tidslinje(segmenterForYtelse)
                }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningVurderingPeriode>>>()) { acc, curr ->
                    acc.kombiner(curr, slåSammenTilListe())
                }

        return vurderinger
    }

    fun tidslinje(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val vurderinger = hentVurderinger(behandlingId)
        val ytelser = hentYtelser(behandlingId)
        val vurderingTidslinje = vurderingTidslinje(vurderinger)
        return vurder(ytelser, vurderingTidslinje)
    }

    fun perioderSomIkkeHarBlittVurdert(
        grunnlag: SamordningYtelseGrunnlag?,
        tidligereVurderinger: Tidslinje<List<Pair<Ytelse, SamordningVurderingPeriode>>>
    ): Tidslinje<List<Ytelse>> {
        val hentedeYtelserByManuelleYtelser =
            grunnlag?.ytelser.orEmpty().filter { it.ytelseType.type == AvklaringsType.MANUELL }
                .map { ytelse ->
                    val tidslinjePerPeriode = ytelse.ytelsePerioder.map { Tidslinje(it.periode, ytelse.ytelseType) }
                    tidslinjePerPeriode.fold(Tidslinje.empty<Ytelse>()) { acc, curr ->
                        acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
                    }.komprimer()
                }.fold(Tidslinje.empty<List<Ytelse>>()) { acc, curr ->
                    acc.kombiner(curr, slåSammenTilListe())
                }

        val perioderSomIkkeHarBlittVurdert =
            hentedeYtelserByManuelleYtelser.kombiner(tidligereVurderinger, StandardSammenslåere.minus())

        return perioderSomIkkeHarBlittVurdert
    }

    fun vurder(
        grunnlag: SamordningYtelseGrunnlag?,
        manuelleVurderinger: Tidslinje<List<Pair<Ytelse, SamordningVurderingPeriode>>>
    ): Tidslinje<SamordningGradering> {
        /**
         * Henter kun automatiske ytelser fra register - disse skal ikke ha overlappende perioder
         * Pr nå har vi ingen typer som er satt opp til å vurderes automatisk
         */
        val hentedeYtelserFraRegisterForAutomatiskVurdering =
            grunnlag?.ytelser.orEmpty().filter { it.ytelseType.type == AvklaringsType.AUTOMATISK }.map { ytelse ->
                Tidslinje(ytelse.ytelsePerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) })
            }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningYtelsePeriode>>>()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }

        // Slå sammen med vurderinger og regn ut graderinger

        val samordningTidslinje =
            hentedeYtelserFraRegisterForAutomatiskVurdering.kombiner(manuelleVurderinger, JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                // Manuelt vurderte perioder er allerede validert
                val manueltVurderteGraderinger =
                    høyre?.verdi.orEmpty().associate { it.first to it.second }
                        .mapValues { it.value.gradering!! }
                        .filterKeys { it.type == AvklaringsType.MANUELL }

                val registerVurderinger = venstre?.verdi.orEmpty().associate { it.first to it.second.gradering!! }
                    .filterKeys { it.type == AvklaringsType.AUTOMATISK }

                val alleSammen = manueltVurderteGraderinger.plus(registerVurderinger)
                val gradering =
                    min(alleSammen.values.sumOf { it.prosentverdi() }, 100)
                Segment(
                    periode, SamordningGradering(
                        gradering = Prosent(gradering),
                        ytelsesGraderinger = alleSammen.entries.map { YtelseGradering(it.key, it.value) }
                    )
                )
            })

        return samordningTidslinje
    }
}