package no.nav.aap.behandlingsflyt.test.modell

import java.time.LocalDate
import kotlin.math.floor

data class TestYrkesskade(
    val skadedato: LocalDate? = LocalDate.now(),
    val saksreferanse: String = "YRK" + "-" + floor(Math.random() * 100),
    val skadeart: String? = null,
    val diagnose: String? = null,
    val skadebeskrivelse: String? = null,
)