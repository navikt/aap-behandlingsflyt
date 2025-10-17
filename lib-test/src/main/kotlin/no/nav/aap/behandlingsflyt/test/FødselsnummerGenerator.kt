package no.nav.aap.behandlingsflyt.test

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class FødselsnummerGenerator private constructor(
    private val kjonn: Kjønn,
    private val identType: IdentType,
    private val fodselsdato: LocalDate
) {
    // Opprettes på nytt for hver instans av klassen for å få forskjellige random seeds på dem
    private val random: Random = Random()

    private fun generate(): String {
        var day = String.format("%02d", fodselsdato.dayOfMonth)
        val month = String.format("%02d", fodselsdato.monthValue + NAV_SYNTETISK_IDENT_OFFSET_MND)
        val year = fodselsdato.year.toString().substring(2)

        val birthNumber: Int = if (this.kjonn === Kjønn.KVINNE) {
            100 + random.nextInt(900 / 2) * 2
        } else if (this.kjonn === Kjønn.MANN) {
            100 + random.nextInt(900 / 2) * 2 + 1
        } else {
            999
        }

        if (this.identType === IdentType.DNR) {
            day = (day.toInt() + DNR_OFFSETT_DAYS).toString()
        }

        val fullYear = fodselsdato.year
        if (betweenExclusive(fullYear, 1854, 1899)) {
            if (!betweenExclusive(birthNumber, 500, 749)) return generate()
        } else if (betweenExclusive(fullYear, 1900, 1999)) {
            if (!betweenExclusive(birthNumber, 0, 499)) return generate()
        } else if (betweenExclusive(fullYear, 1940, 1999)) {
            if (!betweenExclusive(birthNumber, 900, 999)) return generate()
        } else if (betweenExclusive(fullYear, 2000, 2039)) {
            if (!betweenExclusive(birthNumber, 500, 999)) return generate()
        } else {
            LOG.info("Kunne ikke identifisere fødselsnummerserie: $fullYear")
        }

        val withoutControlDigits = day + month + year + birthNumber

        val d1 = getDigit(withoutControlDigits, 0)
        val d2 = getDigit(withoutControlDigits, 1)
        val m1 = getDigit(withoutControlDigits, 2)
        val m2 = getDigit(withoutControlDigits, 3)
        val y1 = getDigit(withoutControlDigits, 4)
        val y2 = getDigit(withoutControlDigits, 5)
        val i1 = getDigit(withoutControlDigits, 6)
        val i2 = getDigit(withoutControlDigits, 7)
        val i3 = getDigit(withoutControlDigits, 8)

        var control1 = 11 - ((3 * d1 + 7 * d2 + 6 * m1 + 1 * m2 + 8 * y1 + 9 * y2 + 4 * i1 + 5 * i2 + 2 * i3) % 11)
        if (control1 == 11) {
            control1 = 0
        }
        var control2 =
            11 - ((5 * d1 + 4 * d2 + 3 * m1 + 2 * m2 + 7 * y1 + 6 * y2 + 5 * i1 + 4 * i2 + 3 * i3 + 2 * control1) % 11)
        if (control2 == 11) {
            control2 = 0
        }
        if (control1 == 10 || control2 == 10) {
            //Invalid number. Get a new one
            return generate()
        }
        return withoutControlDigits + control1 + control2
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val LOG: Logger = LoggerFactory.getLogger(javaClass)
        private const val NAV_SYNTETISK_IDENT_OFFSET_MND = 40
        private const val DNR_OFFSETT_DAYS = 40

        private fun getDigit(text: String, index: Int): Int {
            return text.substring(index, index + 1).toInt()
        }

        private fun betweenExclusive(x: Int, min: Int, max: Int): Boolean {
            return x > min && x <= max
        }
    }

    class Builder {
        private var kjonn: Kjønn = Kjønn.random()
        private var identType: IdentType = IdentType.FNR
        private var fodselsdato: LocalDate = LocalDate.now().minusYears(25).minusDays(Holder.hent())

        fun kjonn(k: Kjønn): Builder {
            this.kjonn = k
            return this
        }

        fun identType(i: IdentType): Builder {
            this.identType = i
            return this
        }

        fun fodselsdato(lt: LocalDate): Builder {
            this.fodselsdato = lt
            return this
        }

        fun buildAndGenerate(): String {
            return FødselsnummerGenerator(kjonn, identType, fodselsdato).generate()
        }
    }
}

enum class IdentType {
    DNR,
    FNR
}

enum class Kjønn {
    KVINNE,
    MANN;

    companion object {
        fun random(): Kjønn {
            return Kjønn.entries.random()
        }
    }
}
