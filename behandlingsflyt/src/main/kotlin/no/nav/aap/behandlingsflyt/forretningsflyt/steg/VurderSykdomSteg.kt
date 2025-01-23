package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg private constructor(
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        val vilkårResultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val studentVurdering = studentGrunnlag?.studentvurdering

        kontekst.perioderTilVurdering.forEach { periodeTilVurdering ->
            when (periodeTilVurdering.type) {
                VurderingType.FØRSTEGANGSBEHANDLING -> {

                    if (skalStoppeIFørstegangsbehandling(
                            vilkårResultat,
                            studentVurdering,
                            sykdomsGrunnlag,
                            yrkesskadeGrunnlag
                        )
                    ) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
                    } else {
                        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                        if (avklaringsbehov != null && avklaringsbehov.erÅpent() && studentVurdering?.erOppfylt() == true) {
                            avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKDOM)
                        }
                    }
                }

                VurderingType.REVURDERING -> {
                    if (skalStoppeIRevurdering(avklaringsbehovene)) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
                    } else {
                        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                        if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                            avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKDOM)
                        }
                    }
                }

                VurderingType.FORLENGELSE -> {
                    // Skal ikke tvinge noen form for vurdering
                }
            }
        }

        return Fullført
    }

    private fun skalStoppeIRevurdering(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
        return (avklaringsbehov == null || avklaringsbehov.erÅpent())
    }

    private fun skalStoppeIFørstegangsbehandling(
        vilkårResultat: Vilkårsresultat,
        studentVurdering: StudentVurdering?,
        sykdomsGrunnlag: SykdomGrunnlag?,
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?
    ): Boolean {
        return erIkkeAvslagPåVilkårTidligere(vilkårResultat) &&
                (studentVurdering?.erOppfylt() != true
                        && sykdomsGrunnlag?.erKonsistentForSykdom(harYrkesskade(yrkesskadeGrunnlag)) != true)
    }

    private fun harYrkesskade(yrkesskadeGrunnlag: YrkesskadeGrunnlag?): Boolean =
        yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true

    private fun erIkkeAvslagPåVilkårTidligere(vilkårsresultat: Vilkårsresultat): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            return VurderSykdomSteg(
                repositoryProvider.provide(),
                StudentRepository(connection),
                repositoryProvider.provide(),
                vilkårsresultatRepository,
                avklaringsbehovRepository,
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
