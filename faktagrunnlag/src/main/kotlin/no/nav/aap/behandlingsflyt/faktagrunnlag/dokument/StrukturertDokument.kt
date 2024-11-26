package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkode

class StrukturertDokument<T>(val data: T, val brevkode: Brevkode) : StrukturerteData