package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SykdomsvurderingBrevSteg internal constructor(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tidligereVurderinger: TidligereVurderinger
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.SKRIV_SYKDOMSVURDERING_BREV,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                            return@oppdaterAvklaringsbehov false
                        }

                        val behandling = behandlingRepository.hent(kontekst.behandlingId)
                        val sykdomsSteggruppedefinisjoner =
                            Definisjon.entries.filter {
                                behandling.flyt()
                                    .erStegFør(it.løsesISteg, Definisjon.SKRIV_SYKDOMSVURDERING_BREV.løsesISteg)
                            }

                        val behov = avklaringsbehovene.hentBehovForDefinisjon(sykdomsSteggruppedefinisjoner)

                        val finnesAvsluttede =
                            behov.any { it.status().erAvsluttet() && it.status() != Status.AVBRUTT }
                        finnesAvsluttede
                    }

                    else -> false
                }
            },
            erTilstrekkeligVurdert = {
                sykdomsvurderingForBrevRepository.hent(kontekst.behandlingId)?.vurdering?.isNotBlank() == true
            },
            tilbakestillGrunnlag = {
                sykdomsvurderingForBrevRepository.deaktiverEksisterende(kontekst.behandlingId)
            },
            kontekst = kontekst
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SykdomsvurderingBrevSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SYKDOMSVURDERING_BREV
        }
    }
}