package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.søknad.AarsakTilTrekkSoknad
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelseNavn
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelseStatus
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

@Fakes
class BehandlingVisningDatadelingServiceTest {
    @Test
    fun `søknad, under behandling`() {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)

        val service = BehandlingVisningDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledBehandlingerVisning(behandling.sakId)
        val behandlingVisning = res.first()

        assertThat(res.size).isEqualTo(1)
        assertThat(behandlingVisning.avsluttet).isEqualTo(false)
        assertThat(behandlingVisning.hendelser.find { it.navn == BehandlingHendelseNavn.SØKNAD_MOTTATT }?.status).isEqualTo(
            BehandlingHendelseStatus.FULLFØRT
        )
        assertThat(behandlingVisning.hendelser.find { it.navn == BehandlingHendelseNavn.UNDER_BEHANDLING }?.status).isEqualTo(
            BehandlingHendelseStatus.AKTIV
        )

    }

    @Test
    fun `trukket søknad`() {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)

        InMemoryTrukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandling.id,
            TrukketSøknadVurdering(
                journalpostId = JournalpostId("123"),
                begrunnelse = "Bruker ønsker ikke lenger søknaden behandlet",
                skalTrekkes = true,
                vurdertAv = Bruker("Z999999"),
                vurdert = Instant.now(),
                aarsak = AarsakTilTrekkSoknad.BRUKER_ONSKER_IKKE_SOKE_LENGER,
            )
        )

        val service = BehandlingVisningDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledBehandlingerVisning(behandling.sakId)
        val behandlingVisning = res.first()

        assertThat(res.size).isEqualTo(1)
        assertThat(behandlingVisning.avsluttet).isEqualTo(false)
        assertThat(behandlingVisning.hendelser.find { it.navn == BehandlingHendelseNavn.SØKNAD_MOTTATT }?.status).isEqualTo(
            BehandlingHendelseStatus.FULLFØRT
        )
        assertThat(behandlingVisning.hendelser.find { it.navn == BehandlingHendelseNavn.SØKNAD_TRUKKET }?.status).isEqualTo(
            BehandlingHendelseStatus.FULLFØRT
        )
    }

    @Test
    fun `ventepunkt er aktiv`() {
        val behandling = opprettBehandlingMedVentepunkter()

        val behandlingVisning = BehandlingVisningDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())
            .utledBehandlingerVisning(behandling.sakId)
            .single()

        assertThat(behandlingVisning.hendelser.map { it.navn }).containsExactlyInAnyOrder(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
            BehandlingHendelseNavn.UNDER_BEHANDLING,
        )
        assertThat(behandlingVisning.hendelser.single { it.navn == BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER }.status)
            .isEqualTo(BehandlingHendelseStatus.AKTIV)
    }

    @Test
    fun `ventepunkt er fullført`() {
        val behandling = opprettBehandlingMedVentepunkter()
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).løsAvklaringsbehov(
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            begrunnelse = "Mottatt legeerklæring",
            endretAv = Bruker("Z999999"),
        )

        val behandlingVisning = BehandlingVisningDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())
            .utledBehandlingerVisning(behandling.sakId)
            .single()

        assertThat(behandlingVisning.hendelser.map { it.navn }).containsExactlyInAnyOrder(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
            BehandlingHendelseNavn.UNDER_BEHANDLING,
        )
        assertThat(behandlingVisning.hendelser.single { it.navn == BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER }.status)
            .isEqualTo(BehandlingHendelseStatus.FULLFØRT)
    }

    @Test
    fun `avbrutt ventepunkt gir ingen hendelse`() {
        val behandling = opprettBehandlingMedVentepunkter()
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .avbryt(Definisjon.MANUELT_SATT_PÅ_VENT)

        val behandlingVisning = BehandlingVisningDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())
            .utledBehandlingerVisning(behandling.sakId)
            .single()

        assertThat(behandlingVisning.hendelser.map { it.navn }).containsExactlyInAnyOrder(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.UNDER_BEHANDLING,
        )
    }

    /**
     * Oppretter en behandling med to ventepunkter: ett med en venteårsak som gir hendelse,
     * og ett med en venteårsak som ikke gir hendelse.
     */
    private fun opprettBehandlingMedVentepunkter(): Behandling {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        avklaringsbehovene.opprett(
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            funnetISteg = StegType.START_BEHANDLING,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
            perioderKanVurderes = null,
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
            gradBehov = null,
        )
        avklaringsbehovene.opprett(
            definisjon = Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING,
            funnetISteg = StegType.VURDER_LOVVALG,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
            perioderKanVurderes = null,
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING,
            gradBehov = null,
        )

        return behandling
    }
}