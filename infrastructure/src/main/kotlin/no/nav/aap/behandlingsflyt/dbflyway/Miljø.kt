package no.nav.aap.behandlingsflyt.dbflyway

object Miljø {
    fun er(): MiljøKode {
        val cluster = System.getenv("NAIS_CLUSTER_NAME")
        if (cluster?.substring(0, cluster.indexOf("-"))?.equals("DEV", ignoreCase = true) == true) {
            return MiljøKode.DEV
        } else if (cluster == "LOCAL") {
            return MiljøKode.LOKALT
        }
        return MiljøKode.PROD
    }
}