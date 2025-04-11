package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class TrekkSøknadLøser(connection: DBConnection) : AvklaringsbehovsLøser<TrekkSøknadLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
    private val trekkSøknadRepository = repositoryProvider.provide<TrukketSøknadRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning
    ): LøsningsResultat {
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
