package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.BehandlingFlytStoppetHendelseTilStatistikk
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime

class BehandlingHendelseServiceImplTest {
    val behandlingHendelseSerice = BehandlingHendelseServiceImpl(
        inMemoryRepositoryProvider,
        createGatewayProvider { register<AlleAvskruddUnleash>() })

    @ParameterizedTest
    @CsvSource(
        "ny,legacy,ny",
        "null,legacy,legacy",
        "null,null,null",
        nullValues = ["null"]
    )
    fun `statistikk bruker nytt felt opprettetAv for manuell revurdering med legacy fallback`(
        opprettetAv: String?,
        legacy: String?,
        forventet: String?
    ) {
        val sak = opprettInMemorySak(søknadsdato = 1 januar 2025)
        val behandling = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.id,
            VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE)
        )
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.REVURDERING_ID, "revurdering"),
                sakId = sak.id,
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.of(2026, 1, 1, 12, 0),
                type = InnsendingType.MANUELL_REVURDERING,
                kanal = Kanal.DIGITAL,
                strukturertDokument = StrukturertDokument(
                    ManuellRevurderingV0(emptyList(), "Revurdering", legacy)
                ),
                opprettetAv = opprettetAv,
            )
        )

        behandlingHendelseSerice.stoppet(
            behandling,
            Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        )

        val statistikk = InMemoryFlytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .single { it.type() == StatistikkJobbUtfører.type }
            .payload<BehandlingFlytStoppetHendelseTilStatistikk>()
        assertThat(statistikk.opprettetAv).isEqualTo(forventet)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Z123456"])
    fun `mottatt dokument i stopphendelsen bevarer nullable felt opprettetAv`(opprettetAv: String?) {
        val sak = opprettInMemorySak(søknadsdato = 1 januar 2025)
        val behandling = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.id,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_DIALOGMELDING)),
                ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            )
        )
        val dokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "dialogmelding"),
            sakId = sak.id,
            behandlingId = behandling.id,
            mottattTidspunkt = LocalDateTime.of(2026, 1, 1, 12, 0),
            type = InnsendingType.DIALOGMELDING,
            kanal = Kanal.DIGITAL,
            strukturertDokument = null,
            opprettetAv = opprettetAv,
        )
        InMemoryMottattDokumentRepository.lagre(dokument)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_SYKDOM, StegType.AVKLAR_SYKDOM, null, null, null)

        behandlingHendelseSerice.stoppet(behandling, avklaringsbehovene)

        val hendelse = InMemoryFlytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .single { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
            .payload<BehandlingFlytStoppetHendelse>()
        assertThat(hendelse.mottattDokumenter).hasSize(1)
        assertThat(hendelse.mottattDokumenter.single().opprettetAv).isEqualTo(opprettetAv)
        assertThat(hendelse.mottattDokumenter.single().referanse).isEqualTo(dokument.referanse)
    }

    @Test
    fun `Avklaringsbehov sorteres i rekkefølgen de kan løses i`() {
        val sak = opprettInMemorySak(søknadsdato = 1 januar 2025)
        val behandling = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.id,
            VurderingsbehovOgÅrsak(listOf(), ÅrsakTilOpprettelse.SØKNAD)
        )

        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        avklaringsbehovene.leggTil(
            Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP,
            StegType.VURDER_MEDLEMSKAP,
            null,
            null,
            null
        )
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_SYKDOM, StegType.AVKLAR_SYKDOM, null, null, null)
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_STUDENT_V2, StegType.AVKLAR_STUDENT_V2, null, null, null)

        behandlingHendelseSerice.stoppet(behandling, avklaringsbehovene)

        val hendelse = InMemoryFlytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .single { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
            .payload<BehandlingFlytStoppetHendelse>()
        assertThat(hendelse.avklaringsbehov.map { it.avklaringsbehovDefinisjon })
            .containsExactly(
                Definisjon.AVKLAR_SYKDOM,
                Definisjon.AVKLAR_STUDENT_V2,
                Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
            )
    }
}