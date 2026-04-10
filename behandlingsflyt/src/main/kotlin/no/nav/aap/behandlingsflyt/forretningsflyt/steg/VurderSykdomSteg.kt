package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.OVERGANG_UFORE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg(
    private val sykdomRepository: SykdomRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sykdomRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov(kontekst),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            nårVurderingErGyldig = { tilstrekkeligVurdert(kontekst) },
            kontekst,
            tilbakestillGrunnlag = {
                val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId
                    ?.let { sykdomRepository.hentHvisEksisterer(it) }
                    ?.sykdomsvurderinger
                    ?: emptyList()
                sykdomRepository.lagre(kontekst.behandlingId, vedtatteSykdomsvurderinger)
            },
        )
        return Fullført
    }

    private fun tvingerAvklaringsbehov(kontekst: FlytKontekstMedPerioder): Set<Vurderingsbehov> {
        val forrigeOvergangArbeidGrunnlag = kontekst.forrigeBehandlingId?.let {
            overgangArbeidRepository.hentHvisEksisterer(it)
        }
        val standardVurderingsbehov = kontekst.vurderingsbehovRelevanteForSteg

        if (forrigeOvergangArbeidGrunnlag?.vurderinger.isNullOrEmpty()) {
            return standardVurderingsbehov
        }

        return standardVurderingsbehov - Vurderingsbehov.OVERGANG_ARBEID
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(
            kontekst,
            type()
        )

        return tidligereVurderingsutfall.mapValue { behandlingsutfall ->
            when (behandlingsutfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    behandlingsutfall.rettighetstype == null
                }
            }
        }
    }

    fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)

        return sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
            .mapValue { it.vurderingenGjelderFra <= kontekst.rettighetsperiode.tom }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykdomSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
