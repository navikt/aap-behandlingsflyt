package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId

enum class DokumentType {
    REGISTRERING,
    FEILREGISTRERING,
}


data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: BruddType,
    val paragraf: Paragraf?,
    val begrunnelse: String,
    val grunn: Grunn?,
    val perioder: List<Periode>,
    val dokumenttype: DokumentType,
) {
    fun tilDomene(sakId: SakId, periode: Periode, innsender: NavIdent): AktivitetspliktRepository.DokumentInput {
        val brudd = Brudd(
            sakId = sakId,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf?.somDomene),
        )
        return when (dokumenttype) {
            DokumentType.REGISTRERING ->
                AktivitetspliktRepository.RegistreringInput(
                    brudd = brudd,
                    innsender = innsender,
                    begrunnelse = begrunnelse,
                    grunn = grunn ?: Grunn.INGEN_GYLDIG_GRUNN,
                )
            DokumentType.FEILREGISTRERING -> AktivitetspliktRepository.FeilregistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = begrunnelse,
            )
        }
    }
}

enum class Paragraf(val somDomene: Brudd.Paragraf) {
    PARAGRAF_11_8(Brudd.Paragraf.PARAGRAF_11_8),
    PARAGRAF_11_9(Brudd.Paragraf.PARAGRAF_11_9),
}

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val grunn: Grunn,
    val periode: Periode,
)
