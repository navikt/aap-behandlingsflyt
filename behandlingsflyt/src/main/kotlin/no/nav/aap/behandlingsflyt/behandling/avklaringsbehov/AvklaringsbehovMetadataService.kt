package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklaringsbehovMetadataService(
    private val flytKontekstMedPeriodeService: FlytKontekstMedPeriodeService,
    private val unleashGateway: UnleashGateway,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    fun perioderSomSkalFremhevesSomIkkeRelevant(
        avklaringsbehovMetadataUtleder: AvklaringsbehovMetadataUtleder,
        behandling: Behandling,
    ): List<Periode> {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.VisIkkeRelevantPeriode)) {
            val kontekst = flytKontekstMedPeriodeService.utled(
                behandling.flytKontekst(),
                avklaringsbehovMetadataUtleder.stegType
            )
            return avklaringsbehovMetadataUtleder.nårVurderingErRelevant(kontekst)
                .filter { it.verdi }
                .komplement(kontekst.rettighetsperiode) {}
                .perioder()
                .toList()
                .filter { it.fom <= LocalDate.now().plusYears(1) }
        } else {
            return emptyList()
        }
    }
}