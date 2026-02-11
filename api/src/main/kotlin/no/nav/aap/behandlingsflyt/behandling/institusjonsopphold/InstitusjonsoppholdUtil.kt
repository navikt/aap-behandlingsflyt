package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import java.time.LocalDate

fun lagOppholdId(institusjonNavn: String, fom: LocalDate): String =
    "${institusjonNavn}::${fom}"