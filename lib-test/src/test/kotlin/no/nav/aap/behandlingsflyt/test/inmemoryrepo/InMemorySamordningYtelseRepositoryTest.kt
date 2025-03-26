package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InMemorySamordningYtelseRepositoryTest {

    @BeforeEach
    fun setup() {
        // Clear the repository before each test
        val repo = InMemorySamordningYtelseRepository
        val field = repo.javaClass.getDeclaredField("ytelser")
        field.isAccessible = true
        val ytelser = field.get(repo)
        ytelser.javaClass.getMethod("clear").invoke(ytelser)
    }

    @Test
    fun `lagre og hente ut igjen`() {
        val repo = InMemorySamordningYtelseRepository
        repo.lagre(BehandlingId(123), emptyList())

        val res = repo.hentHvisEksisterer(BehandlingId(123))

        assertThat(res).isNotNull()
    }

    @Test
    fun `kopier fra en behandling til en annen`() {
        val repo = InMemorySamordningYtelseRepository
        val fraBehandlingId = BehandlingId(123)
        val tilBehandlingId = BehandlingId(456)

        // Create a test ytelse
        val ytelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = listOf(
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
        repo.lagre(fraBehandlingId, listOf(ytelse))

        // Copy from source to target
        repo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target has the same ytelse
        val fraYtelse = repo.hentHvisEksisterer(fraBehandlingId)
        val tilYtelse = repo.hentHvisEksisterer(tilBehandlingId)

        assertThat(tilYtelse).isNotNull()
        assertThat(tilYtelse?.ytelser).hasSize(1)
        assertThat(tilYtelse?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(tilYtelse?.ytelser?.get(0)?.kilde).isEqualTo("TEST")
        assertThat(tilYtelse?.ytelser?.get(0)?.saksRef).isEqualTo("REF123")
        assertThat(tilYtelse?.ytelser?.get(0)?.ytelsePerioder).hasSize(1)
        assertThat(tilYtelse?.ytelser?.get(0)?.ytelsePerioder?.get(0)?.kronesum).isEqualTo(1000)

        // Verify that the grunnlagId is the same (reference to the same data)
        assertThat(tilYtelse?.grunnlagId).isEqualTo(fraYtelse?.grunnlagId)
    }

    @Test
    fun `kopier fra en behandling som ikke eksisterer`() {
        val repo = InMemorySamordningYtelseRepository
        val fraBehandlingId = BehandlingId(123)
        val tilBehandlingId = BehandlingId(456)

        // Try to copy from a non-existent behandling
        repo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target doesn't have any ytelse
        val tilYtelse = repo.hentHvisEksisterer(tilBehandlingId)

        assertThat(tilYtelse).isNull()
    }

    @Test
    fun `hentEldsteGrunnlag returnerer det eldste grunnlaget`() {
        val repo = InMemorySamordningYtelseRepository
        val behandlingId = BehandlingId(123)

        // Create test ytelser with different attributes to identify them
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = listOf(
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
            ytelsePerioder = listOf(
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
            ytelsePerioder = listOf(
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

        // Save the ytelser with different timestamps
        // First, save ytelse2 (middle timestamp)
        val middleTime = Instant.parse("2023-02-01T12:00:00Z")
        val middleClock = Clock.fixed(middleTime, ZoneId.systemDefault())
        repo.setClock(middleClock)
        repo.lagre(behandlingId, listOf(ytelse2))

        // Then, save ytelse3 (newest timestamp)
        val newestTime = Instant.parse("2023-03-01T12:00:00Z")
        val newestClock = Clock.fixed(newestTime, ZoneId.systemDefault())
        repo.setClock(newestClock)
        repo.lagre(behandlingId, listOf(ytelse3))

        // Finally, save ytelse1 (oldest timestamp)
        val oldestTime = Instant.parse("2023-01-01T12:00:00Z")
        val oldestClock = Clock.fixed(oldestTime, ZoneId.systemDefault())
        repo.setClock(oldestClock)
        repo.lagre(behandlingId, listOf(ytelse1))

        // Reset the clock to system default
        repo.setClock(Clock.systemDefaultZone())

        // Get the oldest grunnlag
        val eldsteGrunnlag = repo.hentEldsteGrunnlag(behandlingId)

        // Verify that it's the one with ytelse1 (oldest timestamp)
        assertThat(eldsteGrunnlag).isNotNull()
        assertThat(eldsteGrunnlag?.ytelser).hasSize(1)
        assertThat(eldsteGrunnlag?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(eldsteGrunnlag?.ytelser?.get(0)?.kilde).isEqualTo("TEST1")
        assertThat(eldsteGrunnlag?.ytelser?.get(0)?.saksRef).isEqualTo("REF1")
        assertThat(eldsteGrunnlag?.ytelser?.get(0)?.ytelsePerioder).hasSize(1)
        assertThat(eldsteGrunnlag?.ytelser?.get(0)?.ytelsePerioder?.get(0)?.kronesum).isEqualTo(1000)

        // Also verify that hentHvisEksisterer returns the newest one (ytelse3)
        val nyesteGrunnlag = repo.hentHvisEksisterer(behandlingId)
        assertThat(nyesteGrunnlag).isNotNull()
        assertThat(nyesteGrunnlag?.ytelser).hasSize(1)
        assertThat(nyesteGrunnlag?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.PLEIEPENGER)
        assertThat(nyesteGrunnlag?.ytelser?.get(0)?.kilde).isEqualTo("TEST3")
        assertThat(nyesteGrunnlag?.ytelser?.get(0)?.saksRef).isEqualTo("REF3")
    }

    @Test
    fun `lagre med flere ytelser i en enkelt kall`() {
        val repo = InMemorySamordningYtelseRepository
        val behandlingId = BehandlingId(123)

        // Create multiple ytelser
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = listOf(
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
            ytelsePerioder = listOf(
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
        repo.lagre(behandlingId, listOf(ytelse1, ytelse2))

        // Verify that both ytelser are saved
        val grunnlag = repo.hentHvisEksisterer(behandlingId)
        assertThat(grunnlag).isNotNull()
        assertThat(grunnlag?.ytelser).hasSize(2)

        // Verify first ytelse
        val savedYtelse1 = grunnlag?.ytelser?.find { it.kilde == "TEST1" }
        assertThat(savedYtelse1).isNotNull()
        assertThat(savedYtelse1?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(savedYtelse1?.saksRef).isEqualTo("REF1")
        assertThat(savedYtelse1?.ytelsePerioder).hasSize(1)
        assertThat(savedYtelse1?.ytelsePerioder?.get(0)?.kronesum).isEqualTo(1000)

        // Verify second ytelse
        val savedYtelse2 = grunnlag?.ytelser?.find { it.kilde == "TEST2" }
        assertThat(savedYtelse2).isNotNull()
        assertThat(savedYtelse2?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(savedYtelse2?.saksRef).isEqualTo("REF2")
        assertThat(savedYtelse2?.ytelsePerioder).hasSize(1)
        assertThat(savedYtelse2?.ytelsePerioder?.get(0)?.kronesum).isEqualTo(2000)
    }

    @Test
    fun `lagre med flere kall for samme behandlingId`() {
        val repo = InMemorySamordningYtelseRepository
        val behandlingId = BehandlingId(123)

        // Create different ytelser for each call
        val ytelse1 = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = listOf(
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
            ytelsePerioder = listOf(
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
        repo.lagre(behandlingId, listOf(ytelse1))

        // Store the grunnlagId from the first call
        val firstGrunnlagId = repo.hentHvisEksisterer(behandlingId)?.grunnlagId
        assertThat(firstGrunnlagId).isNotNull()

        // Make a second call to lagre
        repo.lagre(behandlingId, listOf(ytelse2))

        // Verify that hentHvisEksisterer returns the newest grunnlag (ytelse2)
        val newestGrunnlag = repo.hentHvisEksisterer(behandlingId)
        assertThat(newestGrunnlag).isNotNull()
        assertThat(newestGrunnlag?.ytelser).hasSize(1)
        assertThat(newestGrunnlag?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(newestGrunnlag?.ytelser?.get(0)?.kilde).isEqualTo("TEST2")

        // Verify that the grunnlagId is different from the first call
        assertThat(newestGrunnlag?.grunnlagId).isNotEqualTo(firstGrunnlagId)

        // Verify that hentEldsteGrunnlag returns the oldest grunnlag (ytelse1)
        val oldestGrunnlag = repo.hentEldsteGrunnlag(behandlingId)
        assertThat(oldestGrunnlag).isNotNull()
        assertThat(oldestGrunnlag?.ytelser).hasSize(1)
        assertThat(oldestGrunnlag?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(oldestGrunnlag?.ytelser?.get(0)?.kilde).isEqualTo("TEST1")

        // Verify that the grunnlagId matches the one from the first call
        assertThat(oldestGrunnlag?.grunnlagId).isEqualTo(firstGrunnlagId)
    }

    @Test
    fun `hentHvisEksisterer og hentEldsteGrunnlag returnerer null når ingen data finnes`() {
        val repo = InMemorySamordningYtelseRepository
        val behandlingId = BehandlingId(999) // Using a behandlingId that doesn't exist

        // Verify that hentHvisEksisterer returns null
        val grunnlag = repo.hentHvisEksisterer(behandlingId)
        assertThat(grunnlag).isNull()

        // Verify that hentEldsteGrunnlag returns null
        val eldsteGrunnlag = repo.hentEldsteGrunnlag(behandlingId)
        assertThat(eldsteGrunnlag).isNull()
    }

    @Test
    fun `kopier når målbehandlingen allerede har data`() {
        val repo = InMemorySamordningYtelseRepository
        val fraBehandlingId = BehandlingId(123)
        val tilBehandlingId = BehandlingId(456)

        // Create ytelser for source and target
        val ytelseSource = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            ytelsePerioder = listOf(
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
            ytelsePerioder = listOf(
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
        repo.lagre(fraBehandlingId, listOf(ytelseSource))
        repo.lagre(tilBehandlingId, listOf(ytelseTarget))

        // Store the target's grunnlagId before copying
        val targetGrunnlagIdBeforeCopy = repo.hentHvisEksisterer(tilBehandlingId)?.grunnlagId
        assertThat(targetGrunnlagIdBeforeCopy).isNotNull()

        // Copy from source to target
        repo.kopier(fraBehandlingId, tilBehandlingId)

        // Verify that the target now has a new grunnlag
        val targetGrunnlagAfterCopy = repo.hentHvisEksisterer(tilBehandlingId)
        assertThat(targetGrunnlagAfterCopy).isNotNull()

        // Verify that the grunnlagId is different (a new grunnlag was created)
        assertThat(targetGrunnlagAfterCopy?.grunnlagId).isNotEqualTo(targetGrunnlagIdBeforeCopy)

        // Verify that the new grunnlag has the source's data
        assertThat(targetGrunnlagAfterCopy?.ytelser).hasSize(1)
        assertThat(targetGrunnlagAfterCopy?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.SYKEPENGER)
        assertThat(targetGrunnlagAfterCopy?.ytelser?.get(0)?.kilde).isEqualTo("SOURCE")
        assertThat(targetGrunnlagAfterCopy?.ytelser?.get(0)?.saksRef).isEqualTo("REF_SOURCE")

        // Verify that hentEldsteGrunnlag still returns the original target grunnlag
        val oldestGrunnlag = repo.hentEldsteGrunnlag(tilBehandlingId)
        assertThat(oldestGrunnlag).isNotNull()
        assertThat(oldestGrunnlag?.grunnlagId).isEqualTo(targetGrunnlagIdBeforeCopy)
        assertThat(oldestGrunnlag?.ytelser).hasSize(1)
        assertThat(oldestGrunnlag?.ytelser?.get(0)?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(oldestGrunnlag?.ytelser?.get(0)?.kilde).isEqualTo("TARGET")
    }
}
