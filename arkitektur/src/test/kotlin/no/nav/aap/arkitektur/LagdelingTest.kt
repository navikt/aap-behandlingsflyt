package no.nav.aap.arkitektur

import no.nav.aap.arkitektur.Kildekode.klasser
import no.nav.aap.arkitektur.Kildekode.produksjon
import org.junit.jupiter.api.Test

/**
 * Lagdelingen kompilatoren ikke kan håndheve. Domenet ser `dbconnect` uansett, fordi kelvin `motor` eksporterer det
 * med `api(...)`. Kjente brudd er det som fantes 16.10.2026; listene skal bare krympe.
 */
class LagdelingTest {

    private val domenefiler = produksjon.files.filter { it.moduleName == "behandlingsflyt" }
    private val apifiler = produksjon.files.filter { it.moduleName == "api" }

    @Test
    fun `domenet importerer ikke dbconnect, Kafka eller Ktor`() {
        val forbudt = mapOf(
            "no.nav.aap.komponenter.dbconnect" to "dbconnect",
            "org.apache.kafka" to "kafka",
            "io.ktor" to "ktor",
        )
        sjekkRegel(
            sjekket = domenefiler,
            funn = domenefiler.flatMap { f ->
                f.imports.mapNotNull { import -> forbudt.entries.find { import.name.startsWith(it.key) }?.value }
                    .distinct().map { "${f.name}: $it" }
            },
            kjenteBrudd = setOf(
                "BehandlingsflytLogInfoProvider: dbconnect",
                "KafkaProducerConfig: kafka",
                "KafkaProdusent: kafka",
                "LazyFaktaGrunnlag: dbconnect",
                "LazyStrukturertDokument: dbconnect",
            ),
            slikRetterDu = "SQL hører hjemme i repository-modulen (*RepositoryImpl), HTTP og Kafka i repository/api/app. " +
                "Domenet kjenner bare grensesnittene.",
        )
    }

    @Test
    fun `RepositoryImpl ligger i repository-modulen`() {
        val repositoryImpl = klasser.filter { it.name.endsWith("RepositoryImpl") && it.erKonkret() && it.moduleName != "lib-test" }
        sjekkRegel(
            sjekket = repositoryImpl,
            funn = repositoryImpl.filter { it.moduleName != "repository" }.map { "${it.moduleName}: ${it.name}" },
            slikRetterDu = "Legg nye repositories i repository/, med grensesnittet i domenet ved siden av modellen.",
        )
    }

    @Test
    fun `api importerer ikke Impl-klasser`() = sjekkRegel(
        sjekket = apifiler,
        funn = apifiler.flatMap { f ->
            f.imports.map { it.name.substringAfterLast(".") }.filter { it.endsWith("Impl") }.map { "${f.name}: $it" }
        },
        kjenteBrudd = setOf(
            "DriftApi: TidligereVurderingerImpl",
            "ForeslåVedtakApi: TidligereVurderingerImpl",
            "TidligereVurderingerApi: TidligereVurderingerImpl",
        ),
        slikRetterDu = "Bruk grensesnittet og hent implementasjonen via repositoryProvider/gatewayProvider.",
    )

    @Test
    fun `api konstruerer ikke steg direkte`() = sjekkRegel(
        sjekket = apifiler,
        funn = apifiler.flatMap { f ->
            Regex("""(${IDENTIFIKATOR}Steg)\.konstruer\(""").findAll(f.text).map { "${f.name}: ${it.groupValues[1]}" }.toList()
        },
        kjenteBrudd = setOf(
            "BistandsgrunnlagApi: VurderBistandsbehovSteg",
            "ForutgåendeMedlemskapGrunnlagApi: VurderForutgåendeMedlemskapSteg",
            "LovvalgMedlemskapGrunnlagApi: VurderLovvalgSteg",
            "OppholdskravGrunnlagApi: VurderOppholdskravSteg",
            "overgangArbeidGrunnlagApi: OvergangArbeidSteg",
        ),
        slikRetterDu = "Flytt logikken API-et trenger ut av steget (f.eks. til en service) og bruk den fra begge steder.",
    )

    @Test
    fun `konstruktører tar ikke inn RepositoryImpl- eller GatewayImpl-typer`() {
        val kandidater = klasser.filter { it.moduleName !in setOf("repository", "lib-test") }
        sjekkRegel(
            sjekket = kandidater,
            funn = kandidater.flatMap { k ->
                (listOfNotNull(k.primaryConstructor) + k.secondaryConstructors).flatMap { c ->
                    c.parameters.map { it.type.name.removeSuffix("?") }
                        .filter { it.endsWith("RepositoryImpl") || it.endsWith("GatewayImpl") }
                        .map { "${k.name}($it)" }
                }
            },
            slikRetterDu = "Bruk marker-grensesnittet som parametertype. repositoryProvider.provide() slår opp på " +
                "den deklarerte typen og feiler i runtime for *Impl.",
        )
    }
}
