package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import java.math.BigDecimal
import java.time.LocalDate

/**
 * @param gradering Gradering i prosent. (0-100)
 */
data class TilkjentYtelsePeriodeDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val dagsats: BigDecimal,
    val gradering: Int,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
    val utbetalingsdato: LocalDate,
    val redusertDagsats: Double,
    val arbeidGradering: Int?,
    val institusjonGradering: Int?,
    val samordningGradering: Int?,
    val samordningUføreGradering: Int?
)