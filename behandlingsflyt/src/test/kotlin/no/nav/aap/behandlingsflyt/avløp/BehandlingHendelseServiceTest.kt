package no.nav.aap.behandlingsflyt.avløp

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class BehandlingHendelseServiceTest {
    @Test
    fun `verifiser at FlytJobbRepository blir kalt med riktige argumenter`() {
        // SETUP
        val sakService = mockk<SakService>()
        val flytJobbRepository = mockk<FlytJobbRepository>()
        val brevbestillingRepository = mockk<BrevbestillingRepository>()
        val mottattDokumentRepository = mockk<MottattDokumentRepository>()

        every { flytJobbRepository.leggTil(any()) } returns Unit
        val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
        val behandlingHendelseService =
            BehandlingHendelseServiceImpl(flytJobbRepository, brevbestillingRepository, sakService, mottattDokumentRepository)

        val behandling = Behandling(
            BehandlingId(0), sakId = SakId(1), typeBehandling = TypeBehandling.Førstegangsbehandling, forrigeBehandlingId = null, versjon = 1
        )

        every { sakService.hent(SakId(1)) } returns Sak(
            id = SakId(1),
            saksnummer = Saksnummer("1"),
            person = Person(0, UUID.randomUUID(), listOf(Ident("123", true))),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val avklaringsbehovene = mockk<Avklaringsbehovene>()

        every { avklaringsbehovene.alle() } returns emptyList()
        every { avklaringsbehovene.hentÅpneVentebehov() } returns emptyList()


        // ACT

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        // VERIFY

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
    }
}