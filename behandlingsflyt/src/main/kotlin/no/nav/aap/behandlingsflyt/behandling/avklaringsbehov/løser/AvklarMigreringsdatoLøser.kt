package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarMigreringsdatoLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarMigreringsdatoLøser(
    private val migreringsdatoRepository: MigreringsdatoRepository,
) : AvklaringsbehovsLøser<AvklarMigreringsdatoLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        migreringsdatoRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarMigreringsdatoLøsning): LøsningsResultat {
        val vurdering = MigreringsdatoVurdering(
            migreringsdato = løsning.migreringsdato,
            vurdertAv = kontekst.bruker,
            vurdertIBehandling = kontekst.behandlingId(),
            opprettet = LocalDateTime.now(),
        )
        migreringsdatoRepository.lagreVurdering(kontekst.behandlingId(), vurdering)
        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_MIGRERINGSDATO
}
