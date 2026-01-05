package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `Stopper opp på institusjonssteget i førstegangsbehandling når innleggelsesdato er mer enn 2 mnd siden`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()
        person.institusjonsopphold = listOf(
            InstitusjonsoppholdJSON(
                startdato = LocalDate.now().minusMonths(3),
                forventetSluttdato = LocalDate.now().plusMonths(2),
                institusjonstype = "HS",
                institusjonsnavn = "institusjon",
                organisasjonsnummer = "2334",
                kategori = "H",
            )
        )

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
    }
}