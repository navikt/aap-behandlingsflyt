package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonopplysningInformasjonskravTest {

    @Test
    fun `skal trigge revurdering når grunnlag endrer seg på andre felt enn statsborgerskap og adresse`() {
        val eksisterende = personopplysning(
            status = PersonStatus.bosatt,
            statsborgerskap = listOf(Statsborgerskap("NOR")),
            utenlandsAddresser = emptyList()
        )
        val oppdatert = eksisterende.copy(status = PersonStatus.utflyttet)

        assertTrue(harRelevantEndringForLovvalgOgMedlemskap(eksisterende, oppdatert))
    }

    @Test
    fun `skal trigge revurdering når bruker mister EØS eller norsk statsborgerskap`() {
        val eksisterende = personopplysning(
            statsborgerskap = listOf(Statsborgerskap("NOR"))
        )
        val oppdatert = eksisterende.copy(
            statsborgerskap = listOf(Statsborgerskap("XUK"))
        )

        assertTrue(harRelevantEndringForLovvalgOgMedlemskap(eksisterende, oppdatert))
    }

    @Test
    fun `skal trigge revurdering når bruker får ny utenlandsk adresse i nytt land`() {
        val eksisterende = personopplysning(
            utenlandsAddresser = listOf(utenlandsAdresse("SWE"))
        )
        val oppdatert = eksisterende.copy(
            utenlandsAddresser = listOf(
                utenlandsAdresse("SWE"),
                utenlandsAdresse("DNK"),
            )
        )

        assertTrue(harRelevantEndringForLovvalgOgMedlemskap(eksisterende, oppdatert))
    }

    @Test
    fun `skal ikke trigge revurdering når ny utenlandsk adresse er i samme land`() {
        val eksisterende = personopplysning(
            utenlandsAddresser = listOf(utenlandsAdresse("SWE", "Gamlebyen 1"))
        )
        val oppdatert = eksisterende.copy(
            utenlandsAddresser = listOf(
                utenlandsAdresse("SWE", "Gamlebyen 1"),
                utenlandsAdresse("SWE", "Nybyen 2"),
            )
        )

        assertFalse(harRelevantEndringForLovvalgOgMedlemskap(eksisterende, oppdatert))
    }

    private fun personopplysning(
        status: PersonStatus = PersonStatus.bosatt,
        statsborgerskap: List<Statsborgerskap> = listOf(Statsborgerskap("NOR")),
        utenlandsAddresser: List<UtenlandsAdresse> = emptyList()
    ) = Personopplysning(
        fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)),
        dødsdato = null,
        status = status,
        statsborgerskap = statsborgerskap,
        utenlandsAddresser = utenlandsAddresser
    )

    private fun utenlandsAdresse(
        landkode: String,
        adresseNavn: String = "Adresse"
    ) = UtenlandsAdresse(
        gyldigFraOgMed = LocalDate.now().minusDays(1),
        gyldigTilOgMed = null,
        adresseNavn = adresseNavn,
        postkode = "1234",
        bySted = "Sted",
        landkode = landkode,
        adresseType = AdresseType.BOSTEDS_ADRESSE
    )
}
