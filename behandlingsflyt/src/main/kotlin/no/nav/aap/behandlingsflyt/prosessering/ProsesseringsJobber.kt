package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.prosessering.statistikk.ResendStatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
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
            OpprettBehandlingFritakMeldepliktJobbUtfører,
            OpprettBehandlingFastsattPeriodePassertJobbUtfører,
            OppdagEndretInformasjonskravJobbUtfører,
            HentSamIdJobbUtfører,
            VarsleVedtakJobbUtfører,
            IverksettUtbetalingJobbUtfører,
            KafkaFeilJobbUtfører
        )
    }
}