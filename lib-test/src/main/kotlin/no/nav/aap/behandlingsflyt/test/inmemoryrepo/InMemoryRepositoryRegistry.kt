package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.komponenter.repository.RepositoryRegistry


val inMemoryRepositoryRegistry = RepositoryRegistry().apply {
    register<InMemoryAktivitetsplikt11_7Repository>()
    register<InMemoryAvbrytRevurderingRepository>()
    register<InMemoryAvklaringsbehovRepository>()
    register<InMemoryBarnRepository>()
    register<InMemoryBehandlingRepository>()
    register<InMemoryBeregningsgrunnlagRepository>()
    register<InMemoryBrevbestillingRepository>()
    register<InMemoryContextRepository>()
    register<InMemoryFlytJobbRepository>()
    register<InMemoryMeldekortRepository>()
    register<InMemoryMeldeperiodeRepository>()
    register<InMemoryMellomlagretVurderingRepository>()
    register<InMemoryMottattDokumentRepository>()
    register<InMemoryPersonopplysningRepository>()
    register<InMemoryPersonRepository>()
    register<InMemoryPipRepository>()
    register<InMemoryRefusjonKravRepository>()
    register<InMemorySakRepository>()
    register<InMemorySamordningRepository>()
    register<InMemorySamordningVurderingRepository>()
    register<InMemorySamordningYtelseRepository>()
    register<InMemoryTilkjentYtelseRepository>()
    register<InMemoryTjenestePensjonRepository>()
    register<InMemoryTrukketSøknadRepository>()
    register<InMemoryUnderveisRepository>()
    register<InMemoryVilkårsresultatRepository>()
}

val inMemoryRepositoryProvider = inMemoryRepositoryRegistry.provider(MockConnection().toDBConnection())