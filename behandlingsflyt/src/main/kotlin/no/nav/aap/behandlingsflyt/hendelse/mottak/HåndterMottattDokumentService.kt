package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingService
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FagsysteminfoBehovV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Omgjøringskilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.OppdagEndretInformasjonskravJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.FagsysteminfoSvarHendelse
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.MottakerDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilVurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetaling.helved.base64ToUUID
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime
import java.util.*

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val flytJobbRepository: FlytJobbRepository,
    private val rettighetstypeService: RettighetstypeService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
        vedtakRepository = repositoryProvider.provide<VedtakRepository>(),
        tilbakekrevingService = TilbakekrevingService(repositoryProvider, gatewayProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>(),
        rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
    )

    fun håndterMottatteKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Klage,
    ) {
        when (melding) {
            is KlageV0 -> {
                val sak = sakService.hent(sakId)
                val vurderingsbehov = utledVurderingsbehov(brevkategori, melding)

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
                }

                val behandlingSkrivelås = behandling?.let {
                    låsRepository.låsBehandling(it.id)
                }

                sakService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

                mottaDokumentService.markerSomBehandlet(sakId, behandling!!.id, referanse)

                prosesserBehandling.triggProsesserBehandling(
                    behandling,
                    vurderingsbehov = vurderingsbehov.map { it.type }
                )

                if (behandlingSkrivelås != null) {
                    låsRepository.verifiserSkrivelås(behandlingSkrivelås)
                }
            }
        }

    }

    fun håndterMottattOmgjøringEtterKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: OmgjøringKlageRevurdering,
    ) {
        log.info("Håndterer dokument på sak-id $sakId, og referanse $referanse, med brevkategori $brevkategori")

        val sak = sakService.hent(sakId)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(brevkategori, melding)
        val vurderingsbehov = utledVurderingsbehov(brevkategori, melding)

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

    fun håndterMottatteDokumenter(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        log.info("Mottok dokument på sak-id $sakId, og referanse $referanse, med brevkategori $brevkategori.")
        val sak = sakService.hent(sakId)
        val vurderingsbehov = utledVurderingsbehov(brevkategori, melding)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(brevkategori, melding)

        val opprettetBehandling = behandlingService.finnEllerOpprettBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                årsak = årsakTilOpprettelse,
                vurderingsbehov = vurderingsbehov,
                opprettet = mottattTidspunkt,
                beskrivelse = when (melding) {
                    is ManuellRevurderingV0 -> melding.beskrivelse
                    is OmgjøringKlageRevurdering -> melding.beskrivelse
                    is PdlHendelseV0 -> melding.beskrivelse
                    is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.joinToString(", ")
                    is AnnetRelevantDokument -> melding.begrunnelse
                    else -> null
                }
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

        prosesserBehandling.triggProsesserBehandling(
            opprettetBehandling,
            vurderingsbehov = vurderingsbehov.map { it.type }
        )

        if (behandlingSkrivelås != null) {
            låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        }
    }

    fun håndterMottattDialogMelding(
        sakId: SakId,
        referanse: InnsendingReferanse,
        brevkategori: InnsendingType,
        melding: Melding?
    ) {
        val sak = sakService.hent(sakId)
        val vurderingsbehov = utledVurderingsbehov(brevkategori, melding)
        log.info("Håndterer dialogmelding for ${sak.id}")
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sak.id)

        if (sisteYtelsesBehandling != null) {
            mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
            log.info("Markerer dialogmelding som behandlet ${sisteYtelsesBehandling.id}")
            if (sisteYtelsesBehandling.status().erÅpen()) {
                prosesserBehandling.triggProsesserBehandling(
                    sisteYtelsesBehandling,
                    vurderingsbehov = vurderingsbehov.filter { it.type == Vurderingsbehov.MOTTATT_DIALOGMELDING }
                        .map { it.type }
                )
                log.info("Prosessert behandling etter mottatt dialogmelding ${sisteYtelsesBehandling.id}")
            }

        }
    }

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

    fun håndterMottattSykepengevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
    ) {
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")
        if (!trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            flytJobbRepository.leggTil(
                JobbInput(jobb = OppdagEndretInformasjonskravJobbUtfører).forSak(sakId.toLong()).medCallId()
            )
        }
        mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
    }

    fun håndterMottattUførevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
        uførevedtak: UførevedtakV0,
        mottattTidspunkt: LocalDateTime
    ) {
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")
        if (uførevedtak.resultat.erOpphørEllerEndring()) {
            log.info("Uførevedtak for sak $sakId er opphør eller endring, gjør ingenting i Kelvin med dette")
        } else if (trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            log.info("Søknad er trukket for sak $sakId, oppretter ikke revurdering ved mottak av uførevedtak")
        } else {
            log.info("Oppretter vurderingsbehov for mottatt uførevedtak for sak $sakId")
            val rettighetstypeTidslinje =
                rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(sisteYtelsesBehandling.id)
            val harRettPåAapEllerEnÅpenBehandling = rettighetstypeTidslinje.isNotEmpty() || sisteYtelsesBehandling.status().erÅpen()
            if (harRettPåAapEllerEnÅpenBehandling) {
                behandlingService.finnEllerOpprettBehandling(
                    sakId,
                    VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
                        vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)),
                        opprettet = mottattTidspunkt,
                        beskrivelse = uførevedtak.beskrivelseVurderingsbehov()
                    )
                )
            }
        }
        mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
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

    /**
     * Knytter klage og oppfølgingsbehandling direkte til behandlingen den opprettet, ikke via informasjonskrav.
     * Dette fordi det være flere åpne behandlinger av disse typene.
     * ManuellVurdering og NyÅrsakTilBehandling er knyttet eksplisitt til behandling og er ikke et informasjonskrav i flyten
     */
    private fun skalMarkereDokumentSomBehandlet(melding: Melding?): Boolean =
        melding is KabalHendelse || melding is Oppfølgingsoppgave || melding is ManuellRevurdering || melding is NyÅrsakTilBehandling || melding is TilbakekrevingHendelse

    fun oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
        sakId: SakId,
        behandlingsreferanse: BehandlingReferanse,
        innsendingType: InnsendingType,
        melding: NyÅrsakTilBehandlingV0,
        referanse: InnsendingReferanse
    ) {
        val behandling = behandlingRepository.hent(behandlingsreferanse)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(innsendingType, melding)

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


    private fun utledÅrsakTilOpprettelse(brevkategori: InnsendingType, melding: Melding?): ÅrsakTilOpprettelse {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> ÅrsakTilOpprettelse.SØKNAD
            InnsendingType.AKTIVITETSKORT -> ÅrsakTilOpprettelse.AKTIVITETSMELDING
            InnsendingType.MELDEKORT -> ÅrsakTilOpprettelse.MELDEKORT
            InnsendingType.LEGEERKLÆRING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.LEGEERKLÆRING_AVVIST -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.DIALOGMELDING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.KLAGE -> ÅrsakTilOpprettelse.KLAGE
            InnsendingType.ANNET_RELEVANT_DOKUMENT -> ÅrsakTilOpprettelse.ANNET_RELEVANT_DOKUMENT
            InnsendingType.MANUELL_REVURDERING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.KABAL_HENDELSE -> ÅrsakTilOpprettelse.SVAR_FRA_KLAGEINSTANS
            InnsendingType.OPPFØLGINGSOPPGAVE -> utledÅrsakTilOppfølgningsOppave(melding)
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> utledÅrsakEtterOmgjøringAvKlage(melding)
            InnsendingType.TILBAKEKREVING_HENDELSE -> ÅrsakTilOpprettelse.TILBAKEKREVING_HENDELSE
            InnsendingType.INSTITUSJONSOPPHOLD -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE -> ÅrsakTilOpprettelse.FAGSYSTEMINFO_BEHOV_HENDELSE
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE -> throw IllegalArgumentException("Sykepengevedtakhendelser skal trigge sjekk av informasjonskrav og ikke opprette en behandling direkte")
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
        }
    }

    private fun utledÅrsakTilOppfølgningsOppave(melding: Melding?): ÅrsakTilOpprettelse {
        require(melding is OppfølgingsoppgaveV0) { "Melding must be of type OppfølgingsoppgaveV0" }
        val kode = melding.opprinnelse?.avklaringsbehovKode
        val stegType = kode?.let { finnStegType(it) }
        return when (stegType) {
            StegType.SAMORDNING_GRADERING -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE_SAMORDNING_GRADERING
            else -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE
        }
    }


    private fun finnStegType(avklaringsTypeKode: String): StegType {
        return Definisjon.forKode(avklaringsTypeKode).løsesISteg
    }

    private fun utledÅrsakEtterOmgjøringAvKlage(melding: Melding?): ÅrsakTilOpprettelse = when (melding) {
        is OmgjøringKlageRevurdering -> when (melding.kilde) {
            Omgjøringskilde.KLAGEINSTANS -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS
            Omgjøringskilde.KELVIN -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_KLAGE
        }

        else -> error("Melding må være OmgjøringKlageRevurderingV0")
    }

    private fun utledVurderingsbehov(
        brevkategori: InnsendingType,
        melding: Melding?
    ): List<VurderingsbehovMedPeriode> {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            InnsendingType.MANUELL_REVURDERING -> when (melding) {
                is ManuellRevurderingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være ManuellRevurderingV0")
            }

            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> when (melding) {
                is OmgjøringKlageRevurdering -> melding.vurderingsbehov.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være OmgjøringKlageRevurderingV0")
            }

            InnsendingType.MELDEKORT ->
                listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                    )
                )

            InnsendingType.AKTIVITETSKORT -> listOf(
                VurderingsbehovMedPeriode(
                    type = Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                )
            )

            InnsendingType.LEGEERKLÆRING_AVVIST -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING))
            InnsendingType.LEGEERKLÆRING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING))
            InnsendingType.DIALOGMELDING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_DIALOGMELDING))
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokument -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være AnnetRelevantDokumentV0")
                }

            InnsendingType.KLAGE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE))
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING ->
                when (melding) {
                    is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være NyÅrsakTilBehandlingV0")
                }

            InnsendingType.KABAL_HENDELSE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_KABAL_HENDELSE))
            InnsendingType.OPPFØLGINGSOPPGAVE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.OPPFØLGINGSOPPGAVE))
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BRUKER))
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BARN))
            InnsendingType.INSTITUSJONSOPPHOLD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD))

            InnsendingType.TILBAKEKREVING_HENDELSE,
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE,
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> emptyList()
        }
    }

}
