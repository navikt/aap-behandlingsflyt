package no.nav.aap.behandlingsflyt.test.modell

data class MockUnleashFeatures(
    val version: Int = 2,
    val features: List<MockUnleashFeature>,
)

data class MockUnleashFeature(
    val name: String,
    val enabled: Boolean,
//    val type: String = "release",
//    val stale: Boolean = false,
//    val impressionData: Boolean = false,
//    val project: String = "project",
//    val description: String = "Beskrivelse",
)
