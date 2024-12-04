package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktFeilregistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt.GrunnDTO.entries
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode

interface AktivitetspliktDTO {
    fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput>
}

data class OpprettAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf?,
    val begrunnelse: String,
    val grunn: GrunnDTO?,
    val perioder: List<Periode>,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        require(grunn != GrunnDTO.FEILREGISTRERING)
        return perioder.map { periode ->
            val brudd = Brudd.nyttBrudd(
                sak = sak,
                periode = periode,
                bruddType = brudd,
                paragraf = brudd.paragraf(paragraf),
            )
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = begrunnelse,
                grunn = grunn?.tilDomene() ?: Grunn.INGEN_GYLDIG_GRUNN,
            )
        }
    }
}

class OppdaterAktivitetspliktDTOV2(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
    val grunn: GrunnDTO,
    val begrunnelse: String,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd.nyttBrudd(
            sak = sak,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        if (grunn == GrunnDTO.FEILREGISTRERING) {
            return listOf(
                AktivitetspliktRepository.FeilregistreringInput(
                    brudd = brudd,
                    innsender = innsender,
                    begrunnelse = begrunnelse,
                )
            )
        }
        return listOf(
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = begrunnelse,
                grunn = grunn.tilDomene(),
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
    val grunn: GrunnDTO,
    val periode: Periode,
    val begrunnelse: String,
)

fun List<AktivitetspliktDokument>.utledBruddTilstand(): List<BruddAktivitetspliktHendelseDto> {
    val grunnlag = AktivitetspliktGrunnlag(this.toSet())
    val segmenter = grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_7).asSequence() +
            grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_8).asSequence() +
            grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_9).asSequence()

    return segmenter.map { segment ->
        val dokument = segment.verdi
        BruddAktivitetspliktHendelseDto(
            brudd = dokument.brudd.bruddType,
            paragraf = dokument.brudd.paragraf,
            grunn = when (dokument) {
                is AktivitetspliktFeilregistrering -> GrunnDTO.FEILREGISTRERING
                is AktivitetspliktRegistrering -> GrunnDTO.fraDomene(dokument.grunn)
            },
            periode = segment.periode,
            begrunnelse = when (dokument) {
                is AktivitetspliktFeilregistrering -> dokument.begrunnelse
                is AktivitetspliktRegistrering -> dokument.begrunnelse
            }
        )
    }
        .sortedBy { it.periode }
        .toList()
}

enum class GrunnDTO(private val tilDomene: Grunn?) {
    SYKDOM_ELLER_SKADE(Grunn.SYKDOM_ELLER_SKADE),
    STERKE_VELFERDSGRUNNER(Grunn.STERKE_VELFERDSGRUNNER),
    RIMELIG_GRUNN(Grunn.RIMELIG_GRUNN),
    INGEN_GYLDIG_GRUNN(Grunn.INGEN_GYLDIG_GRUNN),
    FEILREGISTRERING(null);

    fun tilDomene(): Grunn {
        return requireNotNull(tilDomene) {
            "$this kan ikke representeres som en grunn i domenet"
        }
    }

    companion object {
        fun fraDomene(grunn: Grunn): GrunnDTO {
            return requireNotNull(entries.find { it.tilDomene == grunn }) {
                "$grunn kan ikke representeres som GrunnDTO"
            }
        }
    }
}
