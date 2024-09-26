package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSoningsforholdLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.SoningRepository
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarSoningsforholdLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSoningsforholdLøsning> {

    private val soningRepository = SoningRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSoningsforholdLøsning): LøsningsResultat {
        soningRepository.lagre(kontekst.kontekst.behandlingId, løsning.soningsvurdering.tilDomeneobjekt())
        return LøsningsResultat(løsning.soningsvurdering.begrunnelse ?: "") // TODO: Kan ikke være null
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SONINGSFORRHOLD
    }

}
