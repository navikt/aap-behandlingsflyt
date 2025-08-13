package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class EtAnnetStedSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        etAnnetStedUtlederService = EtAnnetStedUtlederService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING) {
            if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                avklaringsbehovene.avbrytForSteg(type())
                return Fullført
            }
        }

        val avklaringsbehov = mutableListOf<Definisjon>()

        val harBehovForAvklaringer = etAnnetStedUtlederService.utled(kontekst.behandlingId, true)
        val harBehovForAvklaringerLenger = etAnnetStedUtlederService.utled(kontekst.behandlingId)
        log.info("Perioder til vurdering: {}", harBehovForAvklaringerLenger.perioderTilVurdering)
        if (harBehovForAvklaringerLenger.harBehovForAvklaring()) {
            avklaringsbehov += harBehovForAvklaringerLenger.avklaringsbehov()
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_HELSEINSTITUSJON) && !harBehovForAvklaringer.avklaringsbehov()
                .contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
        ) {
            avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene, Definisjon.AVKLAR_HELSEINSTITUSJON)
        }

        if (!avklaringsbehov.contains(Definisjon.AVKLAR_SONINGSFORRHOLD) && !harBehovForAvklaringer.avklaringsbehov()
                .contains(Definisjon.AVKLAR_SONINGSFORRHOLD)
        ) {
            avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene, Definisjon.AVKLAR_SONINGSFORRHOLD)
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FantAvklaringsbehov(avklaringsbehov)
        }

        return Fullført
    }

    private fun avbrytHvisFinnesOgIkkeTrengs(avklaringsbehovene: Avklaringsbehovene, definisjon: Definisjon) {
        val eksisterendeBehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (eksisterendeBehov?.erÅpent() == true) {
            avklaringsbehovene.avbryt(definisjon)
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