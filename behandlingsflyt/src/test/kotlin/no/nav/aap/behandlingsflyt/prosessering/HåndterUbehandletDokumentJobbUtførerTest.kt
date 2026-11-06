package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.verdityper.dokument.Kanal
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class HåndterUbehandletDokumentJobbUtførerTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Z123456"])
    fun `utsatt håndtering bruker nytt felt opprettetAv, ikke det gamle nøstede`(opprettetAv: String?) {
        val sakId = SakId(1)
        val referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "meldekort")
        val melding = MeldekortV0(
            harDuArbeidet = null,
            timerArbeidPerPeriode = emptyList(),
            opprettetAv = "Z654321",
            begrunnelse = "Korrigert meldekort"
        )
        val tidspunkt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val dokument = MottattDokument(
            referanse = referanse,
            sakId = sakId,
            behandlingId = null,
            mottattTidspunkt = tidspunkt,
            type = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            strukturertDokument = StrukturertDokument(melding),
            opprettetAv = opprettetAv
        )
        val repository = mockk<MottattDokumentRepository> {
            every { hent(sakId, referanse) } returns dokument
        }
        val behandling = mockk<Behandling> {
            every { status() } returns Status.AVSLUTTET
        }
        val behandlingService = mockk<BehandlingService> {
            every { finnSisteGjeldendeEllerÅpneYtelsesbehandling(sakId) } returns behandling
        }
        val håndtering = mockk<HåndterMottattDokumentService>(relaxUnitFun = true)
        val utfører = HåndterUbehandletDokumentJobbUtfører(
            repository,
            behandlingService,
            mockk<TaSkriveLåsRepository>(relaxed = true),
            håndtering
        )

        utfører.utfør(HåndterUbehandletDokumentJobbUtfører.nyJobb(sakId, referanse))

        verify(exactly = 1) {
            håndtering.håndterMottatteDokumenter(
                sakId, referanse, tidspunkt, InnsendingType.MELDEKORT, melding, opprettetAv
            )
        }
    }
}
