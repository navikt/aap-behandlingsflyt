package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingService
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FagsysteminfoBehovV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.FagsysteminfoSvarHendelse
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.MottakerDto
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.utbetaling.helved.base64ToUUID
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

class HåndterTilbakekrevingHendelseService(
    private val sakService: SakService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        tilbakekrevingService = TilbakekrevingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        vedtakRepository = repositoryProvider.provide()
    )
    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterMottattTilbakekrevingHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
        melding: TilbakekrevingHendelse
    ) {
        when (melding) {
            is TilbakekrevingHendelseV0 -> {
                val behandlingId = finnSisteIverksatteBehandling(sakId)
                log.info("Mottatt tilbakekrevingHendelse for sakId $sakId og behandlingId $behandlingId")
                tilbakekrevingService.håndter(sakId, melding.tilTilbakekrevingshendelse())
                mottaDokumentService.markerSomBehandlet(sakId, behandlingId, referanse)
            }

            is FagsysteminfoBehovV0 -> {
                val behandlingId = finnSisteIverksatteBehandling(sakId)
                log.info("Mottatt fagsysteminfo behov for sakId $sakId og behandlingId $behandlingId")
                tilbakekrevingService.håndter(sakId, melding.tilFagsysteminfoSvarHendelse(sakId))
                mottaDokumentService.markerSomBehandlet(sakId, behandlingId, referanse)
            }
        }
    }

    private fun finnSisteIverksatteBehandling(sakId: SakId): BehandlingId {
        return behandlingRepository.hentAlleFor(sakId).firstOrNull { it.status().erAvsluttet() }?.id
            ?: throw IllegalStateException("Kan ikke finne behandlingId for siste iverksatte behandling")
    }

    private fun TilbakekrevingHendelseV0.tilTilbakekrevingshendelse(): Tilbakekrevingshendelse {
        return Tilbakekrevingshendelse(
            tilbakekrevingBehandlingId = this.tilbakekreving.behandlingId,
            eksternFagsakId = this.eksternFagsakId,
            hendelseOpprettet = this.hendelseOpprettet,
            eksternBehandlingId = this.eksternBehandlingId,
            sakOpprettet = this.tilbakekreving.sakOpprettet,
            varselSendt = this.tilbakekreving.varselSendt,
            behandlingsstatus = TilbakekrevingBehandlingsstatus.valueOf(
                this.tilbakekreving.behandlingsstatus.name
            ),
            totaltFeilutbetaltBeløp = Beløp(this.tilbakekreving.totaltFeilutbetaltBeløp),
            tilbakekrevingSaksbehandlingUrl = URI.create(this.tilbakekreving.saksbehandlingURL),
            fullstendigPeriode = Periode(
                fom = this.tilbakekreving.fullstendigPeriode.fom,
                tom = this.tilbakekreving.fullstendigPeriode.tom
            ),
            versjon = this.versjon,
        )
    }

    private fun FagsysteminfoBehovV0.tilFagsysteminfoSvarHendelse(sakId: SakId): FagsysteminfoSvarHendelse {
        val sak = sakService.hent(sakId)
        val kravgrunnlagReferanse = this.kravgrunnlagReferanse.base64ToUUID()
        val behandling = behandlingRepository.hent(BehandlingReferanse(kravgrunnlagReferanse))
        val årsak = when (behandling.årsakTilOpprettelse) {
            ÅrsakTilOpprettelse.SØKNAD,
            ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
            ÅrsakTilOpprettelse.ANNET_RELEVANT_DOKUMENT,
            ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
            ÅrsakTilOpprettelse.FASTSATT_PERIODE_PASSERT,
            ÅrsakTilOpprettelse.FRITAK_MELDEPLIKT,
            ÅrsakTilOpprettelse.AKTIVITETSMELDING,
            ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE,
            ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE_SAMORDNING_GRADERING,
            ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            ÅrsakTilOpprettelse.AKTIVITETSPLIKT_11_9,
            ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
            ÅrsakTilOpprettelse.MIGRER_RETTIGHETSPERIODE -> FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER

            ÅrsakTilOpprettelse.MELDEKORT,
            ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE -> FagsysteminfoSvarHendelse.RevurderingDto.Årsak.KORRIGERING

            ÅrsakTilOpprettelse.OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS,
            ÅrsakTilOpprettelse.OMGJØRING_ETTER_KLAGE,
            ÅrsakTilOpprettelse.BARNETILLEGG_SATSENDRING,
            ÅrsakTilOpprettelse.SVAR_FRA_KLAGEINSTANS,
            ÅrsakTilOpprettelse.KLAGE -> FagsysteminfoSvarHendelse.RevurderingDto.Årsak.KLAGE

            ÅrsakTilOpprettelse.TILBAKEKREVING_HENDELSE,
            ÅrsakTilOpprettelse.FAGSYSTEMINFO_BEHOV_HENDELSE,

            null -> FagsysteminfoSvarHendelse.RevurderingDto.Årsak.UKJENT // Ikke relevant
        }

        val vedtakstidspunkt = vedtakRepository.hent(behandling.id)?.vedtakstidspunkt ?: error("Fant ikke vedtak")
        val nayEnhetForPerson = tilbakekrevingService.finnNayEnhetForPerson(sak.person.aktivIdent(), behandling)
        return FagsysteminfoSvarHendelse(
            eksternFagsakId = this.eksternFagsakId,
            hendelseOpprettet = LocalDateTime.now(),
            mottaker = MottakerDto(
                ident = sak.person.aktivIdent().identifikator,
                type = MottakerDto.MottakerType.PERSON
            ),
            revurdering = FagsysteminfoSvarHendelse.RevurderingDto(
                behandlingId = kravgrunnlagReferanse.toString(),
                årsak = årsak,
                årsakTilFeilutbetaling = null,
                vedtaksdato = vedtakstidspunkt.toLocalDate(),
            ),
            // TODO: Meldeperioder inkluderer helg i Kelvin, men er mandag-fredag i tilbakekreving. Kan bruke denne for å "slå sammen" to mandag-fredag-perioder til én lang periode.
            utvidPerioder = emptyList(),
            behandlendeEnhet = nayEnhetForPerson.enhetNr,
        )
    }
}


