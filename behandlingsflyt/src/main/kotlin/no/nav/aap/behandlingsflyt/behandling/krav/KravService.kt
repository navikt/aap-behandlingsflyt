package no.nav.aap.behandlingsflyt.behandling.krav

import no.nav.aap.behandlingsflyt.behandling.StansOpphørService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class KravService(
    private val kravRepository: KravRepository,
    private val stansOpphørService: StansOpphørService
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        kravRepository = repositoryProvider.provide(),
        stansOpphørService = StansOpphørService(repositoryProvider, gatewayProvider)
    )

    fun kravtypeTidslinje(kontekst: FlytKontekst): Tidslinje<RelevantKravType> {
        val kravTidslinje = kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.kravtidslinje().orEmpty()
        val vedtatteStansOpphør =
            kontekst.forrigeBehandlingId?.let { stansOpphørService.vedtattStansOpphør(it) } ?: emptyList()
        val gjeldendeStønadsperiodeVurderinger = emptySet<Boolean>()

        return kravTidslinje.map { krav ->
            utledKravtype(
                krav,
                erEksisterendeStønadsperiode(krav, gjeldendeStønadsperiodeVurderinger),
                vedtatteStansOpphør
            )
        }
    }

    private fun utledKravtype(
        krav: RelevantKrav,
        erEksisterendeStønadsperiode: Boolean,
        vedtattStansOpphør: List<GjeldendeStansEllerOpphør>
    ): RelevantKravType {
        if (!erEksisterendeStønadsperiode) {
            return RelevantKravType.NYTT_KRAV
        } else {
            // TODO: Send inn vurdering av hva man gjeninntrer etter i stedet for å prøve å utlede  
            val stansEllerOpphør = vedtattStansOpphør.lastOrNull { it.fom < krav.muligRettFra }
            return when (stansEllerOpphør?.vurdering) {
                null -> throw IllegalStateException("Forventet å finne stans/opphør-årsak ved inntredelse i eksisterende stønadsperiode")
                is Stans -> RelevantKravType.GJENOPPTAK_ETTER_STANS
                is Opphør -> RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR
            }
        }
    }

    private fun erEksisterendeStønadsperiode(
        krav: RelevantKrav,
        gjeldendeStønadsperiodeVurderinger: Set<Boolean>
    ): Boolean {
        // TODO: Sjekk § 12-vurdering for krav + stans/opphør-årsak
        return false
    }
}

enum class RelevantKravType {
    GJENOPPTAK_ETTER_STANS,
    GJENINNTREDEN_ETTER_OPPHØR,
    NYTT_KRAV
}