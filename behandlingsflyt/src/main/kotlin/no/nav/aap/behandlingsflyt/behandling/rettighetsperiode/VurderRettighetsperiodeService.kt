package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderRettighetsperiodeService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
): Informasjonskrav {
    constructor(repositoryProvider: RepositoryProvider): this(
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    override val navn = TrukketSøknadService.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return Vurderingsbehov.VURDER_RETTIGHETSPERIODE in kontekst.vurderingsbehovRelevanteForSteg
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val relevantAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)

        return if (relevantAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }


    companion object: Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.RETTIGHETSPERIODE

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): Informasjonskrav {
            return VurderRettighetsperiodeService(repositoryProvider)
        }
    }
}