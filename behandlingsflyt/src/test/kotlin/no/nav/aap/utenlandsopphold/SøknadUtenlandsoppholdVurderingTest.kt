package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class SøknadUtenlandsoppholdVurderingTest {
    private val søknadsperiode = Periode(1 januar 2025, 31 januar 2025)

    private fun valider(
        land: String = "SE",
        resultat: SøknadUtenlandsoppholdResultat = SøknadUtenlandsoppholdResultat.INNVILGET,
        godkjentPeriode: Periode? = søknadsperiode,
        antallGodkjenteDager: Int? = null,
        begrunnelse: String = "Begrunnelse",
    ) = SøknadUtenlandsoppholdVurdering.valider(
        søknadsperiode, land, resultat, godkjentPeriode, antallGodkjenteDager, begrunnelse
    )

    @Test
    fun `innvilgelse med godkjent periode er gyldig`() {
        assertThat(valider()).isNull()
    }

    @Test
    fun `innvilgelse med kun antall dager er gyldig`() {
        assertThat(valider(godkjentPeriode = null, antallGodkjenteDager = 14)).isNull()
    }

    @Test
    fun `delvis innvilgelse med periode innenfor søknadsperioden er gyldig`() {
        val godkjent = Periode(1 januar 2025, 10 januar 2025)
        assertThat(valider(resultat = SøknadUtenlandsoppholdResultat.DELVIS_INNVILGET, godkjentPeriode = godkjent))
            .isNull()
    }

    @Test
    fun `avslag uten godkjent periode og dager er gyldig`() {
        assertThat(valider(resultat = SøknadUtenlandsoppholdResultat.AVSLÅTT, godkjentPeriode = null)).isNull()
    }

    @Test
    fun `innvilgelse uten periode eller dager er ugyldig`() {
        assertThat(valider(godkjentPeriode = null, antallGodkjenteDager = null)).isNotNull()
    }

    @Test
    fun `avslag med godkjent periode eller dager er ugyldig`() {
        assertThat(valider(resultat = SøknadUtenlandsoppholdResultat.AVSLÅTT)).isNotNull()
        assertThat(
            valider(resultat = SøknadUtenlandsoppholdResultat.AVSLÅTT, godkjentPeriode = null, antallGodkjenteDager = 3)
        ).isNotNull()
    }

    @Test
    fun `godkjent periode utenfor søknadsperioden er ugyldig`() {
        assertThat(valider(godkjentPeriode = Periode(1 januar 2025, 28 februar 2025))).isNotNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "S", "SWE", "se", "XX", "Sverige"])
    fun `ugyldig landkode avvises`(land: String) {
        assertThat(valider(land = land)).isNotNull()
    }

    @Test
    fun `blank begrunnelse avvises`() {
        assertThat(valider(begrunnelse = "  ")).isNotNull()
    }

    @Test
    fun `antall dager må være positivt`() {
        assertThat(valider(godkjentPeriode = null, antallGodkjenteDager = 0)).isNotNull()
    }

    @Test
    fun `domeneobjektet kan ikke opprettes med ugyldige verdier`() {
        assertThatThrownBy {
            SøknadUtenlandsoppholdVurdering(
                søknadsperiode = søknadsperiode,
                land = "SE",
                resultat = SøknadUtenlandsoppholdResultat.INNVILGET,
                godkjentPeriode = null,
                antallGodkjenteDager = null,
                begrunnelse = "Begrunnelse",
                vurdertAv = "Z000000",
                vurdertTidspunkt = LocalDateTime.now(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
