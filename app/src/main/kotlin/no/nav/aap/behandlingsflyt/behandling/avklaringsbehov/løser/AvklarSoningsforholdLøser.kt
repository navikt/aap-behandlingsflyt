package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSoningsforholdLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarSoningsforholdLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSoningsforholdLøsning> {

    private val soningRepository = InstitusjonsoppholdRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSoningsforholdLøsning): LøsningsResultat {
        // TODO Kombiner i tidslinje basert på forrige vedtatte verdier
        soningRepository.lagreSoningsVurdering(
            kontekst.kontekst.behandlingId,
            løsning.soningsvurdering.vurderinger.map { it.tilDomeneobjekt() })

        return LøsningsResultat(løsning.soningsvurdering.vurderinger.map { it.begrunnelse }.joinToString(" "))
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SONINGSFORRHOLD
    }

}
