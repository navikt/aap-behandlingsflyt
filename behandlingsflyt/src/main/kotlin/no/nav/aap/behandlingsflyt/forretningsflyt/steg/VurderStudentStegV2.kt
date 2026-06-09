package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.student.StudentFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.student.StudentVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somSykdomsvurderingTidslinje
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderStudentStegV2 private constructor(
    private val studentRepository: StudentRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        studentRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = Definisjon.AVKLAR_STUDENT_V2,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.REVURDER_STUDENT), // TODO funker dette?
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { studentRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                studentRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
            },
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert(kontekst) },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                vurderStudentvilkår(kontekst)
            }

            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført

    }

    private fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, OvergangUføreSteg.type())
        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()

        return Tidslinje.map2(
            utfall,
            sykdomsvurderinger
        ) { _, utfall, sykdomsvurdering ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> sykdomsvurdering?.potensieltOppfyltStudent() == true && unleashGateway.isEnabled(
                    BehandlingsflytFeature.StudentV2
                )

                else -> false
            }
        }
    }

    /**
     * Det må finnes en vurdering for alle perioder der sykdom er vurdert til potensielt oppfylt student,
     * og det kan ikke finnes en vurdering som er oppfylt for perioder der sykdomsvurdering ikke er potensielt oppfylt student
     */
    private fun perioderSomIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val studentTidslinje =
            studentRepository.hentHvisEksisterer(kontekst.behandlingId)?.somStudenttidslinje() ?: tidslinjeOf()
        val sykdomTidslinje =
            sykdomRepository.hent(kontekst.behandlingId).sykdomsvurderinger.somSykdomsvurderingTidslinje()

        val nårPåkrevdVurderingMangler =
            nårVurderingErRelevant(kontekst).leftJoin(studentTidslinje) { erRelevant, studentVurdering ->
                erRelevant && studentVurdering == null
            }

        val nårVurderingErKonsistentMedSykdom = nårVurderingErKonsistentMedSykdom(
            studentTidslinje,
            sykdomTidslinje
        )

        return Tidslinje.map2(
            nårPåkrevdVurderingMangler,
            nårVurderingErKonsistentMedSykdom
        ) { vurderingMangler, erKonsistent ->
            vurderingMangler == true || erKonsistent == false
        }.komprimer().filter { erUtilstrekkelig -> erUtilstrekkelig.verdi }.perioder().toSet()

    }

    private fun nårVurderingErKonsistentMedSykdom(
        studentTidslinje: Tidslinje<StudentVurdering>,
        sykdomstidslinje: Tidslinje<Sykdomsvurdering>
    ): Tidslinje<Boolean> {
        return Tidslinje.map2(studentTidslinje, sykdomstidslinje) { studentVurdering, sykdomsvurdering ->
            !(sykdomsvurdering?.potensieltOppfyltStudent() != true && studentVurdering?.erOppfylt() == true)
        }
    }

    private fun vurderStudentvilkår(kontekst: FlytKontekstMedPerioder) {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.STUDENT)

        val grunnlag = StudentFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        )
        StudentVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderStudentStegV2(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STUDENT_V2
        }
    }
}