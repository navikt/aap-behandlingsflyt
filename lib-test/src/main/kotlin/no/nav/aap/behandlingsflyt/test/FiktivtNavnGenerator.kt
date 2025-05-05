package no.nav.aap.behandlingsflyt.test

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*


object FiktivtNavnGenerator {

    fun genererNavn(): PersonNavn {
        val kjønn = Kjønn.random()

        val fornavn = if (kjønn == Kjønn.KVINNE) {
            Navnelager.randomFornavnKvinne
        } else {
            Navnelager.randomFornavnMann
        }

        return PersonNavn(fornavn, Navnelager.randomEtternavn)
    }

    private object Navnelager {

        private val etternavn = loadNames("/etternavn.txt")
        private val fornavnKvinner = loadNames("/fornavn-kvinner.txt")
        private val fornavnMenn = loadNames("/fornavn-menn.txt")

        val randomFornavnMann: String
            get() = getRandom(fornavnMenn)

        val randomFornavnKvinne: String
            get() = getRandom(fornavnKvinner)

        val randomEtternavn: String
            get() = getRandom(etternavn)

        private val RANDOM = Random()

        @Synchronized
        private fun getRandom(liste: List<String>): String {
            return liste[RANDOM.nextInt(liste.size)]
        }

        private fun loadNames(resourceName: String): List<String> {
            try {
                // Try multiple class loaders to find the resource
                val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
                    ?: Navnelager::class.java.classLoader.getResourceAsStream(resourceName)
                    ?: FiktivtNavnGenerator::class.java.classLoader.getResourceAsStream(resourceName)
                    ?: FiktivtNavnGenerator::class.java.getResourceAsStream(resourceName)
                    ?: throw RuntimeException("Resource not found: $resourceName")

                BufferedReader(InputStreamReader(resourceStream)).use { br ->
                    val resultat: MutableList<String> =
                        ArrayList()
                    var strLine: String? = br.readLine()
                    while (strLine != null) {
                        resultat.add(strLine.uppercase(Locale.getDefault()))
                        strLine = br.readLine()
                    }
                    return resultat
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}
