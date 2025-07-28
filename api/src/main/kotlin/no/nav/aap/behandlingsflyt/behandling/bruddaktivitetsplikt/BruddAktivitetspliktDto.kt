package no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktFeilregistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FeilregistreringInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.RegistreringInput
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

interface AktivitetspliktDTO {
    fun tilDomene(sak: Sak, innsender: Bruker): List<DokumentInput>
}

data class OpprettAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf?,
    val begrunnelse: String,
    val grunn: GrunnDTO?,
    val perioder: List<PeriodeDTO>,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<DokumentInput> {
        require(grunn != GrunnDTO.FEILREGISTRERING)
        return perioder.map { periode ->
            val brudd = Brudd.nyttBrudd(
                sak = sak,
                periode = periode.tilDomene(),
                bruddType = brudd,
                paragraf = brudd.paragraf(paragraf),
            )
            RegistreringInput(
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
    val periode: PeriodeDTO,
    val grunn: GrunnDTO,
    val begrunnelse: String,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<DokumentInput> {
        val brudd = Brudd.nyttBrudd(
            sak = sak,
            periode = periode.tilDomene(),
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        if (grunn == GrunnDTO.FEILREGISTRERING) {
            return listOf(
                FeilregistreringInput(
                    brudd = brudd,
                    innsender = innsender,
                    begrunnelse = begrunnelse,
                )
            )
        }
        return listOf(
            RegistreringInput(
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

data class Effektuer11_7Dto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val begrunnelse: String?,
    val forhåndsvarselDato: LocalDate?,
    val gjeldendeBrudd: List<BruddAktivitetspliktHendelseDto>,
)

data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val grunn: GrunnDTO,
    val periode: PeriodeDTO,
    val begrunnelse: String,
)

fun List<AktivitetspliktDokument>.utledBruddTilstand(): List<BruddAktivitetspliktHendelseDto> {
    val grunnlag = AktivitetspliktGrunnlag(this.toSet())
    val segmenter = grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_7).asSequence() +
            grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_8).asSequence() +
            grunnlag.dokumentTidslinje(Brudd.Paragraf.PARAGRAF_11_9).asSequence()

    return segmenter.map { segment ->
        val dokument = segment.verdi
        bruddAktivitetspliktHendelseDto(dokument, segment.periode)
    }
        .sortedBy { it.periode.tilDomene() }
        .toList()
}

fun bruddAktivitetspliktHendelseDto(
    dokument: AktivitetspliktDokument,
    periode: Periode
) = BruddAktivitetspliktHendelseDto(
    brudd = dokument.brudd.bruddType,
    paragraf = dokument.brudd.paragraf,
    grunn = when (dokument) {
        is AktivitetspliktFeilregistrering -> GrunnDTO.FEILREGISTRERING
        is AktivitetspliktRegistrering -> GrunnDTO.fraDomene(dokument.grunn)
    },
    periode = PeriodeDTO.fraDomene(periode),
    begrunnelse = when (dokument) {
        is AktivitetspliktFeilregistrering -> dokument.begrunnelse
        is AktivitetspliktRegistrering -> dokument.begrunnelse
    }
)

enum class GrunnDTO(private val tilDomene: Grunn?) {
    SYKDOM_ELLER_SKADE(Grunn.SYKDOM_ELLER_SKADE),
    STERKE_VELFERDSGRUNNER(Grunn.STERKE_VELFERDSGRUNNER),
    RIMELIG_GRUNN(Grunn.RIMELIG_GRUNN),
    INGEN_GYLDIG_GRUNN(Grunn.INGEN_GYLDIG_GRUNN),
    BIDRAR_AKTIVT(Grunn.BIDRAR_AKTIVT),
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

data class PeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate?,
) {
    fun tilDomene(): Periode {
        return Periode(fom, tom ?: Tid.MAKS)
    }

    companion object {
        fun fraDomene(periode: Periode): PeriodeDTO {
            return PeriodeDTO(
                fom = periode.fom,
                tom = periode.tom.takeUnless { it == Tid.MAKS },
            )
        }
    }
}
