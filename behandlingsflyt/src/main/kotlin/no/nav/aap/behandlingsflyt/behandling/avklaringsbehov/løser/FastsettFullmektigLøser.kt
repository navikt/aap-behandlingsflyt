package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.Organisasjonsnummer
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.validering.FolkeregisterIdentValidering
import no.nav.aap.komponenter.verdityper.validering.OrganisasjonsnummerValidering
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettFullmektigLøser(
    private val fullmektigRepository: FullmektigRepository,
    private val eregGateway: EnhetsregisteretGateway
) : AvklaringsbehovsLøser<FastsettFullmektigLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        fullmektigRepository = repositoryProvider.provide(),
        eregGateway = gatewayProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettFullmektigLøsning): LøsningsResultat {
        return when (val validertLøsning = valider(løsning.fullmektigVurdering)) {
            is Validation.Invalid -> throw UgyldigForespørselException(validertLøsning.errorMessage)
            is Validation.Valid -> {
                fullmektigRepository.lagre(
                    kontekst.kontekst.behandlingId,
                    vurdering = løsning.fullmektigVurdering.tilVurdering(kontekst.bruker)
                )
                LøsningsResultat(
                    begrunnelse = "Fastatt fullmektig"
                )
            }
        }
    }

    private fun valider(løsning: FullmektigLøsningDto): Validation<FullmektigLøsningDto> {
        if (løsning.harFullmektig) {
            val fullmektig = løsning.fullmektigIdentMedType

            if (fullmektig?.type == IdentType.ORGNR) {
                if (!OrganisasjonsnummerValidering.erGyldig(fullmektig.ident)) {
                    return Validation.Invalid(løsning, "Organisasjonsnummeret er ikke gyldig")
                }
                if (eregGateway.hentEREGData(Organisasjonsnummer(fullmektig.ident)) == null) {
                    return Validation.Invalid(løsning, "Fant ikke organisasjonsnummeret i enhetsregisteret")
                }
            } else if (fullmektig?.type == IdentType.FNR_DNR) {
                if (!FolkeregisterIdentValidering.erGyldig(fullmektig.ident)) {
                    return Validation.Invalid(
                        løsning,
                        "Fullmektig ident er av type FNR_DNR, men identen er ikke en gyldig folkeregisteridentifikator"
                    )
                }
            }
        }
        return Validation.Valid(løsning)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_FULLMEKTIG
    }
}