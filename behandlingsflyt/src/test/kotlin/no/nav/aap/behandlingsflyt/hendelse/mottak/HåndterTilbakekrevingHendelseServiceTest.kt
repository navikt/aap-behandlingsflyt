package no.nav.aap.behandlingsflyt.hendelse.mottak

import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingFagsysteminfoBehovHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FagsysteminfoBehovV0
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.FagsysteminfoSvarHendelse
import no.nav.aap.behandlingsflyt.prosessering.tilbakekreving.SendFagsysteminfoBehovTilTilbakekrevingUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.SetSystemProperty
import java.nio.ByteBuffer
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

class HåndterTilbakekrevingHendelseServiceTest {
    private val dato = LocalDate.of(2026, 6, 1)
    private val mottaDokumentService = MottaDokumentService(InMemoryMottattDokumentRepository)

    @Test
    @SetSystemProperty(key = "INTEGRASJON_SAKSBEHANDLING_URL", value = "https://kelvin.intern.dev.nav.no")
    fun `fagsysteminfo svar inkluderer saksbehandlingsurl i jobbens payload`() {
        val (sak, behandling) = opprettSakMedVedtak()
        val behov = lagFagsysteminfoBehov(sak, behandling)
        val referanse = registrerMottattBehov(sak, behov)

        opprettService().håndterMottattTilbakekrevingHendelse(sak.id, referanse, behov)

        val svar = hentFagsysteminfoSvar(sak)
        assertThat(svar.revurdering.url)
            .isEqualTo("https://kelvin.intern.dev.nav.no/saksbehandling/sak/${sak.saksnummer}")
    }

    private fun opprettSakMedVedtak(): Pair<Sak, Behandling> {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = dato)
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
        InMemoryVedtakRepository.lagre(behandling.id, dato.atStartOfDay(), dato)
        return sak to behandling
    }

    private fun lagFagsysteminfoBehov(sak: Sak, behandling: Behandling) =
        FagsysteminfoBehovV0(
            hendelsestype = "fagsysteminfo_behov",
            versjon = 1,
            eksternFagsakId = sak.saksnummer.toString(),
            kravgrunnlagReferanse = behandling.referanse.referanse.somKravgrunnlagReferanse(),
            hendelseOpprettet = dato.atStartOfDay(),
        )

    private fun registrerMottattBehov(sak: Sak, behov: FagsysteminfoBehovV0): InnsendingReferanse {
        val referanse = InnsendingReferanse(TilbakekrevingFagsysteminfoBehovHendelseId.ny("0-1"))
        mottaDokumentService.mottattDokument(
            referanse = referanse,
            sakId = sak.id,
            mottattTidspunkt = dato.atStartOfDay(),
            brevkategori = InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
            kanal = Kanal.DIGITAL,
            strukturertDokument = StrukturertDokument(behov),
            digitalisertAvPostmottak = false,
        )
        return referanse
    }

    private fun opprettService() =
        HåndterTilbakekrevingHendelseService(
            sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository),
            tilbakekrevingService = TilbakekrevingService(
                tilbakekrevingsbehandlingRepository = mockk(),
                flytJobbRepository = InMemoryFlytJobbRepository,
                oppgavestyringGateway = FakeOppgavestyringGateway(),
                pipService = PipService(inMemoryRepositoryProvider),
            ),
            mottaDokumentService = mottaDokumentService,
            behandlingRepository = InMemoryBehandlingRepository,
            vedtakRepository = InMemoryVedtakRepository,
        )

    private fun hentFagsysteminfoSvar(sak: Sak): FagsysteminfoSvarHendelse {
        val jobb = InMemoryFlytJobbRepository.hentJobberForSak(sak.id.toLong())
            .single { it.type() == SendFagsysteminfoBehovTilTilbakekrevingUtfører.type }
        return jobb.payload<FagsysteminfoSvarHendelse>()
    }

    private fun UUID.somKravgrunnlagReferanse(): String =
        Base64.getEncoder().encodeToString(
            ByteBuffer.allocate(16)
                .putLong(mostSignificantBits)
                .putLong(leastSignificantBits)
                .array()
        )
}
