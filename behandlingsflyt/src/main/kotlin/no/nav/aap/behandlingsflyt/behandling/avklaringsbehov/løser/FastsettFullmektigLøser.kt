package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.verdityper.validering.FolkeregisterIdentValidering
import no.nav.aap.komponenter.verdityper.validering.OrganisasjonsnummerValidering
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettFullmektigLøser(
    private val fullmektigRepository: FullmektigRepository,
) : AvklaringsbehovsLøser<FastsettFullmektigLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        fullmektigRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettFullmektigLøsning): LøsningsResultat {
        validerFullmektig(løsning.fullmektigVurdering)

        fullmektigRepository.lagre(
            kontekst.kontekst.behandlingId,
            vurdering = løsning.fullmektigVurdering.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = "Fastatt fullmektig"
        )
    }

    private fun validerFullmektig(løsning: FullmektigLøsningDto) {
        if (!løsning.harFullmektig) return

        val fullmektig = løsning.fullmektigIdentMedType

        if (fullmektig?.type == IdentType.ORGNR) {
            check(OrganisasjonsnummerValidering.erGyldig(fullmektig.ident)) {
                "Fullmektig ident er av type ORGNR, men organisasjonsnummeret er ikke gyldig"
            }
        } else if (fullmektig?.type == IdentType.FNR_DNR) {
            check(FolkeregisterIdentValidering.erGyldig(fullmektig.ident)) {
                "Fullmektig ident er av type FNR_DNR, men identen er ikke en gyldig folkeregisteridentifikator"
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_FULLMEKTIG
    }
}