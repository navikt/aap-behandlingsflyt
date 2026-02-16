package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderSykdomSteg.Companion.type
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklaringsbehovMetadataService(
    private val flytKontekstMedPeriodeService: FlytKontekstMedPeriodeService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider)
    )

    fun perioderSomSkalFremhevesSomIkkeRelevant(
        avklaringsbehovMetadataUtleder: AvklaringsbehovMetadataUtleder,
        behandling: Behandling,
    ): List<Periode> {
        val kontekst = flytKontekstMedPeriodeService.utled(behandling.flytKontekst(), type())
        return avklaringsbehovMetadataUtleder.nårVurderingErRelevant(kontekst)
            .filter { it.verdi }
            .komplement(kontekst.rettighetsperiode) {}
            .perioder()
            .toList()
            .filter { it.fom <= LocalDate.now().plusYears(1) }
    }
}