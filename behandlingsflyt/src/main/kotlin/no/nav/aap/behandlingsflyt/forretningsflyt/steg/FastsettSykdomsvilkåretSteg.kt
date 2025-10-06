package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettSykdomsvilkåretSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    vilkårService.ingenNyeVurderinger(
                        kontekst,
                        Vilkårtype.SYKDOMSVILKÅRET,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }
                vurderVilkåret(kontekst)
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
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
        val sykepengerErstatningGrunnlag = sykepengerErstatningRepository.hentHvisEksisterer(behandlingId)

        val rettighetsperiode = kontekst.rettighetsperiode
        val faktagrunnlag = SykdomsFaktagrunnlag(
            kontekst.behandlingType,
            rettighetsperiode.fom,
            rettighetsperiode.tom,
            sykdomsGrunnlag?.yrkesskadevurdering,
            sykepengerErstatningGrunnlag?.vurdering,
            sykdomsGrunnlag?.sykdomsvurderinger.orEmpty(),
            studentGrunnlag?.studentvurdering
        )
        Sykdomsvilkår(vilkårResultat).vurder(faktagrunnlag)

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårResultat)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FastsettSykdomsvilkåretSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_SYKDOMSVILKÅRET
        }
    }
}
