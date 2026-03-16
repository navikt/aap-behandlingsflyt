package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.UnparsedStrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterDialogMeldingService
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterKlageService
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterSykepengevedtakService
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterTilbakekrevingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterUførevedtakService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private const val BREVKODE = "brevkode"
private const val KANAL = "kanal"
private const val MOTTATT_DOKUMENT_REFERANSE = "referanse"
private const val MOTTATT_TIDSPUNKT = "mottattTidspunkt"
private const val DIGITALISERT_AV_POSTMOTTAK = "digitalisertAvPostmottak"

class HendelseMottattHåndteringJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val håndterMottattDokumentService: HåndterMottattDokumentService,
    private val mottaDokumentService: MottaDokumentService,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val håndterKlageService:HåndterKlageService,
    private val håndterTilbakekrevingHendelse: HåndterTilbakekrevingHendelseService,
    private val håndterDialogMeldingService: HåndterDialogMeldingService,
    private val håndterSykepengevedtakService: HåndterSykepengevedtakService,
    private val håndterUførevedtakService: HåndterUførevedtakService,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val sakSkrivelås = låsRepository.låsSak(sakId)

        val innsendingType = InnsendingType.valueOf(input.parameter(BREVKODE))
        val kanal = Kanal.valueOf(input.parameter(KANAL))
        val payloadAsString = if (input.harPayload()) input.payload() else null
        val mottattTidspunkt = DefaultJsonMapper.fromJson<LocalDateTime>(input.parameter(MOTTATT_TIDSPUNKT))

        val parsedMelding = if (payloadAsString != null) {
            DefaultJsonMapper.fromJson<Melding>(payloadAsString)
        } else null

        val referanse = DefaultJsonMapper.fromJson<InnsendingReferanse>(input.parameter(MOTTATT_DOKUMENT_REFERANSE))

        val digitalisertAvPostmottak = input.optionalParameter(DIGITALISERT_AV_POSTMOTTAK)?.let {
            DefaultJsonMapper.fromJson<Boolean?>(it)
        }

        if (kjennerTilDokumentFraFør(referanse, innsendingType, sakId)) {
            log.warn("Allerede håndtert dokument med referanse {}", referanse)
            return
        }

        // DO WORK
        mottaDokumentService.mottattDokument(
            referanse = referanse,
            sakId = sakId,
            mottattTidspunkt = mottattTidspunkt,
            brevkategori = innsendingType,
            kanal = kanal,
            strukturertDokument = if (payloadAsString != null) UnparsedStrukturertDokument(payloadAsString) else null,
            digitalisertAvPostmottak = digitalisertAvPostmottak
        )

        håndterMottattDokument(innsendingType, parsedMelding, sakId, referanse, mottattTidspunkt)

        låsRepository.verifiserSkrivelås(sakSkrivelås)
    }

    private fun håndterMottattDokument(
        innsendingType: InnsendingType,
        parsedMelding: Melding?,
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime
    ) {
        when (innsendingType) {
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> {
                require(parsedMelding is NyÅrsakTilBehandlingV0) { "Melding må være av typen NyÅrsakTilBehandlingV0" }
                håndterMottattDokumentService.oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
                    sakId = sakId,
                    behandlingsreferanse = BehandlingReferanse(UUID.fromString(parsedMelding.behandlingReferanse)),
                    innsendingType = innsendingType,
                    melding = parsedMelding,
                    referanse = referanse
                )
            }

            InnsendingType.KLAGE -> {
                håndterKlageService.håndterMottatteKlage(
                    sakId = sakId,
                    referanse = referanse,
                    mottattTidspunkt = mottattTidspunkt,
                    brevkategori = innsendingType,
                    melding = parsedMelding as Klage,
                )
            }

            InnsendingType.TILBAKEKREVING_HENDELSE,
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE -> {
                håndterTilbakekrevingHendelse.håndterMottattTilbakekrevingHendelse(
                    sakId = sakId,
                    referanse = referanse,
                    melding = parsedMelding as TilbakekrevingHendelse,
                )
            }

            InnsendingType.DIALOGMELDING -> {
                håndterDialogMeldingService.håndterMottattDialogMelding(
                    sakId = sakId,
                    referanse = referanse,
                    brevkategori = innsendingType,
                    melding = parsedMelding,
                )
            }

            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> {
                håndterKlageService.håndterMottattOmgjøringEtterKlage(
                    sakId = sakId,
                    referanse = referanse,
                    mottattTidspunkt = mottattTidspunkt,
                    brevkategori = innsendingType,
                    melding = parsedMelding as OmgjøringKlageRevurdering,
                )
            }

            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE -> {
                håndterSykepengevedtakService.håndterMottattSykepengevedtakHendelse(
                    sakId = sakId,
                    referanse = referanse,
                )
            }

            InnsendingType.UFØRE_VEDTAK_HENDELSE -> {
                håndterUførevedtakService.håndterMottattUførevedtakHendelse(
                    sakId = sakId,
                    referanse = referanse,
                    uførevedtak = parsedMelding as UførevedtakV0,
                    mottattTidspunkt = mottattTidspunkt,
                )
            }

            else -> {
                håndterMottattDokumentService.håndterMottatteDokumenter(
                    sakId,
                    referanse,
                    mottattTidspunkt,
                    innsendingType,
                    parsedMelding
                )
            }
        }
    }

    private fun kjennerTilDokumentFraFør(
        innsendingReferanse: InnsendingReferanse,
        innsendingType: InnsendingType,
        sakId: SakId,
    ): Boolean {
        val innsendinger = mottattDokumentRepository.hentDokumenterAvType(sakId, innsendingType)

        return innsendinger.any { dokument -> dokument.referanse == innsendingReferanse }
    }

    companion object : ProvidersJobbSpesifikasjon {
        fun nyJobb(
            sakId: SakId,
            dokumentReferanse: InnsendingReferanse,
            brevkategori: InnsendingType,
            kanal: Kanal,
            melding: Melding? = null,
            mottattTidspunkt: LocalDateTime,
            digitalisertAvPostmottak: Boolean? = null
        ) = JobbInput(HendelseMottattHåndteringJobbUtfører)
            .apply {
                forSak(sakId.toLong())
                medCallId()
                medParameter(MOTTATT_DOKUMENT_REFERANSE, DefaultJsonMapper.toJson(dokumentReferanse))
                medParameter(BREVKODE, brevkategori.name)
                medParameter(KANAL, kanal.name)
                medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(mottattTidspunkt))
                medParameter(DIGITALISERT_AV_POSTMOTTAK, DefaultJsonMapper.toJson(digitalisertAvPostmottak ?: false))
                medPayload(melding)
            }

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return HendelseMottattHåndteringJobbUtfører(
                låsRepository = repositoryProvider.provide(),
                håndterMottattDokumentService = HåndterMottattDokumentService(repositoryProvider, gatewayProvider),
                mottaDokumentService = MottaDokumentService(repositoryProvider),
                mottattDokumentRepository = repositoryProvider.provide(),
                håndterKlageService = HåndterKlageService(repositoryProvider, gatewayProvider),
                håndterTilbakekrevingHendelse = HåndterTilbakekrevingHendelseService(repositoryProvider, gatewayProvider),
                håndterDialogMeldingService = HåndterDialogMeldingService(repositoryProvider, gatewayProvider),
                håndterSykepengevedtakService = HåndterSykepengevedtakService(repositoryProvider, gatewayProvider),
                håndterUførevedtakService = HåndterUførevedtakService(repositoryProvider, gatewayProvider),
            )
        }

        override val type = "hendelse.håndterer"
        override val navn = "Hendelses håndterer"
        override val beskrivelse =
            "Håndterer hendelser på en gitt sak. Knytter de nye opplysningene til rett behandling og oppretter behandling hvis det er behov for det."
    }
}