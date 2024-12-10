package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
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
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderBistandsbehovSteg private constructor(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val bistandsGrunnlag = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            val sykdomsvurdering = sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurdering

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

            val studentVurdering = studentGrunnlag?.studentvurdering

            if (studentVurdering?.erOppfylt() == true || bistandsGrunnlag != null) {
                for (periode in kontekst.perioderTilVurdering) {
                    val grunnlag = BistandFaktagrunnlag(
                        periode.periode.fom,
                        periode.periode.tom,
                        bistandsGrunnlag?.vurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
                }
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

            val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

            // TODO: Se på vurdering fra sykdom om viss varighet
            if (erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurdering) && harBehovForAvklaring(
                    vilkår,
                    kontekst.perioder(),
                    studentVurdering?.erOppfylt() == true
                )
            ) {
                return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
        }

        return Fullført
    }

    private fun harBehovForAvklaring(
        vilkår: Vilkår,
        periodeTilVurdering: Set<Periode>,
        erStudentStegOppfylt: Boolean
    ): Boolean {
        return (vilkår.harPerioderSomIkkeErVurdert(periodeTilVurdering)
                || harInnvilgetForStudentUtenÅVæreStudent(vilkår, erStudentStegOppfylt))
    }

    private fun erIkkeAvslagPåVilkårTidligere(
        vilkårsresultat: Vilkårsresultat,
        sykdomsvurdering: Sykdomsvurdering?
    ): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
            .harPerioderSomErOppfylt() && sykdomsvurdering?.erNedsettelseIArbeidsevneAvEnVissVarighet != false
    }


    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, erStudentStegOppfylt: Boolean): Boolean {

        return !erStudentStegOppfylt && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide(VilkårsresultatRepository::class)
            return VurderBistandsbehovSteg(
                BistandRepository(connection),
                StudentRepository(connection),
                SykdomRepository(connection),
                vilkårsresultatRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
