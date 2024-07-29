package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SoningsServiceTest {

    val connection = mockk<DBConnection>()
    val soningRepository = mockk<SoningRepository>()
    val institusjonRepository = mockk<InstitusjonsoppholdService>()
    val behandlingReferanseService = mockk<BehandlingReferanseService>()

    val soningsService = SoningsService(connection, soningRepository, institusjonRepository, behandlingReferanseService)

    @Test
    fun samleSoningsGrunnlag() {
        val fromDate = LocalDate.of(2022, 1, 1)
        val toDate = LocalDate.of(2023, 1, 1)

        every { behandlingReferanseService.behandling(any()) } returns mockk<Behandling>(relaxed = true)
        every { soningRepository.hentAktivSoningsvurderingHvisEksisterer(any()) } returns Soningsvurdering(
            emptyList(),
            soningUtenforFengsel = false,
            begrunnelseForSoningUtenforAnstalt = "YOLO"
        )
        every { institusjonRepository.hentHvisEksisterer(any()) } returns InstitusjonsoppholdGrunnlag(
            listOf(
                Segment(
                    Periode(fromDate, toDate),
                    Institusjon(
                        type = Institusjonstype.FO,
                        kategori = Oppholdstype.S,
                        orgnr = "12345"
                    )
                )
            )
        )

        val actual = soningsService.samleSoningsGrunnlag(BehandlingReferanse("324234"))

        assertThat(actual.soningsvurdering)
            .isEqualTo(SoningsvurderingDto(emptyList(), false, "YOLO"))

        assertThat(actual.soningsopphold).hasSize(1)
        assertThat(actual.soningsopphold[0]).isEqualTo(
            InstitusjonsoppholdDto(
                status = "Ukjent",
                institusjonstype = Institusjonstype.FO.beskrivelse,
                oppholdstype = Oppholdstype.S.beskrivelse,
                kildeinstitusjon = "12345",
                avsluttetDato = toDate,
                oppholdFra = fromDate
            )
        )
    }
}