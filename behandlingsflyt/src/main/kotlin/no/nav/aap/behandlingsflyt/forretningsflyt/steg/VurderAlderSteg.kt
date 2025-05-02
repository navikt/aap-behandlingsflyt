package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.alder.Aldersvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAlderSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val vilkårService: VilkårService,
    private val personopplysningRepository: PersonopplysningRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        vilkårService = VilkårService(repositoryProvider),
        personopplysningRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.ALDERSVILKÅRET,
                        kontekst.vurdering.rettighetsperiode,
                        begrunnelse = "mangler behandlingsgrunnlag"
                    )
                } else {
                    vurderVilkår(kontekst)
                }
            }

            VurderingType.REVURDERING -> {
                vurderVilkår(kontekst)
            }

            VurderingType.FORLENGELSE,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder) {
        val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?: throw IllegalStateException("Forventet å finne personopplysninger")

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val aldersgrunnlag =
            Aldersgrunnlag(
                kontekst.vurdering.rettighetsperiode,
                personopplysningGrunnlag.brukerPersonopplysning.fødselsdato
            )
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return VurderAlderSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_ALDER
        }
    }
}
