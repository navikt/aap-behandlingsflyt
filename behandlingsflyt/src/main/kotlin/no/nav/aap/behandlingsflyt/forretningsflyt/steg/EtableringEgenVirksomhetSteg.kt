package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class EtableringEgenVirksomhetSteg(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val etableringEgenVirksomhetService: EtableringEgenVirksomhetService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        etableringEgenVirksomhetService = EtableringEgenVirksomhetService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.VirksomhetsEtablering)) {
            return Fullført
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.ETABLERING_EGEN_VIRKSOMHET,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            nårVurderingErGyldig = { tilstrekkeligVurdert(kontekst) },
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }
                    ?.vurderinger.orEmpty()
                etableringEgenVirksomhetRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
            },
        )

        return Fullført
    }

    private fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val relevantPeriode =
            etableringEgenVirksomhetService.utledGyldighetsPeriode(kontekst.behandlingId, LocalDate.now().plusDays(1))
                .somTidslinje { it }

        return Tidslinje.map2(tidligereVurderingsutfall, relevantPeriode) { utfall, relevantPeriode ->
            when (utfall) {
                null -> false
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> true
            }
        }
    }

    private fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val grunnlag = etableringEgenVirksomhetRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (grunnlag == null || grunnlag.vurderinger.isEmpty()) {
            return Tidslinje(kontekst.rettighetsperiode, true)
        }

        val evaluering = grunnlag.vurderinger.let {
            etableringEgenVirksomhetService.erVurderingerGyldig(
                behandlingId = kontekst.behandlingId,
                nyeVurderinger = it
            )
        }

        return Tidslinje(kontekst.rettighetsperiode, evaluering.erOppfylt)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return EtableringEgenVirksomhetSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.ETABLERING_EGEN_VIRKSOMHET
        }
    }
}