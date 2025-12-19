package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.vilkårIkkeOppfylt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg(
    private val studentRepository: StudentRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gstewayProvider: GatewayProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        unleashGateway = gstewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return if (unleashGateway.isDisabled(BehandlingsflytFeature.PeriodisertSykdom)) {
            utførGammel(kontekst)
        } else {
            avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
                avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
                behandlingRepository = behandlingRepository,
                vilkårsresultatRepository = vilkårsresultatRepository,
                definisjon = Definisjon.AVKLAR_SYKDOM,
                tvingerAvklaringsbehov = kontekst.vurderingsbehovRelevanteForSteg,
                nårVurderingErRelevant = { nyKontekst -> perioderHvorSykdomsvurderingErRelevant(nyKontekst) },
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
    }

    private fun perioderHvorSykdomsvurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(
            kontekst,
            type()
        )

        val studentvurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somTidslinje(kontekst.rettighetsperiode)
            .orEmpty()

        return Tidslinje.map2(tidligereVurderingsutfall, studentvurderinger)
        { behandlingsutfall, studentvurdering ->
            when (behandlingsutfall) {
                null -> false
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> {
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

    fun utførGammel(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.AVKLAR_SYKDOM,
            vedtakBehøverVurdering = { vedtakBehøverVurderingGammel(kontekst) },
            erTilstrekkeligVurdert = { tilstrekkeligVurdertGammel(kontekst) },
            tilbakestillGrunnlag = {
                val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId
                    ?.let { sykdomRepository.hentHvisEksisterer(it) }
                    ?.sykdomsvurderinger
                    ?: emptyList()
                sykdomRepository.lagre(kontekst.behandlingId, vedtatteSykdomsvurderinger)
            },
            kontekst
        )
        return Fullført
    }

    fun vedtakBehøverVurderingGammel(kontekst: FlytKontekstMedPerioder): Boolean {

        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                /* Hvordan håndtere periodisering av studentGrunnlag? */
                val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

                tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                        studentGrunnlag.vilkårIkkeOppfylt() &&
                        kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
            }

            VurderingType.MELDEKORT -> false
            VurderingType.IKKE_RELEVANT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
        }
    }

    fun tilstrekkeligVurdertGammel(kontekst: FlytKontekstMedPerioder): Boolean {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return false

        val alleVurderingerErInnenforRettighetsperioden =
            sykdomGrunnlag.sykdomsvurderingerVurdertIBehandling(kontekst.behandlingId)
                .all { it.vurderingenGjelderFra <= kontekst.rettighetsperiode.tom }

        return sykdomGrunnlag.sykdomsvurderinger.isNotEmpty()
                && sykdomGrunnlag.somSykdomsvurderingstidslinje().helePerioden()
            .inneholder(kontekst.rettighetsperiode)
                && alleVurderingerErInnenforRettighetsperioden
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
