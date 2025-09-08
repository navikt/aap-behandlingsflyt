package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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

class OvergangArbeidSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val overgangArbeidGrunnlag = overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sykdomgrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_ARBEID)
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                return Fullført
            }

            VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.OVERGANGARBEIDVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.OVERGANGARBEIDVILKÅRET,
                        kontekst.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }
                if (harVurdertBistandsVilkår(avklaringsbehovene) && bistandsVilkårErOppfylt(kontekst.behandlingId) && !brukerHarTilstrekkeligNedsattArbeidsevne(
                        sykdomgrunnlag) && harIkkeVurdert_11_17_tidligere(
                        avklaringsbehovene
                    )
                ) {
                    settSykdomsVilkårTilIkkeRelevant(kontekst)
                    return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_ARBEID)
                } else {
                    if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_OVERGANG_ARBEID)
                    }
                    vurderVilkårForPeriode(
                        kontekst.rettighetsperiode,
                        overgangArbeidGrunnlag,
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

    private fun brukerHarTilstrekkeligNedsattArbeidsevne(sykdomGrunnlag: SykdomGrunnlag?): Boolean {
        if (sykdomGrunnlag == null) return false
        val sisteSykdomsvurdering = sykdomGrunnlag.sykdomsvurderinger.maxByOrNull { it.opprettet }
        if (sisteSykdomsvurdering == null) {
            return false
        } else {
            if (sisteSykdomsvurdering.erArbeidsevnenNedsatt == true) {
                return true
            } else
                return false
        }
    }

    private fun harIkkeVurdert_11_17_tidligere(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_OVERGANG_ARBEID)
    }

    private fun settSykdomsVilkårTilIkkeRelevant(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).leggTilVurdering(
            Vilkårsperiode(
                periode = Periode(kontekst.rettighetsperiode.fom, kontekst.rettighetsperiode.tom),
                utfall = Utfall.IKKE_RELEVANT,
                begrunnelse = null,
                versjon = ApplikasjonsVersjon.versjon
            )
        )
        log.info("Merket sykdom som ikke relevant pga innvilget arbeidssøker - vilkår.")
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)


        return Fullført
    }


    private fun vurderVilkårForPeriode(
        periode: Periode,
        overgangArbeidGrunnlag: OvergangArbeidGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ) {
        val grunnlag = OvergangArbeidFaktagrunnlag(
            periode.fom,
            periode.tom,
            overgangArbeidGrunnlag?.vurderinger.orEmpty(),
        )
        OvergangArbeidVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangArbeidSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_ARBEID
        }
    }
}