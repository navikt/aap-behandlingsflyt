package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderYrkesskadeSteg private constructor(
    private val sykdomRepository: SykdomRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val behandlingId = kontekst.behandlingId
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        val sykdomsgrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_YRKESSKADE,
            vedtakBehøverVurdering = {
                behøverVurdering(
                    kontekst,
                    tidligereVurderinger,
                    yrkesskader
                )
            },
            erTilstrekkeligVurdert = { sykdomsgrunnlag != null && sykdomsgrunnlag.yrkesskadevurdering != null },
            tilbakestillGrunnlag = {
                val forrigeGrunnlag =
                    kontekst.forrigeBehandlingId?.let { sykdomRepository.hentHvisEksisterer(it) }?.yrkesskadevurdering
                sykdomRepository.lagre(kontekst.behandlingId, forrigeGrunnlag)
            },
            kontekst
        )

        return Fullført
    }

    private fun behøverVurdering(
        flytKontekstMedPerioder: FlytKontekstMedPerioder,
        tidligereVurderinger: TidligereVurderinger,
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?
    ): Boolean {
        return when (flytKontekstMedPerioder.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                return if (tidligereVurderinger.girIngenBehandlingsgrunnlag(flytKontekstMedPerioder, type())) {
                    false
                } else if (yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() != true) {
                    false
                } else {
                    true
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> false
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderYrkesskadeSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_YRKESSKADE
        }
    }
}
