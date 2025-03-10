package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.motor.Jobb

object ProsesseringsJobber {

    fun alle(): List<Jobb> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            ProsesserBehandlingJobbUtfører,
            StoppetHendelseJobbUtfører,
            GjenopptaBehandlingJobbUtfører,
            HendelseMottattHåndteringJobbUtfører,
            StatistikkJobbUtfører,
            DatadelingMeldePerioderJobbUtfører,
            DatadelingSakStatusJobbUtfører,
            DatadelingBehandlingJobbUtfører
        )
    }
}