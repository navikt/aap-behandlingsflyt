package no.nav.aap.behandlingsflyt.hendelse.mottak

import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilVurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
    )

    fun håndterMottatteDokumenter(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        log.info("Mottok dokument på sak-id $sakId, og referanse $referanse, med brevkategori $brevkategori.")
        val sak = sakService.hent(sakId)
        val vurderingsbehov = MottattHendelseUtleder.utledVurderingsbehov(brevkategori, melding)
        val årsakTilOpprettelse = MottattHendelseUtleder.utledÅrsakTilOpprettelse(brevkategori, melding)

        val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                årsak = årsakTilOpprettelse,
                vurderingsbehov = vurderingsbehov,
                opprettet = mottattTidspunkt,
                beskrivelse = MottattHendelseUtleder.utledBeskrivelseForÅrsakTilOpprettelse(melding)
            )
        )

        val behandlingSkrivelås = opprettetBehandling.åpenBehandling?.let {
            låsRepository.låsBehandling(it.id)
        }

        sakService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

        if (skalMarkereDokumentSomBehandlet(melding)) {
            require(opprettetBehandling is BehandlingService.Ordinær) {
                "Forventet ordinær behandling ved mottak av dokumenter som skal markeres som behandlet"
            }
            mottaDokumentService.markerSomBehandlet(sakId, opprettetBehandling.åpenBehandling.id, referanse)
        } else {
            knyttDokumentTilRiktigBehandling(opprettetBehandling, sakId, referanse)
        }

        prosesserBehandling.triggProsesserBehandling(
            opprettetBehandling,
            vurderingsbehov = vurderingsbehov.map { it.type }
        )

        if (behandlingSkrivelås != null) {
            låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        }
    }

    fun oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
        sakId: SakId,
        behandlingsreferanse: BehandlingReferanse,
        innsendingType: InnsendingType,
        melding: NyÅrsakTilBehandlingV0,
        referanse: InnsendingReferanse
    ) {
        val behandling = behandlingRepository.hent(behandlingsreferanse)
        val årsakTilOpprettelse = MottattHendelseUtleder.utledÅrsakTilOpprettelse(innsendingType, melding)

        låsRepository.withLåstBehandling(behandling.id) {
            val vurderingsbehov =
                melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
            behandlingService.oppdaterVurderingsbehovOgÅrsak(
                behandling,
                VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse, beskrivelse = melding.beskrivelse)
            )
            mottaDokumentService.markerSomBehandlet(sakId, behandling.id, referanse)
            prosesserBehandling.triggProsesserBehandling(
                sakId,
                behandling.id,
                vurderingsbehov = vurderingsbehov.map { it.type }
            )
        }
    }

    private fun knyttDokumentTilRiktigBehandling(
        opprettetBehandling: BehandlingService.OpprettetBehandling,
        sakId: SakId,
        referanse: InnsendingReferanse
    ) {
        when (opprettetBehandling) {
            is BehandlingService.Ordinær -> mottaDokumentService.oppdaterMedBehandlingId(
                sakId,
                opprettetBehandling.åpenBehandling.id,
                referanse
            )

            is BehandlingService.MåBehandlesAtomært -> mottaDokumentService.oppdaterMedBehandlingId(
                sakId,
                opprettetBehandling.nyBehandling.id,
                referanse
            )
        }
    }

    /**
     * Knytter klage og oppfølgingsbehandling direkte til behandlingen den opprettet, ikke via informasjonskrav.
     * Dette fordi det være flere åpne behandlinger av disse typene.
     * ManuellVurdering og NyÅrsakTilBehandling er knyttet eksplisitt til behandling og er ikke et informasjonskrav i flyten
     */
    private fun skalMarkereDokumentSomBehandlet(melding: Melding?): Boolean =
        melding is KabalHendelse || melding is Oppfølgingsoppgave || melding is ManuellRevurdering || melding is NyÅrsakTilBehandling || melding is TilbakekrevingHendelse

}
