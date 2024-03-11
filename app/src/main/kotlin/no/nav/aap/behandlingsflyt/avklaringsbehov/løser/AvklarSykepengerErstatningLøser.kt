package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekst

class AvklarSykepengerErstatningLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykepengerErstatningLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.sykepengeerstatningVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
