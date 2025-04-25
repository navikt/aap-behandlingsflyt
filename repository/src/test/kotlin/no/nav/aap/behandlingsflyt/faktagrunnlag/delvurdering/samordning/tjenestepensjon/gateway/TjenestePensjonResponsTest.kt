package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TjenestePensjonResponsTest {

    @Test
    fun `test deserialisering`() {
        @Language("JSON")
        val dummyRespons = """
             {
               "fnr": "1337",
               "forhold": [
                 {
                   "samtykkeSimulering": false,
                   "kilde": "PP01",
                   "tpNr": "3010",
                   "ordning": {
                     "navn": "Statens Pensjon Kasse",
                     "tpNr": "3010",
                     "orgNr": "123445675645",
                     "tssId": "8000123455666",
                     "alias": []
                   },
                   "harSimulering": false,
                   "harUtlandsPensjon": false,
                   "datoSamtykkeGitt": null,
                   "ytelser": [
                     {
                       "datoInnmeldtYtelseFom": null,
                       "ytelseType": "ALDER",
                       "datoYtelseIverksattFom": "2020-01-01",
                       "datoYtelseIverksattTom": "2025-04-22",
                       "changeStamp": {
                         "createdBy": "DEFAULT_USER_ID",
                         "createdDate": "2025-04-22T10:40:06.269049700",
                         "updatedBy": "DEFAULT_USER_ID",
                         "updatedDate": "2025-04-22T10:40:06.269049700"
                       },
                       "ytelseId": 123
                     },
                     {
                       "datoInnmeldtYtelseFom": null,
                       "ytelseType": "BETINGET_TP",
                       "datoYtelseIverksattFom": "2020-01-01",
                       "datoYtelseIverksattTom": null,
                       "changeStamp": {
                         "createdBy": "DEFAULT_USER_ID",
                         "createdDate": "2025-04-22T10:40:06.269049700",
                         "updatedBy": "DEFAULT_USER_ID",
                         "updatedDate": "2025-04-22T10:40:06.269049700"
                       },
                       "ytelseId": 222
                     }
                   ],
                   "changeStampDate": {
                     "createdBy": "DEFAULT_USER_ID",
                     "createdDate": "2025-04-22T10:40:06.301355200",
                     "updatedBy": "DEFAULT_USER_ID",
                     "updatedDate": "2025-04-22T10:40:06.301355200"
                   }
                 }
               ]
             }
         """.trimIndent()

        assertThat(DefaultJsonMapper.fromJson<TjenestePensjonRespons>(dummyRespons)).usingRecursiveComparison()
            .isEqualTo(
                TjenestePensjonRespons(
                    fnr = "1337",
                    forhold = listOf(
                        SamhandlerForholdDto(
                            ordning = TpOrdning(
                                navn = "Statens Pensjon Kasse",
                                tpNr = "3010",
                                orgNr = "123445675645"
                            ),
                            ytelser = listOf(
                                SamhandlerYtelseDto(
                                    datoInnmeldtYtelseFom = null,
                                    ytelseType = YtelseTypeCode.ALDER,
                                    datoYtelseIverksattFom = LocalDate.of(2020, 1, 1),
                                    datoYtelseIverksattTom = LocalDate.of(2025, 4, 22),
                                    ytelseId = 123,
                                ), SamhandlerYtelseDto(
                                    datoInnmeldtYtelseFom = null,
                                    ytelseType = YtelseTypeCode.BETINGET_TP,
                                    datoYtelseIverksattFom = LocalDate.of(2020, 1, 1),
                                    datoYtelseIverksattTom = null,
                                    ytelseId = 222,
                                )
                            )
                        )
                    ),
                )
            )
    }

}