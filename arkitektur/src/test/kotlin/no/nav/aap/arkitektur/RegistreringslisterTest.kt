package no.nav.aap.arkitektur

import no.nav.aap.arkitektur.Kildekode.fil
import no.nav.aap.arkitektur.Kildekode.grensesnitt
import no.nav.aap.arkitektur.Kildekode.klasser
import no.nav.aap.arkitektur.Kildekode.objekter
import no.nav.aap.arkitektur.Kildekode.produksjon
import no.nav.aap.arkitektur.Kildekode.registrerte
import org.junit.jupiter.api.Test

/**
 * De håndvedlikeholdte registreringslistene kompilerer uten endring og feiler først i runtime
 * ("Repository av typen … er ikke registrert") eller ikke i det hele tatt (jobben kjører aldri).
 * Historikken har flere rene "glemte å registrere"-commits, f.eks. f78841eed, 1050bb4fe, 19d9c173e, 66c8bd9ab, bcc37c5c0.
 */
class RegistreringslisterTest {

    private val repositoryImpl = klasser.filter {
        it.name.endsWith("RepositoryImpl") && it.erKonkret() && it.moduleName != "lib-test"
    }
    private val steg = klasser.filter { it.arver("BehandlingSteg") && it.erKonkret() }
    private val informasjonskrav = klasser.filter { it.arver("Informasjonskrav") && it.erKonkret() }
    private val jobber = klasser.filter { it.arver("JobbUtfører") && it.erKonkret() }
    /** Klasser og objekter (mange gatewayer er `object`), med direkte foreldre. */
    private data class Type(val navn: String, val modul: String, val foreldre: List<String>, val erGateway: Boolean)

    private val typer = klasser.filter { it.erKonkret() }.map { Type(it.name, it.moduleName, it.foreldrenavn(), it.arver("Gateway")) } +
        objekter.filterNot { it.hasCompanionModifier }.map { Type(it.name, it.moduleName, it.foreldrenavn(), it.arver("Gateway")) }
    private val gatewayer = typer.filter { it.erGateway && it.modul != "lib-test" }
    private val flytdefinisjoner by lazy {
        produksjon.files.filter { it.path.contains("/forretningsflyt/behandlingstyper/") }.joinToString("\n") { it.text }
    }

    @Test
    fun `alle RepositoryImpl er registrert i PostgresRepositoryRegistry`() {
        val registrert = registrerte(fil("repository/PostgresRepositoryRegistry.kt"))
        sjekkRegel(
            sjekket = repositoryImpl,
            funn = repositoryImpl.filterNot { it.name in registrert }.map { it.name },
            slikRetterDu = "Legg til .register<Navn>() i repository/.../repository/PostgresRepositoryRegistry.kt.",
        )
    }

    @Test
    fun `alle jobbutførere er registrert i ProsesseringsJobber`() {
        val liste = fil("prosessering/ProsesseringsJobber.kt").text.substringAfter("listOf")
        sjekkRegel(
            sjekket = jobber,
            funn = jobber.filterNot { helOrd(it.name).containsMatchIn(liste) }.map { it.name },
            slikRetterDu = "Legg til jobben i listen i behandlingsflyt/.../prosessering/ProsesseringsJobber.kt, " +
                "ellers kjenner ikke Motor den.",
        )
    }

    @Test
    fun `alle gateway-implementasjoner er registrert i DefaultGatewayProvider`() {
        val registrert = registrerte(fil("integrasjon/DefaultGatewayProvider.kt"))
        sjekkRegel(
            sjekket = gatewayer,
            funn = gatewayer.filterNot { it.navn in registrert }.map { it.navn },
            slikRetterDu = "Legg til register<Navn>() i repository/.../integrasjon/DefaultGatewayProvider.kt.",
        )
    }

    @Test
    fun `alle gatewayer i DefaultGatewayProvider har en variant i testGatewayProvider`() {
        val iTest = registrerte(fil("test/TestGatewayProvider.kt"))
        // Unleash registreres via parameteren unleashGateway (AlleAvskruddUnleash som standard).
        val dekketITest = typer.filter { it.navn in iTest }.flatMap { it.foreldre }.toSet() + "UnleashGateway" - "Gateway"
        val iProd = registrerte(fil("integrasjon/DefaultGatewayProvider.kt"))
        val prodTyper = typer.filter { it.navn in iProd }
        sjekkRegel(
            sjekket = prodTyper,
            funn = prodTyper.filterNot { t -> t.navn in iTest || t.foreldre.any { it in dekketITest } }.map { it.navn },
            slikRetterDu = "Registrer gatewayen (eller en fake med samme grensesnitt) i " +
                "lib-test/.../test/TestGatewayProvider.kt, ellers feiler TestApp og flyttester i runtime.",
        )
    }

