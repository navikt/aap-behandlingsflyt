package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class KansellerRevurderingService (
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val kansellerRevurderingRepository: KansellerRevurderingRepository
) : Informasjonskrav {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        kansellerRevurderingRepository = repositoryProvider.provide(),
    )

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return Vurderingsbehov.REVURDERING_KANSELLERT in kontekst.vurderingsbehovRelevanteForSteg
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val kansellerRevurderingAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KANSELLER_REVURDERING)

        return if (kansellerRevurderingAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }

    fun revurderingErKansellert(behandlingId: BehandlingId): Boolean {
        return kansellerRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering != null;
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.KANSELLERT_REVURDERING

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): Informasjonskrav {
            return KansellerRevurderingService(repositoryProvider)
        }
    }
}