// top-level build gradle

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Lås postgres-versjonen til en "known good" versjon for nå
            force("org.postgresql:postgresql:42.7.6")
        }
    }
}
