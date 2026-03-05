package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class HåndterKlageService(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
    )
    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterMottatteKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Klage,
        vurderingsbehov: List<VurderingsbehovMedPeriode>,
    ) {
        when (melding) {
            is KlageV0 -> {
                val sak = sakService.hent(sakId)
                val behandling = if (melding.behandlingReferanse != null) {
                    behandlingRepository.hent(BehandlingReferanse(UUID.fromString(melding.behandlingReferanse)))
                } else {
                    behandlingService.finnEllerOpprettBehandling(
                        sak.saksnummer,
                        VurderingsbehovOgÅrsak(
                            årsak = ÅrsakTilOpprettelse.KLAGE,
                            vurderingsbehov = vurderingsbehov,
                            beskrivelse = melding.beskrivelse,
                            opprettet = mottattTidspunkt
                        )
                    ).åpenBehandling
                } ?: error("Fant ikke behandling for å håndtere mottatt klage på sak $sakId")

                val behandlingSkrivelås = låsRepository.låsBehandling(behandling.id)

                sakService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

                mottaDokumentService.markerSomBehandlet(sakId, behandling.id, referanse)

                prosesserBehandling.triggProsesserBehandling(
                    behandling,
                    vurderingsbehov = vurderingsbehov.map { it.type }
                )
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)
            }
        }
    }

    fun håndterMottattOmgjøringEtterKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        melding: OmgjøringKlageRevurdering,
        vurderingsbehov: List<VurderingsbehovMedPeriode>,
        årsakTilOpprettelse: ÅrsakTilOpprettelse,
    ) {
        log.info("Håndterer mottatt omgjøring etter klage på sak-id $sakId, og referanse $referanse")

        val sak = sakService.hent(sakId)

        val (vurderingsbehovForAktivitetsplikt, vurderingsbehovForYtelsesbehandling) = vurderingsbehov.toSet()
            .partition { it.type in Vurderingsbehov.forAktivitetspliktbehandling() }

        val opprettedeAktivitetspliktBehandlinger = vurderingsbehovForAktivitetsplikt
            .map { it.type }.toSet()
            .map {
                val beskrivelse = melding.beskrivelse
                val opprettet = behandlingService.opprettAktivitetspliktBehandling(
                    sakId,
                    årsakTilOpprettelse,
                    it,
                    mottattTidspunkt,
                    beskrivelse
                )
                prosesserBehandling.triggProsesserBehandling(opprettet)
                opprettet
            }

        if (vurderingsbehovForYtelsesbehandling.isEmpty() && vurderingsbehovForAktivitetsplikt.isNotEmpty()) {
            // TODO: Bør kanskje støtte flere behandlinger - velger den første
            mottaDokumentService.markerSomBehandlet(sak.id, opprettedeAktivitetspliktBehandlinger.first().id, referanse)
            return
        }

        val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                årsak = årsakTilOpprettelse,
                vurderingsbehov = vurderingsbehovForYtelsesbehandling,
                opprettet = mottattTidspunkt,
                beskrivelse = melding.beskrivelse
            )
        )

        val behandlingSkrivelås = opprettetBehandling.åpenBehandling?.let {
            låsRepository.låsBehandling(it.id)
        }

        when (opprettetBehandling) {
            is BehandlingService.Ordinær ->
                mottaDokumentService.oppdaterMedBehandlingId(sakId, opprettetBehandling.åpenBehandling.id, referanse)

            else -> throw IllegalStateException("Forventet ordinær behandling ved omgjøring etter klage")
        }

        prosesserBehandling.triggProsesserBehandling(
            opprettetBehandling,
            vurderingsbehov = vurderingsbehovForYtelsesbehandling.map { it.type }
        )

        if (behandlingSkrivelås != null) {
            låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        }
    }

}


