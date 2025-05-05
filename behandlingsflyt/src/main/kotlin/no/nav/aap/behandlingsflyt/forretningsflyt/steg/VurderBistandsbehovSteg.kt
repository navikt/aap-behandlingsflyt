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
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

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
            sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger ?: emptyList()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.BISTANDSVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.BISTANDSVILKÅRET,
                        kontekst.vurdering.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }

                // sjekk behovet for avklaring for periode
                if (erBehovForAvklarForPerioden(
                        kontekst.vurdering.rettighetsperiode,
                        studentGrunnlag,
                        sykdomsvurderinger,
                        bistandsGrunnlag,
                        vilkårsresultat,
                        avklaringsbehovene
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                }

                // Vurder vilkår
                vurderVilkårForPeriode(
                    kontekst.vurdering.rettighetsperiode,
                    bistandsGrunnlag,
                    studentGrunnlag,
                    vilkårsresultat
                )
            }

            VurderingType.REVURDERING -> {
                // sjekk behovet for avklaring for periode
                if (erBehovForAvklarForPerioden(
                        kontekst.vurdering.rettighetsperiode,
                        studentGrunnlag,
                        sykdomsvurderinger,
                        bistandsGrunnlag,
                        vilkårsresultat,
                        avklaringsbehovene
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                }

                // Vurder vilkår for periode
                vurderVilkårForPeriode(
                    kontekst.vurdering.rettighetsperiode,
                    bistandsGrunnlag,
                    studentGrunnlag,
                    vilkårsresultat
                )
            }

            VurderingType.FORLENGELSE,
            VurderingType.IKKE_RELEVANT -> {
                // Skal ikke gjøre noe
            }
        }
        if (kontekst.vurdering.skalVurdereNoe()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        postcondition(vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET))

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)
        return (avklaringsbehov == null || avklaringsbehov.erÅpent())
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
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val erIkkeAvslagPåVilkårTidligere = erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurderinger)
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
            bistandsGrunnlag?.vurderinger ?: emptyList(),
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
        sykdomsvurderinger: List<Sykdomsvurdering>
    ): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
                && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
                && sykdomsvurderinger.any { it.erOppfylt() }
    }


    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, erStudentStegOppfylt: Boolean): Boolean {

        return !erStudentStegOppfylt && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return VurderBistandsbehovSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
