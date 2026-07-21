package no.nav.aap.behandlingsflyt.behandling.vurdering

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
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

        return medNavnOgEnhet(kvalitetsikring.endretAv, kvalitetsikring.tidsstempel).copy(
            erRetur = kvalitetsikring.status == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        )
    }

    fun besluttetAv(definisjon: Definisjon, behandlingId: BehandlingId): VurdertAvResponse? {
        val beslutning = avklaringsbehoveneFor(behandlingId)
            .hentNyesteBeslutningGittDefinisjon(definisjon)
            ?: return null

        return medNavnOgEnhet(beslutning.endretAv, beslutning.tidsstempel).copy(
            erRetur = beslutning.status == Status.SENDT_TILBAKE_FRA_BESLUTTER
        )
    }

    fun byggVurderingerMeta(
        definisjon: Definisjon,
        behandlingId: BehandlingId,
        vurdertAv: VurdertAvResponse?,
    ) = VurderingerMetaResponse(
        vurdertAv = vurdertAv,
        kvalitetssikretAv = kvalitetssikretAv(definisjon, behandlingId),
        besluttetAv = besluttetAv(definisjon, behandlingId),
    )

    fun medNavnOgEnhet(ident: Bruker, dato: LocalDate): VurdertAvResponse {
        val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(ident)
        return VurdertAvResponse(
            ident = ident.ident,
            dato = dato,
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet,
        )
    }

    fun medNavnOgEnhet(ident: Bruker, tidspunkt: LocalDateTime) =
        medNavnOgEnhet(ident, tidspunkt.toLocalDate())

    fun medNavnOgEnhet(ident: Bruker, instant: Instant) =
        medNavnOgEnhet(ident, instant.atZone(ZoneId.of("Europe/Oslo")).toLocalDate())
}