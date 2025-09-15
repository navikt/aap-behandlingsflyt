package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårService: VilkårService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.SykepengerPeriodisert)) {
            return gammelUtfør(kontekst)
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_SYKEPENGEERSTATNING,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            erTilstrekkeligVurdert = { true }, // ??
            tilbakestillGrunnlag = {
                val vedtatteVurderinger =
                    kontekst.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }
                        ?.vurderinger.orEmpty()

                val aktiveVurderinger =
                    sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)
                        ?.vurderinger.orEmpty()

                if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                    sykepengerErstatningRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            }
        )
        return Fullført
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val kravDato = kontekst.rettighetsperiode.fom

        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje(kravDato)
                ?: Tidslinje.empty()

        val bistandvurderinger =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje(kravDato)
                ?: Tidslinje.empty()

        return Tidslinje.zip3(tidligereVurderingsutfall, sykdomsvurderinger, bistandvurderinger)
            .mapValue { (behandlingsutfall, sykdomsvurdering, bistandvurdering) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        (sykdomsvurdering?.erOppfyltSettBortIfraVissVarighet() == true && !sykdomsvurdering.erOppfylt(
                            kravDato
                        )) || (bistandvurdering?.erBehovForBistand() == false && sykdomsvurdering?.erOppfylt(
                            kravDato
                        ) == true)
                    }
                }
            }
    }

    private fun gammelUtfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for behandlingId ${kontekst.behandlingId}, avbryter steg ${type()}")
                    avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst,
                        Vilkårtype.BISTANDSVILKÅRET,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }

                vurder(kontekst)
            }

            VurderingType.REVURDERING -> {
                // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...

                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for behandlingId ${kontekst.behandlingId}, avbryter steg ${type()}")
                    avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst,
                        Vilkårtype.BISTANDSVILKÅRET,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }
                vurder(kontekst)
            }

            VurderingType.MELDEKORT, VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9, VurderingType.IKKE_RELEVANT -> {
                log.info("Vurderingtype ${kontekst.vurderingType} ikke relevant for steg ${type()} for behandlingId ${kontekst.behandlingId}, fullfører steg.")
                // Do nothing
                Fullført
            }
        }
    }

    private fun vurder(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Vurderer sykepengeerstatning for behandlingId ${kontekst.behandlingId}")
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger.orEmpty()

        val kravDato = kontekst.rettighetsperiode.fom
        val overgangUføre = vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
            ?.harPerioderSomErOppfylt() != true

        val behovForBistand = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET).harPerioderSomErOppfylt()
        val erRelevantÅVurdereSykepengererstatning = sykdomsvurderinger.any { sykdomsvurdering ->
            (sykdomsvurdering.erOppfyltSettBortIfraVissVarighet() && !sykdomsvurdering.erOppfylt(kravDato))
                    || !(behovForBistand && overgangUføre) && sykdomsvurdering.erOppfylt(kravDato)
        }

        log.info("Relevant å vurdere sykepengeerstatning: $erRelevantÅVurdereSykepengererstatning for behandlingId ${kontekst.behandlingId}.")

        if (erRelevantÅVurdereSykepengererstatning) {
            val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (grunnlag?.vurderinger != null) {
                val rettighetsperiode = kontekst.rettighetsperiode
                val vurderingsdato = rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    // TODO: Trenger å finne en god løsning for hvordan vi setter slutt på dette vilkåret ved tom kvote
                    rettighetsperiode.tom, grunnlag.vurderinger
                )

                if (grunnlag.vurderinger.any { it.harRettPå }) { /// !!!!
                    // TODO her bør vi finne en bedre løsning på sikt
                    //      Vi bør sentralisere behandling av vilkår til én vurderer-klasse.
                    vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                        Vilkårsperiode(
                            periode = Periode(faktagrunnlag.vurderingsdato, faktagrunnlag.sisteDagMedMuligYtelse),
                            utfall = Utfall.IKKE_RELEVANT,
                            begrunnelse = null,
                            faktagrunnlag = faktagrunnlag,
                            versjon = ApplikasjonsVersjon.versjon
                        )
                    )
                }
                log.info("Merket bistand som ikke relevant pga innvilget sykepengevilkår.")
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            } else {
                return FantAvklaringsbehov(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        } else {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val sykepengeerstatningsBehov =
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

            if (sykepengeerstatningsBehov?.erÅpent() == true) {
                avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykepengeErstatningSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_SYKEPENGEERSTATNING
        }
    }
}
