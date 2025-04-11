package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettSykdomsvilkåretSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                vurderVilkåret(kontekst)
            }

            VurderingType.REVURDERING -> {
                vurderVilkåret(kontekst)
            }

            VurderingType.FORLENGELSE -> {
                // Forleng vilkåret
                val forlengensePeriode = requireNotNull(kontekst.vurdering.forlengelsePeriode)
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).forleng(
                    forlengensePeriode
                )
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun vurderVilkåret(
        kontekst: FlytKontekstMedPerioder,
    ) {
        val behandlingId = kontekst.behandlingId
        val vilkårResultat = vilkårsresultatRepository.hent(behandlingId)
        val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)

        val rettighetsperiode = kontekst.vurdering.rettighetsperiode
        val faktagrunnlag = SykdomsFaktagrunnlag(
            rettighetsperiode.fom,
            rettighetsperiode.tom,
            sykdomsGrunnlag?.yrkesskadevurdering,
            sykdomsGrunnlag?.sykdomsvurderinger ?: emptyList(),
            studentGrunnlag?.studentvurdering
        )
        Sykdomsvilkår(vilkårResultat).vurder(faktagrunnlag)

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårResultat)
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettSykdomsvilkåretSteg(RepositoryProvider(connection))
        }

        override fun type(): StegType {
            return StegType.FASTSETT_SYKDOMSVILKÅRET
        }
    }
}
