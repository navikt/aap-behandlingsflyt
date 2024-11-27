package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

class StrukturertDokument<T>(val data: T, val brevkategori: InnsendingType) : StrukturerteData