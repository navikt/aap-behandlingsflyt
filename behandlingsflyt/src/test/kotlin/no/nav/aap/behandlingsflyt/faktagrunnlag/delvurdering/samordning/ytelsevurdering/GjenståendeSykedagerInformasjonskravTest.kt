package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryGjenståendeSykedagerRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class GjenståendeSykedagerInformasjonskravTest {
    private val sykedager = GjenståendeSykedager(GjenståendeSykedagerGrunnlag.MAKSDATO_I_INFOTRYGD, 42, 1 januar 2026)

    @Test
    fun `er relevant for vurderingstype førstegangsbehandling`() {
        val kontekst = kontekst(VurderingType.FØRSTEGANGSBEHANDLING)

        assertThat(informasjonskrav().erRelevant(kontekst, StegType.START_BEHANDLING, null)).isTrue()
    }

    @Test
    fun `er ikke relevant for vurderingstype revurdering`() {
        val kontekst = kontekst(VurderingType.REVURDERING)

        assertThat(informasjonskrav().erRelevant(kontekst, StegType.START_BEHANDLING, null)).isFalse()
    }

    @Test
    fun `kjører kun én gang per behandling`() {
        val kontekst = kontekst(VurderingType.FØRSTEGANGSBEHANDLING)
        val oppdatert = InformasjonskravOppdatert(
            behandlingId = kontekst.behandlingId,
            navn = InformasjonskravNavn.GJENSTAAENDE_SYKEDAGER,
            oppdatert = Instant.now(),
            forrigeInput = null,
            rettighetsperiode = null,
        )

        assertThat(informasjonskrav().erRelevant(kontekst, StegType.START_BEHANDLING, oppdatert)).isFalse()
    }

    @Test
    fun `lagrer hentet data og rapporterer ikke endret ved gjentatt oppdatering`() {
        val kontekst = kontekst(VurderingType.FØRSTEGANGSBEHANDLING)
        val informasjonskrav = informasjonskrav()

        assertThat(klargjørHentOgOppdater(informasjonskrav, kontekst)).isEqualTo(Informasjonskrav.Endret.ENDRET)
        assertThat(InMemoryGjenståendeSykedagerRepository.hentHvisEksisterer(kontekst.behandlingId))
            .isEqualTo(sykedager)

        assertThat(klargjørHentOgOppdater(informasjonskrav, kontekst)).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `feil fra sykepengetjenesten stopper ikke flyten og lagrer ingenting`() {
        val kontekst = kontekst(VurderingType.FØRSTEGANGSBEHANDLING)
        val informasjonskrav = informasjonskrav(gateway = FakeSykepengerGateway { error("Tjenesten er nede") })

        assertThat(klargjørHentOgOppdater(informasjonskrav, kontekst)).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
        assertThat(InMemoryGjenståendeSykedagerRepository.hentHvisEksisterer(kontekst.behandlingId)).isNull()
    }

    private fun klargjørHentOgOppdater(
        informasjonskrav: GjenståendeSykedagerInformasjonskrav,
        kontekst: FlytKontekstMedPerioder,
    ): Informasjonskrav.Endret {
        val input = informasjonskrav.klargjør(kontekst)
        return informasjonskrav.oppdater(input, informasjonskrav.hentData(input), kontekst)
    }

    private fun informasjonskrav(
        gateway: SykepengerGateway = FakeSykepengerGateway { sykedager },
    ) = GjenståendeSykedagerInformasjonskrav(
        sakRepository = InMemorySakRepository,
        gjenståendeSykedagerRepository = InMemoryGjenståendeSykedagerRepository,
        sykepengerGateway = gateway,
        unleashGateway = FakeUnleashBaseWithDefaultDisabled(listOf(BehandlingsflytFeature.MaksdatoSykepenger)),
    )

    private fun kontekst(vurderingType: VurderingType): FlytKontekstMedPerioder {
        val person = InMemoryPersonRepository.finnEllerOpprett(listOf(Ident("12345678901")))
        val sak = InMemorySakRepository.finnEllerOpprett(person, LocalDate.now())
        return flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = BehandlingId(System.nanoTime())
            this.vurderingType = vurderingType
            this.rettighetsperiode = sak.rettighetsperiode
        }
    }

    private class FakeSykepengerGateway(
        private val gjenståendeSykedager: () -> GjenståendeSykedager,
    ) : SykepengerGateway {
        override fun hentGjenståendeSykedager(fødselsnummer: String) = gjenståendeSykedager()

        override fun hentYtelseSykepenger(
            personidentifikatorer: Set<String>,
            fom: LocalDate,
            tom: LocalDate,
        ): List<UtbetaltePerioder> = emptyList()
    }
}
