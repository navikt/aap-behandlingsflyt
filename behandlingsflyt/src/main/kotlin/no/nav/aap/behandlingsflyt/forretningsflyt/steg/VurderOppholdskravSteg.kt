package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.validerGyldigForRettighetsperiode
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderOppholdskravSteg private constructor(
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository,
    private val sakRepository: SakRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        oppholdskravGrunnlagRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.AVKLAR_OPPHOLDSKRAV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { tilstrekkeligVurdert(kontekst) },
            tilbakestillGrunnlag = {
                oppholdskravGrunnlagRepository.tilbakestillGrunnlag(
                    kontekst.behandlingId,
                    kontekst.forrigeBehandlingId
                )
            },
            kontekst
        )
        return Fullført
    }

    fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
            }
            VurderingType.MELDEKORT -> false
            VurderingType.IKKE_RELEVANT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
        }
    }

    fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Boolean {
        val grunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return false

        val sak = sakRepository.hent(kontekst.sakId)

        return grunnlag.vurderinger
            .tilTidslinje()
            .validerGyldigForRettighetsperiode(rettighetsperiode = sak.rettighetsperiode)
            .isValid
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderOppholdskravSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_OPPHOLDSKRAV
        }
    }
}
