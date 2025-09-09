package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import kotlin.collections.orEmpty

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_UFORE)
        when (kontekst.vurderingType) {

            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.OVERGANGUFØREVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.OVERGANGUFØREVILKÅRET,
                        kontekst.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }
                if (harVurdertBistandsVilkår(avklaringsbehovene) && !bistandsVilkårErOppfylt(kontekst.behandlingId) && harIkkeVurdert_11_18_tidligere(
                        avklaringsbehovene
                    )
                ) {
                    settBistandsBehovTilIkkeRelevant(kontekst)
                    return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_UFORE)
                } else {
                    if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_OVERGANG_UFORE)
                    }
                    vurderVilkårForPeriode(
                        kontekst.rettighetsperiode,
                        overgangUføreGrunnlag,
                        vilkårsresultat
                    )
                }

            }
            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.IKKE_RELEVANT -> {
                // Skal ikke gjøre noe
            }
        }
        if (kontekst.harNoeTilBehandling()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return Fullført
    }


    private fun harVurdertBistandsVilkår(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_BISTANDSBEHOV)
    }

    private fun bistandsVilkårErOppfylt(behandlingId: BehandlingId): Boolean {
        val alleBistandsVilkårOppfylt =
            vilkårsresultatRepository.hent(behandlingId).finnVilkår(Vilkårtype.BISTANDSVILKÅRET).vilkårsperioder()
                .all { it.erOppfylt() }
        return alleBistandsVilkårOppfylt
    }

    private fun harIkkeVurdert_11_18_tidligere(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_OVERGANG_UFORE)
    }

    private fun settBistandsBehovTilIkkeRelevant(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(kontekst.rettighetsperiode.fom, kontekst.rettighetsperiode.tom),
                utfall = Utfall.IKKE_RELEVANT,
                begrunnelse = null,
                versjon = ApplikasjonsVersjon.versjon
            )
        )
        log.info("Merket bistand som ikke relevant pga innvilget overgang uføre - vilkår.")
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)


        return Fullført
    }

    private fun vurderVilkårForPeriode(
        periode: Periode,
        overgangUføreGrunnlag: OvergangUføreGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ) {
        val grunnlag = OvergangUføreFaktagrunnlag(
            periode.fom,
            periode.tom,
            overgangUføreGrunnlag?.vurderinger.orEmpty(),
        )
        OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
    }

    companion object : FlytSteg {

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {

            return OvergangUføreSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
