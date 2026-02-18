package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg(
    private val studentRepository: StudentRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            tvingerAvklaringsbehov = kontekst.vurderingsbehovRelevanteForSteg,
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

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(
            kontekst,
            type()
        )

        val studentvurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somStudenttidslinje(kontekst.rettighetsperiode.tom)
            .orEmpty()

        return Tidslinje.map2(tidligereVurderingsutfall, studentvurderinger)
        { behandlingsutfall, studentvurdering ->
            when (behandlingsutfall) {
                null -> false
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt, TidligereVurderinger.Ukjent -> {
                    studentvurdering?.erOppfylt() != true
                }
            }
        }
    }

    fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)

        return sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
            .mapValue { it.vurderingenGjelderFra <= kontekst.rettighetsperiode.tom }
    }

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
