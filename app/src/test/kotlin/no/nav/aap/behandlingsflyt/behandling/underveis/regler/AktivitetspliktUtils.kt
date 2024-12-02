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
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDate
import java.time.ZoneId


fun underveisInput(
    rettighetsperiode: Periode,
    aktivitetspliktDokument: Set<AktivitetspliktDokument> = setOf(),
) = tomUnderveisInput.copy(
    rettighetsperiode = rettighetsperiode,
    aktivitetspliktGrunnlag = AktivitetspliktGrunnlag(aktivitetspliktDokument),
)

fun brudd(
    bruddType: BruddType,
    paragraf: Brudd.Paragraf,
    periode: Periode,
    opprettet: LocalDate = periode.tom.plusMonths(4),
    grunn: Grunn = INGEN_GYLDIG_GRUNN,
) = AktivitetspliktRegistrering(
    brudd = Brudd(
        sakId = SakId(1),
        periode = periode,
        bruddType = bruddType,
        paragraf = paragraf,
    ),
    metadata = AktivitetspliktDokument.Metadata(
        id = BruddAktivitetspliktId(0),
        hendelseId = HendelseId.ny(),
        innsendingId = InnsendingId.ny(),
        innsender = Bruker(""),
        opprettetTid = opprettet.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(),
    ),
    begrunnelse = "Informasjon fra tiltaksarrangør",
    grunn = grunn,
)