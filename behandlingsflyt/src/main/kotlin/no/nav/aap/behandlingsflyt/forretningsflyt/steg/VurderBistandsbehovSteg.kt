package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VurderBistandsbehovSteg private constructor(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        sykdomsRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val bistandsGrunnlag = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sykdomsvurderinger =
            sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger.orEmpty()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.BISTANDSVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.BISTANDSVILKÅRET,
                        kontekst.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }

                // sjekk behovet for avklaring for periode
                if (erBehovForAvklarForPerioden(
                        kontekst.rettighetsperiode,
                        studentGrunnlag,
                        sykdomsvurderinger,
                        bistandsGrunnlag,
                        vilkårsresultat,
                        avklaringsbehovene,
                        TypeBehandling.Førstegangsbehandling,
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                }

                // Vurder vilkår
                vurderVilkårForPeriode(
                    kontekst.rettighetsperiode,
                    bistandsGrunnlag,
                    studentGrunnlag,
                    vilkårsresultat
                )
            }

            VurderingType.REVURDERING -> {
                // sjekk behovet for avklaring for periode
                if (erBehovForAvklarForPerioden(
                        kontekst.rettighetsperiode,
                        studentGrunnlag,
                        sykdomsvurderinger,
                        bistandsGrunnlag,
                        vilkårsresultat,
                        avklaringsbehovene,
                        TypeBehandling.Revurdering
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                }

                // Vurder vilkår for periode
                vurderVilkårForPeriode(
                    kontekst.rettighetsperiode,
                    bistandsGrunnlag,
                    studentGrunnlag,
                    vilkårsresultat
                )
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Skal ikke gjøre noe
            }
        }
        if (kontekst.harNoeTilBehandling()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        postcondition(vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET))

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_BISTANDSBEHOV)
    }

    private fun postcondition(vilkår: Vilkår) {
        if (vilkår.harPerioderSomIkkeErVurdert(vilkår.vilkårsperioder().map { it.periode }.toSet())) {
            throw IllegalStateException("Det finnes perioder som ikke er vurdert")
        }
    }

    private fun erBehovForAvklarForPerioden(
        periode: Periode,
        studentGrunnlag: StudentGrunnlag?,
        sykdomsvurderinger: List<Sykdomsvurdering>,
        bistandsGrunnlag: BistandGrunnlag?,
        vilkårsresultat: Vilkårsresultat,
        avklaringsbehovene: Avklaringsbehovene,
        typeBehandling: TypeBehandling,
    ): Boolean {
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val erIkkeAvslagPåVilkårTidligere =
            erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurderinger, typeBehandling, periode.fom)
        if (!erIkkeAvslagPåVilkårTidligere || studentGrunnlag?.studentvurdering?.erOppfylt() == true) {
            return false
        }
        return harBehovForAvklaring(
            bistandsGrunnlag,
            periode,
            vilkår,
            studentGrunnlag?.studentvurdering?.erOppfylt() == true
        ) || erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)
    }


    private fun vurderVilkårForPeriode(
        periode: Periode,
        bistandsGrunnlag: BistandGrunnlag?,
        studentGrunnlag: StudentGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ) {
        val grunnlag = BistandFaktagrunnlag(
            periode.fom,
            periode.tom,
            bistandsGrunnlag?.vurderinger.orEmpty(),
            studentGrunnlag?.studentvurdering
        )
        Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
    }

    private fun harBehovForAvklaring(
        bistandsGrunnlag: BistandGrunnlag?,
        periodeTilVurdering: Periode,
        vilkår: Vilkår,
        erStudentStegOppfylt: Boolean
    ): Boolean {
        return !harVurdertPerioden(periodeTilVurdering, bistandsGrunnlag)
                || harInnvilgetForStudentUtenÅVæreStudent(vilkår, erStudentStegOppfylt)
    }

    private fun harVurdertPerioden(
        periode: Periode,
        grunnlag: BistandGrunnlag?
    ): Boolean {
        return grunnlag?.harVurdertPeriode(periode) == true
    }

    private fun erIkkeAvslagPåVilkårTidligere(
        vilkårsresultat: Vilkårsresultat,
        sykdomsvurderinger: List<Sykdomsvurdering>,
        typeBehandling: TypeBehandling,
        kravDato: LocalDate,
    ): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
                && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
                && sykdomsvurderinger.any { it.erOppfylt(typeBehandling, kravDato) }
    }


    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, erStudentStegOppfylt: Boolean): Boolean {

        return !erStudentStegOppfylt && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return VurderBistandsbehovSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
