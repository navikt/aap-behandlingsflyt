package no.nav.aap.behandlingsflyt.utils

@RequiresOptIn(
    message = """
        Denne metoden muterer et database-objekt. Det er i utgangspunktet kun én service
        som skal kalle på denne metoden, og services skal da som samtidig muterer domeneobjektet.
        
        Det kan gi riktig resultat å mutere direkte i et API-endepunkt, men er det verdt risikoen?
        For en annen utvikler flytter kanskje koden uta fra API-et og inn i en metode i en service, og
        en tredje utvikler begynner så å bruke metoden fra services.
        """,
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
annotation class FarligMutering