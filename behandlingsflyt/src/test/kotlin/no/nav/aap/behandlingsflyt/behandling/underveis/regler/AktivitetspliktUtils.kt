package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.ZoneId


fun underveisInput(
    rettighetsperiode: Periode,
    aktivitetspliktDokument: Set<AktivitetspliktDokument> = setOf(),
) = tomUnderveisInput(
    rettighetsperiode = rettighetsperiode,
    aktivitetspliktGrunnlag = AktivitetspliktGrunnlag(aktivitetspliktDokument),
)

fun brudd(
    bruddType: BruddType,
    paragraf: Brudd.Paragraf,
    periode: Periode,
    opprettet: LocalDate = periode.tom.plusMonths(4),
    grunn: Grunn = INGEN_GYLDIG_GRUNN,
    innsendingId: InnsendingId = InnsendingId.ny(),
) = AktivitetspliktRegistrering(
    brudd = Brudd(
        periode = periode,
        bruddType = bruddType,
        paragraf = paragraf,
    ),
    metadata = AktivitetspliktDokument.Metadata(
        id = BruddAktivitetspliktId(0),
        hendelseId = HendelseId.ny(),
        innsendingId = innsendingId,
        innsender = Bruker(""),
        opprettetTid = opprettet.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(),
    ),
    begrunnelse = "Informasjon fra tiltaksarrangør",
    grunn = grunn,
)