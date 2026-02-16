package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class RettighetstypeService(
    private val rettighetstypeRepository: RettighetstypeRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val underveisRepository: UnderveisRepository
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        rettighetstypeRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
    )

    fun harRettInnenforPeriode(behandlingId: BehandlingId, periode: Periode): Boolean {
        val rettighetstidslinje = rettighetstypeRepository.hentHvisEksisterer(behandlingId)?.rettighetstypeTidslinje
            ?: utledRettighetstidslinjeBakoverkompatibel(behandlingId)

        return rettighetstidslinje.begrensetTil(periode).isNotEmpty()
    }

    /**
     * Denne utleder rettighetstype og kvote basert på lagret vilkårsresultat.
     * Bør bruke nedlagret rettighetstype med kvote, men dette finnes ikke for eldre behandlinger.
     * Underveis er avgrenset i fremtiden, og kan derfor ikke brukes alene,
     * men de periodene vi har i underveis bør samsvare med utledet rettighetstidslinje.
     * Perioder som ikke samsvarer kaster en feil slik at disse sakene kan håndteres manuelt
     */
    private fun utledRettighetstidslinjeBakoverkompatibel(behandlingId: BehandlingId): Tidslinje<RettighetsType> {
        val vilkårsresultat =
            vilkårsresultatRepository.hent(behandlingId)
        val vedtatteUnderveisperioder = underveisRepository.hentHvisEksisterer(behandlingId)

        val utledetRettighetstidslinje = vurderRettighetstypeOgKvoter(
            vilkårsresultat,
            KvoteService().beregn()
        ).filter { it.verdi is KvoteOk }.mapNotNull { it.rettighetsType }

        if (vedtatteUnderveisperioder != null) {
            val harVedtattePerioderSomIkkeSamsvarerMedUtlededePerioder = vedtatteUnderveisperioder.somTidslinje()
                .filter { it.verdi.utfall == Utfall.OPPFYLT }
                .map { it.rettighetsType }
                .komprimer()
                .leftJoin(utledetRettighetstidslinje) { underveisRettighetstype, rettighetstype ->
                    underveisRettighetstype != rettighetstype
                }.segmenter().any { erInkonsistent -> erInkonsistent.verdi }

            if (harVedtattePerioderSomIkkeSamsvarerMedUtlededePerioder) {
                error("Vedtatte underveisperioder samsvarer ikke med utledede rettighetstyper fra vilkårsresultat for behandling $behandlingId.")
            }
        }

        return utledetRettighetstidslinje
    }
}