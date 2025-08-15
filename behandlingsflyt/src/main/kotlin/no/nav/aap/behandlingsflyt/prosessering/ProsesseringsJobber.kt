package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.motor.JobbSpesifikasjon

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            StoppetHendelseJobbUtfører,
            GjenopptaBehandlingJobbUtfører,
            HendelseMottattHåndteringJobbUtfører,
            StatistikkJobbUtfører,
            DatadelingMeldePerioderOgSakStatusJobbUtfører,
            DatadelingBehandlingJobbUtfører,
            MeldeperiodeTilMeldekortBackendJobbUtfører,
            OpprettJobbForFastsattPeriodeJobbUtfører,
            OpprettJobbForFritakMeldepliktJobbUtfører,
            OpprettBehandlingFritakMeldepliktJobbUtfører,
            OpprettBehandlingFastsattPeriodePassertJobbUtfører,
            HentSamIdJobbUtfører,
            VarsleVedtakJobbUtfører
        )
    }
}