package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPipRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class BehandlingHendelseServiceImplTest {
    private val person = Person(UUID.randomUUID(), listOf(genererIdent(1 januar 2020)))
    private val rettighetsperiode = Periode(1 januar 2025, 1 januar 2026)

    val behandlingHendelseSerice = BehandlingHendelseServiceImpl(
        flytJobbRepository = InMemoryFlytJobbRepository,
        brevbestillingRepository = InMemoryBrevbestillingRepository,
        sakService = SakService(InMemorySakRepository),
        dokumentRepository = InMemoryMottattDokumentRepository,
        pipRepository = InMemoryPipRepository,
    )

    @Test
    fun `Avklaringsbehov sorteres i rekkefølgen de kan løses i`() {
        val sak = InMemorySakRepository.finnEllerOpprett(person, rettighetsperiode)
        val behandling = InMemorySakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.id,
            VurderingsbehovOgÅrsak(listOf(), ÅrsakTilOpprettelse.SØKNAD)
        )

        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        avklaringsbehovene.leggTil(listOf(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP), StegType.VURDER_MEDLEMSKAP)
        avklaringsbehovene.leggTil(listOf(Definisjon.AVKLAR_STUDENT), StegType.AVKLAR_STUDENT)
        avklaringsbehovene.leggTil(listOf(Definisjon.AVKLAR_SYKDOM), StegType.AVKLAR_SYKDOM)

        behandlingHendelseSerice.stoppet(behandling, avklaringsbehovene)

        val hendelse = InMemoryFlytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .single { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
            .payload<BehandlingFlytStoppetHendelse>()
        assertThat(hendelse.avklaringsbehov.map { it.avklaringsbehovDefinisjon })
            .containsExactly(Definisjon.AVKLAR_STUDENT, Definisjon.AVKLAR_SYKDOM, Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
    }
}