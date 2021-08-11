package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.kith.xmlstds.msghead._2006_05_24.XMLRefDoc
import java.util.stream.Stream

class Dokument(xmlDocument: XMLDocument) {
    private val dialogmeldingListe: MutableList<Dialogmelding>
    private val vedleggListe: MutableList<Vedlegg>
    private fun getContent(xmlDocument: XMLDocument): Stream<Any> {
        return Stream.of(xmlDocument)
            .map { obj: XMLDocument -> obj.refDoc }
            .map { obj: XMLRefDoc -> obj.content }
            .map { obj: XMLRefDoc.Content -> obj.getAny() }
            .flatMap { obj: List<Any> -> obj.stream() }
    }

    fun erForesporsel(): Boolean {
        return dialogmeldingListe.stream().anyMatch { obj: Dialogmelding -> obj.erForesporsel() }
    }

    fun erNotat(): Boolean {
        return dialogmeldingListe.stream().anyMatch { obj: Dialogmelding -> obj.erNotat() }
    }

    fun harVedlegg(): Boolean {
        return !vedleggListe.isEmpty()
    }

    val dokIdForesporselStream: Stream<String?>
        get() = dialogmeldingListe.stream().flatMap { obj: Dialogmelding -> obj.dokIdForesporselStream }
    val dokIdNotatStream: Stream<String?>
        get() = dialogmeldingListe.stream().flatMap { obj: Dialogmelding -> obj.dokIdNotatStream }

    init {
        dialogmeldingListe = ArrayList()
        vedleggListe = ArrayList()
        getContent(xmlDocument).forEach { o: Any? ->
            if (o is XMLDialogmelding) {
                dialogmeldingListe.add(Dialogmelding1_0(o))
            } else if (o is no.kith.xmlstds.dialog._2013_01_23.XMLDialogmelding) {
                dialogmeldingListe.add(Dialogmelding1_1(o))
            } else if (o is XMLBase64Container) {
                vedleggListe.add(Vedlegg(o as XMLBase64Container?))
            }
        }
    }
}
