package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
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

class BarnetilleggSteg(
    private val barnetilleggService: BarnetilleggService,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        barnetilleggService = BarnetilleggService(repositoryProvider, gatewayProvider),
        barnetilleggRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder) = when (kontekst.vurderingType) {
        VurderingType.FØRSTEGANGSBEHANDLING -> {
            if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                log.info("Gir avslag eller ingen behandlingsgrunnlag, avbryter steg. BehandlingId: ${kontekst.behandlingId}.")
                avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                    .avbrytForSteg(type())
                Fullført
            } else {
                vurder(kontekst)
            }
        }

        VurderingType.REVURDERING -> {
            vurder(kontekst)
        }

        VurderingType.MELDEKORT,
        VurderingType.AKTIVITETSPLIKT,
        VurderingType.IKKE_RELEVANT -> {
            /* do nothing */
            Fullført
        }
    }

    private fun vurder(kontekst: FlytKontekstMedPerioder): StegResultat {
        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        barnetilleggRepository.lagre(
            kontekst.behandlingId,
            barnetillegg.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barnMedRettTil()
                    )
                }
        )

        if (barnetillegg.segmenter().any { it.verdi.harBarnTilAvklaring() }) {
            val perioderTilAvklaring = barnetillegg.segmenter().filter { it.verdi.harBarnTilAvklaring() }
            log.info("Det finnes perioder med barn som ikke har blitt avklart. Antall: ${perioderTilAvklaring.size}")
            return FantAvklaringsbehov(Definisjon.AVKLAR_BARNETILLEGG)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BarnetilleggSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}