package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarHelseinstitusjonLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    private val helseinstitusjonRepository = InstitusjonsoppholdRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarHelseinstitusjonLøsning): LøsningsResultat {
        // TODO Kombiner i tidslinje basert på forrige vedtatte verdier
        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            løsning.helseinstitusjonVurdering.vurderinger.map { it.tilDomeneobjekt() })
        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.map { it.begrunnelse }.joinToString(" "))
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }
}