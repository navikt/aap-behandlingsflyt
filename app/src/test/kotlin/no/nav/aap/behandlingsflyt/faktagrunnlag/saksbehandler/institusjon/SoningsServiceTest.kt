package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SoningsServiceTest {

    val connection = mockk<DBConnection>()
    val soningRepository = mockk<SoningRepository>()
    val institusjonRepository = mockk<InstitusjonsoppholdRepository>()
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
            begrunnelse = "YOLO",
            periode = Periode(fromDate, toDate)
        )
        every { institusjonRepository.hentHvisEksisterer(any()) } returns InstitusjonsoppholdGrunnlag(
            listOf(
                Segment(
                    Periode(fromDate, toDate),
                    Institusjon(
                        type = Institusjonstype.FO,
                        kategori = Oppholdstype.S,
                        orgnr = "12345",
                        navn = "Anstalten"
                    )
                )
            )
        )

        val actual = soningsService.samleSoningsGrunnlag(BehandlingReferanse())

        assertThat(actual.soningsvurdering)
            .isEqualTo(SoningsvurderingDto(emptyList(), false, "YOLO", null, null))

        assertThat(actual.soningsopphold).hasSize(1)
        assertThat(actual.soningsopphold[0]).isEqualTo(
            InstitusjonsoppholdDto(
                status = "AVSLUTTET",
                institusjonstype = Institusjonstype.FO.beskrivelse,
                oppholdstype = Oppholdstype.S.beskrivelse,
                kildeinstitusjon = "Anstalten",
                avsluttetDato = toDate,
                oppholdFra = fromDate
            )
        )
    }
}