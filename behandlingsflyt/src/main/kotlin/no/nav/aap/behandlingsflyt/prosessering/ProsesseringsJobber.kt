package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldekortJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.ResendStatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.SendFagsysteminfoBehovTilTilbakekrevingUtfører
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.JobbSpesifikasjon

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOfNotNull(
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
            OpprettJobbForGReguleringJobbUtfører,
            OpprettBehandlingFritakMeldepliktJobbUtfører,
            OpprettBehandlingFastsattPeriodePassertJobbUtfører,
            OpprettBehandlingUtvidVedtakslengdeJobbUtfører,
            OppdagEndretInformasjonskravJobbUtfører,
            HentSamIdJobbUtfører,
            VarsleVedtakJobbUtfører,
            GenererVilkårsvurderingOppsummeringJobbUtfører,
            IverksettUtbetalingJobbUtfører,
            KafkaFeilJobbUtfører,
            TriggBarnetilleggSatsJobbUtfører,
            OpprettJobbForTriggBarnetilleggSatsJobbUtfører,
            OpprettJobbForMigrereRettighetsperiodeJobbUtfører,
            OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører,
            OpprettBehandlingMigrereRettighetsperiodeJobbUtfører,
            OpprettBehandlingGReguleringJobbUtfører,
            SendFagsysteminfoBehovTilTilbakekrevingUtfører,
            SjekkInstitusjonsOppholdJobbUtfører,
            HåndterUbehandletDokumentJobbUtfører,
            DigitaliserteMeldekortTilMeldekortBackendJobbUtfører,
            HåndterUbehandledeMeldekortForSakJobbUtfører,
            if (!Miljø.erProd()) SendAutomatiskMeldekortJobbUtfører else null,
            if (!Miljø.erProd()) SendAutomatiskMeldekortEngangsJobbUtfører else null
        )
    }
}