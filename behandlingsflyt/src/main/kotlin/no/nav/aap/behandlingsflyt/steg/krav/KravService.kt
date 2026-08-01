package no.nav.aap.behandlingsflyt.steg.krav

import no.nav.aap.behandlingsflyt.steg.stansopphør.StansOpphørService
import no.nav.aap.behandlingsflyt.steg.rettighetstype.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.krav.RelevantKrav
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.stansopphør.Opphør
import no.nav.aap.stansopphør.Stans

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