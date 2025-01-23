package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderBistandsbehovSteg private constructor(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val bistandsGrunnlag = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sykdomsvurdering = sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurdering

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        kontekst.perioderTilVurdering.forEach { periodeTilVurdering ->
            when (periodeTilVurdering.type) {
                VurderingType.FØRSTEGANGSBEHANDLING -> {
                    // sjekk behovet for avklaring for periode
                    if (erBehovForAvklarForPerioden(
                            periodeTilVurdering.periode,
                            studentGrunnlag,
                            sykdomsvurdering,
                            bistandsGrunnlag,
                            vilkårsresultat
                        )
                    ) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                    }

                    // Vurder vilkår
                    vurderVilkårForPeriode(periodeTilVurdering, bistandsGrunnlag, studentGrunnlag, vilkårsresultat)
                }

                VurderingType.REVURDERING -> {
                    // sjekk behovet for avklaring for periode
                    if (erBehovForAvklarForPerioden(
                            periodeTilVurdering.periode,
                            studentGrunnlag,
                            sykdomsvurdering,
                            bistandsGrunnlag,
                            vilkårsresultat
                        ) || harIkkeVærtAvklartIBehandlingenEnda(avklaringsbehovene)
                    ) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                    }

                    // Vurder vilkår for periode
                    vurderVilkårForPeriode(periodeTilVurdering, bistandsGrunnlag, studentGrunnlag, vilkårsresultat)
                }

                VurderingType.FORLENGELSE -> {
                    val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

                    vilkår.forleng(periodeTilVurdering.periode)
                }
            }
        }
        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        postcondition(vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET))

        return Fullført
    }

    private fun harIkkeVærtAvklartIBehandlingenEnda(avklaringsbehovene: Avklaringsbehovene): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)
        return avklaringsbehov == null
    }

    private fun postcondition(vilkår: Vilkår) {
        if (vilkår.harPerioderSomIkkeErVurdert(vilkår.vilkårsperioder().map { it.periode }.toSet())) {
            throw IllegalStateException("Det finnes perioder som ikke er vurdert")
        }
    }

    private fun erBehovForAvklarForPerioden(
        periode: Periode,
        studentGrunnlag: StudentGrunnlag?,
        sykdomsvurdering: Sykdomsvurdering?,
        bistandsGrunnlag: BistandGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ): Boolean {
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        return erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurdering) && harBehovForAvklaring(
            bistandsGrunnlag,
            periode,
            vilkår,
            studentGrunnlag?.studentvurdering?.erOppfylt() == true
        )
    }


    private fun vurderVilkårForPeriode(
        periode: Vurdering,
        bistandsGrunnlag: BistandGrunnlag?,
        studentGrunnlag: StudentGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ) {
        val grunnlag = BistandFaktagrunnlag(
            periode.periode.fom,
            periode.periode.tom,
            bistandsGrunnlag?.vurdering,
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
        sykdomsvurdering: Sykdomsvurdering?
    ): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
            .harPerioderSomErOppfylt() && sykdomsvurdering?.erOppfylt() != false
    }


    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, erStudentStegOppfylt: Boolean): Boolean {

        return !erStudentStegOppfylt && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide(VilkårsresultatRepository::class)
            val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)
            val bistandRepository = repositoryProvider.provide(BistandRepository::class)
            return VurderBistandsbehovSteg(
                bistandRepository,
                StudentRepository(connection),
                repositoryProvider.provide(),
                vilkårsresultatRepository,
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
