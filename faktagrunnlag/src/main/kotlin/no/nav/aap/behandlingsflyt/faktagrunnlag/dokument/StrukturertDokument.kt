package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.Brevkategori

class StrukturertDokument<T>(val data: T, val brevkategori: Brevkategori) : StrukturerteData