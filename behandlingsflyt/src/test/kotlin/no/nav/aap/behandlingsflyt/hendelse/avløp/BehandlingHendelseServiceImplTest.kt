package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingHendelseServiceImplTest {
    val behandlingHendelseSerice = BehandlingHendelseServiceImpl(
        inMemoryRepositoryProvider,
        createGatewayProvider { register<AlleAvskruddUnleash>() })

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
            null
        )
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_STUDENT, StegType.AVKLAR_STUDENT, null, null)
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_SYKDOM, StegType.AVKLAR_SYKDOM, null, null)

        behandlingHendelseSerice.stoppet(behandling, avklaringsbehovene)

        val hendelse = InMemoryFlytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
            .single { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
            .payload<BehandlingFlytStoppetHendelse>()
        assertThat(hendelse.avklaringsbehov.map { it.avklaringsbehovDefinisjon })
            .containsExactly(
                Definisjon.AVKLAR_STUDENT,
                Definisjon.AVKLAR_SYKDOM,
                Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
            )
    }
}