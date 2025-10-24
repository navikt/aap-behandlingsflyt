package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class EtAnnetStedSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,


    ) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        etAnnetStedUtlederService = EtAnnetStedUtlederService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)


        val avslagEllerIngenBehandlingsgrunnlag = (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING ||
            kontekst.vurderingType == VurderingType.REVURDERING) && tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())

        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING &&
            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())
        ) {
            avklaringsbehovene.avbrytForSteg(type())
            return Fullført
        }

        if (kontekst.vurderingType == VurderingType.REVURDERING &&
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())
        ) {
            avklaringsbehovene.avbrytForSteg(type())
            return Fullført
        }

        val avklaringsbehov = mutableListOf<Definisjon>()

        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId, true)
        val harBehovForAvklaringerLenger = etAnnetStedUtlederService.utled(kontekst.behandlingId)
        log.info("Perioder til vurdering: {}", harBehovForAvklaringerLenger.perioderTilVurdering)
        if (harBehovForAvklaringerLenger.harBehovForAvklaring()) {
            avklaringsbehov += harBehovForAvklaringerLenger.avklaringsbehov()
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_HELSEINSTITUSJON) && harBehovForAvklaringer.avklaringsbehov()
                .contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
        ) {

            avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
                avklaringsbehovene = avklaringsbehovene,
                behandlingRepository = behandlingRepository,
                vilkårsresultatRepository = vilkårsresultatRepository,
                erTilstrekkeligVurdert = {true},
                kontekst = kontekst,
                tilbakestillGrunnlag = {},
                definisjon = Definisjon.AVKLAR_HELSEINSTITUSJON,
                tvingerAvklaringsbehov =  setOf<Vurderingsbehov>( Vurderingsbehov.INSTITUSJONSOPPHOLD),
                nårVurderingErRelevant = ::perioderMedVurderingsbehovHelse)
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_SONINGSFORRHOLD) && harBehovForAvklaringer.avklaringsbehov()
                .contains(Definisjon.AVKLAR_SONINGSFORRHOLD)
        ) {
            avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
                avklaringsbehovene = avklaringsbehovene,
                behandlingRepository = behandlingRepository,
                vilkårsresultatRepository = vilkårsresultatRepository,
                erTilstrekkeligVurdert = {true},
                kontekst = kontekst,
                tilbakestillGrunnlag = {},
                definisjon = Definisjon.AVKLAR_HELSEINSTITUSJON,
                tvingerAvklaringsbehov = setOf<Vurderingsbehov>( Vurderingsbehov.INSTITUSJONSOPPHOLD),
                nårVurderingErRelevant = ::perioderMedVurderingsbehovSoning)
        }



        return Fullført
    }


    private fun perioderMedVurderingsbehovHelse(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst,
            type())
        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId). .perioderTilVurdering.flatMap { perioderMedVurderingsbehovSoning(kontekst) }

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                          denneBehandling != null && denneBehandling
                    }
                }
            }
    }


    private fun perioderMedVurderingsbehovSoning(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst,
            type())

        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId)

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer.perioderTilVurdering)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        denneBehandling != null && denneBehandling.soning != null
                    }
                }
            }
    }


    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return EtAnnetStedSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}