    @Test
    fun `alle feature toggles er i bruk og finnes i FakeUnleash`() {
        val enumKlasse = klasser.single { it.name == "BehandlingsflytFeature" }
        val toggles = enumKlasse.enumConstants.map { it.name }
        val fake = fil("test/FakeUnleash.kt").text
        val bruk = produksjon.files.filter { it.path != enumKlasse.path && it.moduleName != "lib-test" }
            .joinToString("\n") { it.text }
        sjekkRegel(
            sjekket = toggles,
            funn = toggles.filterNot { helOrd("BehandlingsflytFeature.$it").containsMatchIn(bruk) }
                .map { "$it er ikke i bruk" } +
                toggles.filterNot { helOrd("BehandlingsflytFeature.$it").containsMatchIn(fake) }
                    .map { "$it mangler i FakeUnleash" },
            kjenteBrudd = setOf(
                // Bruken er fjernet, enum-verdien ble stående (be8a194b1, 390138929, 72554c9ad). Slett dem.
                "FjerneAvventUtbetalingForSamordningArbeidsgiver er ikke i bruk",
                "MigrerStansOgOpphor er ikke i bruk",
                // Ny på main etter 23.10.2026 (e09d06809), funnet ved rebase 8.11.2026.
                "BackfillRedusertDagsats mangler i FakeUnleash",
            ),
            slikRetterDu = "Ny toggle: legg den til i lib-test/.../test/FakeUnleash.kt. " +
                "Fjernet toggle: slett også verdien i BehandlingsflytFeature og i FakeUnleash.",
        )
    }

    @Test
    fun `alle steg brukes i minst én flytdefinisjon`() = sjekkRegel(
        sjekket = steg,
        funn = steg.filterNot { helOrd(it.name).containsMatchIn(flytdefinisjoner) }.map { it.name },
        slikRetterDu = "Legg steget inn i riktig flyt i forretningsflyt/behandlingstyper/, eller slett det. " +
            "Rekkefølgen er semantikk: spør før du flytter eller setter inn steg.",
    )

    @Test
    fun `alle informasjonskrav brukes i minst én flytdefinisjon`() = sjekkRegel(
        sjekket = informasjonskrav,
        funn = informasjonskrav.filterNot { helOrd(it.name).containsMatchIn(flytdefinisjoner) }.map { it.name },
        slikRetterDu = "Legg informasjonskravet inn i kravlisten til steget som eier det i " +
            "forretningsflyt/behandlingstyper/, ellers hentes det aldri.",
    )

    @Test
    fun `alle repository-grensesnitt i domenet har en InMemory-variant`() {
        val registrert = registrerte(fil("inmemoryrepo/InMemoryRepositoryRegistry.kt"))
        val dekket = (klasser.filter { it.name in registrert }.flatMap { it.foreldrenavn() } +
            objekter.filter { it.name in registrert }.flatMap { it.foreldrenavn() }).toSet()
        val repositoryGrensesnitt = grensesnitt.filter { it.moduleName == "behandlingsflyt" && it.arver("Repository") }
        sjekkRegel(
            sjekket = repositoryGrensesnitt,
            funn = repositoryGrensesnitt.filterNot { it.name in dekket }.map { it.name },
            kjenteBrudd = setOf(
                // Mangler per 16.10.2026. Fyll på etter hvert som steg-testharnesset trenger dem.
                "AndreYtelserOppgittISøknadRepository",
                "DagpengerRepository",
                "ForutgåendeMedlemskapRepository",
                "FullmektigRepository",
                "GjenopptakRepository",
                "InntektsbortfallRepository",
                "KandidatForPåminnelseRepository",
                "LovvalgMedlemskapRepository",
                "MedlemskapUnntakForutgåendeRepository",
                "MedlemskapUnntakRepository",
                "OppfølgingsBehandlingRepository",
                "OppholdskravGrunnlagRepository",
                "PersonopplysningForutgåendeRepository",
                "Reduksjon11_9Repository",
                "SamIdRepository",
                "SvarFraAndreinstansRepository",
                "TaSkriveLåsRepository",
                "TestAutomatiskMeldekortSakRepository",
                "TilbakekrevingRepository",
                "TiltakspengerRepository",
                "VurderRettighetsperiodeRepository",
                // Nye på main etter 23.10.2026 (20ac05a11, a5b645be6), funnet ved rebase 8.11.2026.
                "OppgittUtenlandsopplysningerRepository",
                "SporingRepository",
            ),
            slikRetterDu = "Lag InMemory<Navn> i lib-test/.../inmemoryrepo/ og registrer den i InMemoryRepositoryRegistry.kt, " +
                "slik at domenetester kan kjøre uten Postgres.",
        )
    }
}
