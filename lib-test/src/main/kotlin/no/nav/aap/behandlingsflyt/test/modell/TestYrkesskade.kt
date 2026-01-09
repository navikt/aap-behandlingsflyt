package no.nav.aap.behandlingsflyt.test.modell

import java.time.LocalDate

data class TestYrkesskade (
    val skadedato: LocalDate? = LocalDate.now(),
    val saksreferanse: String = "1234"
)