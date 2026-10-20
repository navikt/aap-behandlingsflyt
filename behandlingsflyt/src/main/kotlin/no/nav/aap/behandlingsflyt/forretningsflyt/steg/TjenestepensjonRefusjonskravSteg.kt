package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class TjenestepensjonRefusjonskravSteg private constructor(
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovRepository: AvklaringsbehovOperasjonerRepository,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        tjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        tjenestePensjonRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.SAMORDNING_REFUSJONS_KRAV,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING,
                    VurderingType.MIGERING_FRA_ARENA -> {
                        val avklaringsbehovene =
                            Avklaringsbehovene(avklaringsbehovRepository, kontekst.behandlingId).alle()
                        val behandling = behandlingRepository.hent(kontekst.behandlingId)
                        val vurderingsbehov = behandling.vurderingsbehov().map { it.type }
                        val årsakTilOpprettelse = behandling.årsakTilOpprettelse

                        val bleOpprettetKunPgaInformasjonkrav =
                            avklaringsbehovene.size == 1 && avklaringsbehovene.single().definisjon == Definisjon.SAMORDNING_REFUSJONS_KRAV
                                    && årsakTilOpprettelse == ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
                                    && vurderingsbehov.size == 1 && vurderingsbehov.single() == Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON

                        when {
                            bleOpprettetKunPgaInformasjonkrav -> false
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty() -> true
                            Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON in kontekst.vurderingsbehovRelevanteForSteg -> true
                            else -> tjenestePensjonRepository.hent(kontekst.behandlingId).isNotEmpty()
                        }
                    }

                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.G_REGULERING,
                    VurderingType.OVERGANG_UFORE_STANS,
                    VurderingType.IKKE_RELEVANT -> false
                }
            },
            erTilstrekkeligVurdert = { tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(kontekst.behandlingId) != null },
            tilbakestillGrunnlag = {
                kontekst.forrigeBehandlingId
                    ?.let { tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(it) }
                    ?.also {
                        tjenestepensjonRefusjonsKravVurderingRepository.lagre(kontekst.sakId, kontekst.behandlingId, it)
                    }
            },
            kontekst = kontekst,
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return TjenestepensjonRefusjonskravSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV
        }
    }
}