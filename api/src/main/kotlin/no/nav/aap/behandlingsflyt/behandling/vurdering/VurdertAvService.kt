package no.nav.aap.behandlingsflyt.behandling.vurdering

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class VurdertAvService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val ansattInfoService: AnsattInfoService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        ansattInfoService = AnsattInfoService(gatewayProvider),
    )

    private val avklaringsbehoveneMap = ConcurrentHashMap<BehandlingId, Avklaringsbehovene>()

    private fun avklaringsbehoveneFor(behandlingId: BehandlingId) =
        avklaringsbehoveneMap.computeIfAbsent(behandlingId) {
            avklaringsbehovRepository.hentAvklaringsbehovene(it)
        }

    fun kvalitetssikretAv(definisjon: Definisjon, behandlingId: BehandlingId): VurdertAvResponse? {
        val kvalitetsikring = avklaringsbehoveneFor(behandlingId)
            .hentNyesteKvalitetssikringGittDefinisjon(definisjon)
            ?: return null
        return medNavnOgEnhet(kvalitetsikring.endretAv, kvalitetsikring.tidsstempel)
    }

    fun besluttetAv(definisjon: Definisjon, behandlingId: BehandlingId): VurdertAvResponse? {
        val beslutning = avklaringsbehoveneFor(behandlingId)
            .beslutningFor(definisjon)
            ?: return null
        return medNavnOgEnhet(beslutning.endretAv, beslutning.tidsstempel)
    }

    fun medNavnOgEnhet(ident: String, dato: LocalDate): VurdertAvResponse {
        val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(ident)
        return VurdertAvResponse(
            ident = ident,
            dato = dato,
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet,
        )
    }

    fun medNavnOgEnhet(ident: String, tidspunkt: LocalDateTime) =
        medNavnOgEnhet(ident, tidspunkt.toLocalDate())

    fun medNavnOgEnhet(ident: String, instant: Instant) =
        medNavnOgEnhet(ident, instant.atZone(ZoneId.of("Europe/Oslo")).toLocalDate())
}