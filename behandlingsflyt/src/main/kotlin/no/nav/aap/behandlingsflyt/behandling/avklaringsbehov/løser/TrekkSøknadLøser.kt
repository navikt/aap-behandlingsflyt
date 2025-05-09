package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class TrekkSøknadLøser(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val trekkSøknadRepository: TrukketSøknadRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<TrekkSøknadLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide(),
        trekkSøknadRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            "kan kun trekke søknader i førstegangsbehandling"
        }
        require(behandling.status() in listOf(Status.OPPRETTET, Status.UTREDES)) {
            "kan kun trekke søknader som utredes"
        }

        val søknader = mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId(), InnsendingType.SØKNAD)
        for (søknad in søknader) {
            trekkSøknadRepository.lagreTrukketSøknadVurdering(
                kontekst.behandlingId(),
                TrukketSøknadVurdering(
                    journalpostId = søknad.referanse.asJournalpostId,
                    begrunnelse = løsning.begrunnelse,
                    vurdertAv = kontekst.bruker,
                    vurdert = Instant.now(),
                )
            )
        }
        return LøsningsResultat(løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_TREKK_AV_SØKNAD
    }
}
