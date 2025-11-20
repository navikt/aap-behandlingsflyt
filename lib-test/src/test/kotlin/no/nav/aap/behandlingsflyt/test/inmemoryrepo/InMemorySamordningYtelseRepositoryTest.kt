package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.function.Consumer

class InMemorySamordningYtelseRepositoryTest {

    @Test
    fun `lagre og hente ut igjen`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val behandling = opprettBehandling(nySak())
        samordningYtelseRepo.lagre(behandling.id, emptySet())

        val res = samordningYtelseRepo.hentHvisEksisterer(behandling.id)

        assertThat(res).isNotNull()
    }

    @Test
    fun `kopier fra en behandling til en annen`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling1 = opprettBehandling(sak)
        val behandling2 = opprettBehandling(sak)
        val fraBehandlingId = behandling1.id
        val tilBehandlingId = behandling2.id

        // Create a test ytelse
        val ytelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 1000
                )
            ),
            kilde = "TEST",
            saksRef = "REF123"
        )

        // Save the ytelse to the source behandling
        samordningYtelseRepo.lagre(fraBehandlingId, setOf(ytelse))

        // Copy from source to target
        samordningYtelseRepo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target has the same ytelse
        val fraYtelse = samordningYtelseRepo.hentHvisEksisterer(fraBehandlingId)
        val tilYtelse = samordningYtelseRepo.hentHvisEksisterer(tilBehandlingId)

        assertThat(tilYtelse).isNotNull()
        assertThat(tilYtelse?.ytelser).hasSize(1)
        assertThat(tilYtelse?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(tilYtelse?.ytelser?.first()?.kilde).isEqualTo("TEST")
        assertThat(tilYtelse?.ytelser?.first()?.saksRef).isEqualTo("REF123")
        assertThat(tilYtelse?.ytelser?.first()?.ytelsePerioder).hasSize(1)
        assertThat(tilYtelse?.ytelser?.first()?.ytelsePerioder?.first()?.kronesum).isEqualTo(1000)

        // Verify that the grunnlagId is the same (reference to the same data)
        assertThat(tilYtelse?.grunnlagId).isEqualTo(fraYtelse?.grunnlagId)
    }

    @Test
    fun `kopier fra en behandling som ikke eksisterer`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling1 = opprettBehandling(sak)
        val behandling2 = opprettBehandling(sak)
        val fraBehandlingId = behandling1.id
        val tilBehandlingId = behandling2.id

        // Try to copy from a non-existent behandling
        samordningYtelseRepo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target doesn't have any ytelse
        val tilYtelse = samordningYtelseRepo.hentHvisEksisterer(tilBehandlingId)

        assertThat(tilYtelse).isNull()
    }

    @Test
    fun `hentEldsteGrunnlag returnerer det eldste grunnlaget`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling = opprettBehandling(sak)
        val behandlingId = behandling.id

        // Create test ytelser with different attributes to identify them
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 1000
                )
            ),
            kilde = "TEST1",
            saksRef = "REF1"
        )

        val ytelse2 = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 2, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 2000
                )
            ),
            kilde = "TEST2",
            saksRef = "REF2"
        )

        val ytelse3 = SamordningYtelse(
            ytelseType = Ytelse.PLEIEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 3, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 3000
                )
            ),
            kilde = "TEST3",
            saksRef = "REF3"
        )
        samordningYtelseRepo.lagre(behandlingId, setOf(ytelse1))
        samordningYtelseRepo.lagre(behandlingId, setOf(ytelse2))

        samordningYtelseRepo.lagre(behandlingId, setOf(ytelse3))

        // Get the oldest grunnlag
        val eldsteGrunnlag = samordningYtelseRepo.hentEldsteGrunnlag(behandlingId)

        // Verify that it's the one with ytelse1 (oldest timestamp)
        assertThat(eldsteGrunnlag).isNotNull()
        assertThat(eldsteGrunnlag?.ytelser).hasSize(1)
        assertThat(eldsteGrunnlag?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(eldsteGrunnlag?.ytelser?.first()?.kilde).isEqualTo("TEST1")
        assertThat(eldsteGrunnlag?.ytelser?.first()?.saksRef).isEqualTo("REF1")
        assertThat(eldsteGrunnlag?.ytelser?.first()?.ytelsePerioder).hasSize(1)
        assertThat(eldsteGrunnlag?.ytelser?.first()?.ytelsePerioder?.first()?.kronesum).isEqualTo(1000)

        // Also verify that hentHvisEksisterer returns the newest one (ytelse3)
        val nyesteGrunnlag = samordningYtelseRepo.hentHvisEksisterer(behandlingId)
        assertThat(nyesteGrunnlag).isNotNull()
        assertThat(nyesteGrunnlag?.ytelser).hasSize(1)
        assertThat(nyesteGrunnlag?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.PLEIEPENGER)
        assertThat(nyesteGrunnlag?.ytelser?.first()?.kilde).isEqualTo("TEST3")
        assertThat(nyesteGrunnlag?.ytelser?.first()?.saksRef).isEqualTo("REF3")
    }

    @Test
    fun `lagre med flere ytelser i en enkelt kall`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling = opprettBehandling(sak)

        // Create multiple ytelser
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 1000
                )
            ),
            kilde = "TEST1",
            saksRef = "REF1"
        )

        val ytelse2 = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 2, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 2000
                )
            ),
            kilde = "TEST2",
            saksRef = "REF2"
        )

        // Save multiple ytelser in a single call
        samordningYtelseRepo.lagre(behandling.id, setOf(ytelse1, ytelse2))

        // Verify that both ytelser are saved
        val grunnlag = samordningYtelseRepo.hentHvisEksisterer(behandling.id)
        assertThat(grunnlag).isNotNull()
        assertThat(grunnlag?.ytelser).hasSize(2)

        // Verify first ytelse
        val savedYtelse1 = grunnlag?.ytelser?.find { it.kilde == "TEST1" }
        assertThat(savedYtelse1).isNotNull()
        assertThat(savedYtelse1?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(savedYtelse1?.saksRef).isEqualTo("REF1")
        assertThat(savedYtelse1?.ytelsePerioder).hasSize(1)
        assertThat(savedYtelse1?.ytelsePerioder?.first()?.kronesum).isEqualTo(1000)

        // Verify second ytelse
        val savedYtelse2 = grunnlag?.ytelser?.find { it.kilde == "TEST2" }
        assertThat(savedYtelse2).isNotNull()
        assertThat(savedYtelse2?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(savedYtelse2?.saksRef).isEqualTo("REF2")
        assertThat(savedYtelse2?.ytelsePerioder).hasSize(1)
        assertThat(savedYtelse2?.ytelsePerioder?.first()?.kronesum).isEqualTo(2000)
    }

    @Test
    fun `lagre med flere kall for samme behandlingId`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling = opprettBehandling(sak)
        val behandlingId = behandling.id

        // Create different ytelser for each call
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 1000
                )
            ),
            kilde = "TEST1",
            saksRef = "REF1"
        )

        val ytelse2 = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 2, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 2000
                )
            ),
            kilde = "TEST2",
            saksRef = "REF2"
        )

        // Save ytelser in separate calls
        samordningYtelseRepo.lagre(behandlingId, setOf(ytelse1))

        // Store the grunnlagId from the first call
        val firstGrunnlagId = samordningYtelseRepo.hentHvisEksisterer(behandlingId)?.grunnlagId
        assertThat(firstGrunnlagId).isNotNull()

        // Make a second call to lagre
        samordningYtelseRepo.lagre(behandlingId, setOf(ytelse2))

        // Verify that hentHvisEksisterer returns the newest grunnlag (ytelse2)
        val newestGrunnlag = samordningYtelseRepo.hentHvisEksisterer(behandlingId)
        assertThat(newestGrunnlag).isNotNull()
        assertThat(newestGrunnlag?.ytelser).hasSize(1)
        assertThat(newestGrunnlag?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(newestGrunnlag?.ytelser?.first()?.kilde).isEqualTo("TEST2")

        // Verify that the grunnlagId is different from the first call
        assertThat(newestGrunnlag?.grunnlagId).isNotEqualTo(firstGrunnlagId)

        // Verify that hentEldsteGrunnlag returns the oldest grunnlag (ytelse1)
        val oldestGrunnlag = samordningYtelseRepo.hentEldsteGrunnlag(behandlingId)
        assertThat(oldestGrunnlag).isNotNull()
        assertThat(oldestGrunnlag?.ytelser).hasSize(1)
        assertThat(oldestGrunnlag?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(oldestGrunnlag?.ytelser?.first()?.kilde).isEqualTo("TEST1")

        // Verify that the grunnlagId matches the one from the first call
        assertThat(oldestGrunnlag?.grunnlagId).isEqualTo(firstGrunnlagId)
    }

    @Test
    fun `hentHvisEksisterer og hentEldsteGrunnlag returnerer null når ingen data finnes`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val behandlingId = BehandlingId(999) // Using a behandlingId that doesn't exist

        // Verify that hentHvisEksisterer returns null
        val grunnlag = samordningYtelseRepo.hentHvisEksisterer(behandlingId)
        assertThat(grunnlag).isNull()

        // Verify that hentEldsteGrunnlag returns null
        val eldsteGrunnlag = samordningYtelseRepo.hentEldsteGrunnlag(behandlingId)
        assertThat(eldsteGrunnlag).isNull()
    }

    @Test
    fun `kopier når målbehandlingen allerede har data`() {
        val samordningYtelseRepo = InMemorySamordningYtelseRepository
        val sak = nySak()
        val behandling1 = opprettBehandling(sak)
        val fraBehandlingId = behandling1.id

        // Create ytelser for source and target
        val ytelseSource = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 1000
                )
            ),
            kilde = "SOURCE",
            saksRef = "REF_SOURCE"
        )

        val ytelseTarget = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        fom = LocalDate.of(2023, 2, 1),
                        tom = LocalDate.of(2023, 12, 31)
                    ),
                    gradering = null,
                    kronesum = 2000
                )
            ),
            kilde = "TARGET",
            saksRef = "REF_TARGET"
        )

        // First, save data to both source and target
        samordningYtelseRepo.lagre(fraBehandlingId, setOf(ytelseSource))
        samordningYtelseRepo.lagre(fraBehandlingId, setOf(ytelseTarget))

        // Opprett ny behandling på samme sak
        val behandling2 = opprettBehandling(sak)
        val tilBehandlingId = behandling2.id

        // Copy from source to target
        samordningYtelseRepo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target now has a new grunnlag
        val targetGrunnlagAfterCopy = samordningYtelseRepo.hentHvisEksisterer(tilBehandlingId)
        assertThat(targetGrunnlagAfterCopy).isNotNull()

        // Verify that the new grunnlag has the source's data
        assertThat(targetGrunnlagAfterCopy?.ytelser).satisfiesExactlyInAnyOrder(
            Consumer { ytelse ->
                assertThat(ytelse.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
                assertThat(ytelse.kilde).isEqualTo("TARGET")
                assertThat(ytelse.saksRef).isEqualTo("REF_TARGET")
                assertThat(ytelse.ytelsePerioder).satisfiesExactlyInAnyOrder(
                    Consumer { ytelsePeriode ->
                        assertThat(ytelsePeriode.periode.fom).isEqualTo(LocalDate.of(2023, 2, 1))
                        assertThat(ytelsePeriode.periode.tom).isEqualTo(LocalDate.of(2023, 12, 31))
                        assertThat(ytelsePeriode.gradering).isNull()
                        assertThat(ytelsePeriode.kronesum).isEqualTo(2000)
                    }
                )
            },
        )
        assertThat(targetGrunnlagAfterCopy?.ytelser).hasSize(1)

        // Verify that hentEldsteGrunnlag still returns the original target grunnlag
        val oldestGrunnlag = samordningYtelseRepo.hentEldsteGrunnlag(tilBehandlingId)
        assertThat(oldestGrunnlag).isNotNull()

        assertThat(oldestGrunnlag?.ytelser).hasSize(1)
        assertThat(oldestGrunnlag?.ytelser?.first()?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(oldestGrunnlag?.ytelser?.first()?.kilde).isEqualTo("SOURCE")
    }


    private fun nySak(): Sak {
        return PersonOgSakService(
            object : IdentGateway {
                override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
                    return listOf(ident)
                }
            },
            InMemoryPersonRepository,
            InMemorySakRepository
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }

    private fun opprettBehandling(sak: Sak): Behandling {
        return InMemorySakOgBehandlingService
            .finnEllerOpprettOrdinærBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )
    }
}