package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.prosessering.statistikk.ResendStatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.SendFagsysteminfoBehovTilTilbakekrevingUtfører
import no.nav.aap.motor.JobbSpesifikasjon

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            VarsleOppgaveOmHendelseJobbUtFører,
            GjenopptaBehandlingJobbUtfører,
            HendelseMottattHåndteringJobbUtfører,
            StatistikkJobbUtfører,
            ResendStatistikkJobbUtfører,
            DatadelingMeldePerioderOgSakStatusJobbUtfører,
            DatadelingBehandlingJobbUtfører,
            DatadelingMeldekortJobbUtfører,
            MeldeperiodeTilMeldekortBackendJobbUtfører,
            OpprettJobbForFastsattPeriodeJobbUtfører,
            OpprettJobbForFritakMeldepliktJobbUtfører,
            OpprettJobbUtvidVedtakslengdeJobbUtfører,
            OpprettBehandlingFritakMeldepliktJobbUtfører,
            OpprettBehandlingFastsattPeriodePassertJobbUtfører,
            OpprettBehandlingUtvidVedtakslengdeJobbUtfører,
            OppdagEndretInformasjonskravJobbUtfører,
            HentSamIdJobbUtfører,
            VarsleVedtakJobbUtfører,
            IverksettUtbetalingJobbUtfører,
            KafkaFeilJobbUtfører,
            TriggBarnetilleggSatsJobbUtfører,
            OpprettJobbForTriggBarnetilleggSatsJobbUtfører,
            OpprettJobbForMigrereRettighetsperiodeJobbUtfører,
            OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører,
            OpprettBehandlingMigrereRettighetsperiodeJobbUtfører,
            SendFagsysteminfoBehovTilTilbakekrevingUtfører,
            SjekkInstitusjonsOppholdJobbUtfører,
            HåndterUbehandledeDokumenterJobbUtfører,
            HåndterUbehandletDokumentJobbUtfører,
            DigitaliserteMeldekortTilMeldekortBackendJobbUtfører,
            HåndterUbehandledeMeldekortForSakJobbUtfører
        )
    }
}