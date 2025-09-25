package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattDokumentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldekortJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.BehandlingFlytStoppetHendelseTilStatistikk
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakService: SakService,
    private val dokumentRepository: MottattDokumentRepository,
    private val pipRepository: PipRepository,
) : BehandlingHendelseService {
    constructor(repositoryProvider: RepositoryProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider),
        dokumentRepository = repositoryProvider.provide(),
        pipRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val sak = sakService.hent(behandling.sakId)
        val erPåVent = avklaringsbehovene.hentÅpneVentebehov().isNotEmpty()
        val vurderingsbehov = behandling.vurderingsbehov()
        val mottattDokumenter = hentMottattDokumenter(vurderingsbehov, behandling)

        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            aktivtSteg = behandling.aktivtSteg(),
            status = behandling.status(),
            årsakerTilBehandling = vurderingsbehov.map { it.type.name },
            vurderingsbehov = vurderingsbehov.map { it.type.name },
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "Ukjent årsak",
            avklaringsbehov = sortererteAvklaringsbehov(behandling, avklaringsbehovene.alle()),
            relevanteIdenterPåBehandling = pipRepository.finnIdenterPåBehandling(behandling.referanse).map { it.ident },
            erPåVent = erPåVent,
            mottattDokumenter = mottattDokumenter,
            reserverTil = hentReservertTil(behandling.id, sak),
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        log.info("Legger til flytjobber til statistikk og stoppethendelse for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = VarsleOppgaveOmHendelseJobbUtFører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(
                BehandlingFlytStoppetHendelseTilStatistikk(
                    personIdent = hendelse.personIdent,
                    saksnummer = hendelse.saksnummer,
                    referanse = hendelse.referanse,
                    behandlingType = hendelse.behandlingType,
                    status = hendelse.status,
                    avklaringsbehov = hendelse.avklaringsbehov,
                    opprettetTidspunkt = hendelse.opprettetTidspunkt,
                    hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
                    versjon = hendelse.versjon
                )
            )
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingMeldePerioderOgSakStatusJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )

        // Sender meldekort til API-intern
        flytJobbRepository.leggTil(DatadelingMeldekortJobbUtfører.nyJobb(sak.id, behandling.id))

        if (behandling.typeBehandling() in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)) {
            flytJobbRepository.leggTil(MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(sak.id, behandling.id))
        }
    }

    private fun hentReservertTil(behandlingId: BehandlingId, sak: Sak): String? {
        val oppfølgingsoppgavedokument =
            MottaDokumentService(dokumentRepository).hentOppfølgingsBehandlingDokument(behandlingId)

        val reserverTilBruker = finnReserverTilBrukerVedAvbruttRevurdering(behandlingId)

        return oppfølgingsoppgavedokument?.reserverTilBruker ?: reserverTilBruker
    }

    private fun finnReserverTilBrukerVedAvbruttRevurdering(behandlingId: BehandlingId): String? {
        val nyÅrsakTilBehandlingDokumenter = MottaDokumentService(dokumentRepository).hentMottattDokumenterAvType(
            behandlingId,
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING
        )

        val revurderingAvbruttDokument = nyÅrsakTilBehandlingDokumenter.find { dokument ->
            val melding = dokument.ustrukturerteData()?.let { DefaultJsonMapper.fromJson<Melding>(it) }
            melding is NyÅrsakTilBehandlingV0 &&
                    melding.årsakerTilBehandling.contains(
                        no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT
                    )
        }

        return (revurderingAvbruttDokument
            ?.ustrukturerteData()
            ?.let { DefaultJsonMapper.fromJson<Melding>(it) } as? NyÅrsakTilBehandlingV0)
            ?.reserverTilBruker
    }

    private fun hentMottattDokumenter(
        vurderingsbehov: List<VurderingsbehovMedPeriode>,
        behandling: Behandling
    ): List<MottattDokumentDto> {
        // Sender kun med dokumenter ved følgende behandlingsårsaker
        val gyldigeÅrsaker = listOf(
            Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_DIALOGMELDING
        )

        return if (vurderingsbehov.any { it.type in gyldigeÅrsaker }) {
            val gyldigeDokumenter = listOf(
                InnsendingType.LEGEERKLÆRING,
                InnsendingType.LEGEERKLÆRING_AVVIST,
                InnsendingType.DIALOGMELDING,
            )

            dokumentRepository
                .hentDokumenterAvType(behandling.id, gyldigeDokumenter)
                .map { it.tilMottattDokumentDto() }
                .toList()
        } else {
            emptyList()
        }
    }

    private fun MottattDokument.tilMottattDokumentDto(): MottattDokumentDto =
        MottattDokumentDto(
            type = this.type,
            referanse = this.referanse
        )
}
