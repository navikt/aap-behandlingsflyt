package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding

class StrukturertDokument<M : Melding>(val data: M) : StrukturerteData