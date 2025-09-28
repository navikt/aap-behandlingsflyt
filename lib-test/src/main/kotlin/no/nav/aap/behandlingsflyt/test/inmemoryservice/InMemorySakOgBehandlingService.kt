package no.nav.aap.behandlingsflyt.test.inmemoryservice

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository

val InMemorySakOgBehandlingService = SakOgBehandlingService(
    grunnlagKopierer = object : GrunnlagKopierer {
        override fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        }
    },
    sakRepository = InMemorySakRepository,
    behandlingRepository = InMemoryBehandlingRepository,
    trukketSøknadService = TrukketSøknadService(
        InMemoryTrukketSøknadRepository
    ),
    unleashGateway = FakeUnleash,
    avbrytRevurderingService = AvbrytRevurderingService(
        InMemoryAvbrytRevurderingRepository,
        InMemoryBehandlingRepository
    )
)
