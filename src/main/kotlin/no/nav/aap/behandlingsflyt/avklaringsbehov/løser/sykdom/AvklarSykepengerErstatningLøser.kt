package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarSykepengerErstatningLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    private val behandlingRepository = behandlingRepository(connection)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykepengerErstatningLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.vurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.vurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
