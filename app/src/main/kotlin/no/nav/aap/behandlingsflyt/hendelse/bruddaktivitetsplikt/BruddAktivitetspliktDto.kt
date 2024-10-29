package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId

interface AktivitetspliktDTO {
    fun tilDomene(sakId: SakId, innsender: NavIdent): List<AktivitetspliktRepository.DokumentInput>
}

data class OpprettAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf?,
    val begrunnelse: String,
    val grunn: Grunn?,
    val perioder: List<Periode>,
) : AktivitetspliktDTO {
    override fun tilDomene(sakId: SakId, innsender: NavIdent): List<AktivitetspliktRepository.DokumentInput> {
        return perioder.map { periode ->
            val brudd = Brudd(
                sakId = sakId,
                periode = periode,
                bruddType = brudd,
                paragraf = brudd.paragraf(paragraf),
            )
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = begrunnelse,
                grunn = grunn ?: Grunn.INGEN_GYLDIG_GRUNN,
            )
        }
    }
}

data class OppdaterAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
    val grunn: Grunn,
) : AktivitetspliktDTO {
    override fun tilDomene(sakId: SakId, innsender: NavIdent): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd(
            sakId = sakId,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        return listOf(
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = "",
                grunn = grunn,
            )
        )
    }
}

data class FeilregistrerAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
) : AktivitetspliktDTO {
    override fun tilDomene(sakId: SakId, innsender: NavIdent): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd(
            sakId = sakId,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        return listOf(
            AktivitetspliktRepository.FeilregistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = "",
            )
        )
    }
}

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class SaksnummerParameter(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val grunn: Grunn,
    val periode: Periode,
)
