package no.nav.syfo.testhelper.generator

fun defaultFellesformatDialogmeldingEndreTidStedXmlRegex(): Regex {
    return Regex(
        "<\\?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"\\?>\n" +
            "<EI_fellesformat xmlns=\"http://www.nav.no/xml/eiff/2/\" xmlns:ns6=\"http://www.kith.no/xmlstds/base64container\" xmlns:ns5=\"http://www.kith.no/xmlstds/felleskomponent1\" xmlns:ns2=\"http://www.kith.no/xmlstds/msghead/2006-05-24\" xmlns:ns4=\"http://www.kith.no/xmlstds/dialog/2006-10-11\" xmlns:ns3=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "    <ns2:MsgHead>\n" +
            "        <ns2:MsgInfo>\n" +
            "            <ns2:Type V=\"DIALOG_FORESPORSEL\" DN=\"Forespørsel\"/>\n" +
            "            <ns2:MIGversion>v1.2 2006-05-24</ns2:MIGversion>\n" +
            "            <ns2:GenDate>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{0,9}</ns2:GenDate>\n" +
            "            <ns2:MsgId>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}</ns2:MsgId>\n" +
            "            <ns2:Ack V=\"J\" DN=\"Ja\"/>\n" +
            "            <ns2:ConversationRef>\n" +
            "                <ns2:RefToParent>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}</ns2:RefToParent>\n" +
            "                <ns2:RefToConversation>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}</ns2:RefToConversation>\n" +
            "            </ns2:ConversationRef>\n" +
            "            <ns2:Sender>\n" +
            "                <ns2:Organisation>\n" +
            "                    <ns2:OrganisationName>NAV</ns2:OrganisationName>\n" +
            "                    <ns2:Ident>\n" +
            "                        <ns2:Id>889640782</ns2:Id>\n" +
            "                        <ns2:TypeId V=\"ENH\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Organisasjonsnummeret i Enhetsregisteret\"/>\n" +
            "                    </ns2:Ident>\n" +
            "                    <ns2:Ident>\n" +
            "                        <ns2:Id>79768</ns2:Id>\n" +
            "                        <ns2:TypeId V=\"HER\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Identifikator fra Helsetjenesteenhetsregisteret \\(HER-id\\)\"/>\n" +
            "                    </ns2:Ident>\n" +
            "                </ns2:Organisation>\n" +
            "            </ns2:Sender>\n" +
            "            <ns2:Receiver>\n" +
            "                <ns2:Organisation>\n" +
            "                    <ns2:Ident>\n" +
            "                        <ns2:Id>99</ns2:Id>\n" +
            "                        <ns2:TypeId V=\"HER\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Identifikator fra Helsetjenesteenhetsregisteret \\(HER-id\\)\"/>\n" +
            "                    </ns2:Ident>\n" +
            "                    <ns2:Ident>\n" +
            "                        <ns2:Id>123456789</ns2:Id>\n" +
            "                        <ns2:TypeId V=\"ENH\" S=\"2.16.578.1.12.4.1.1.9051\" DN=\"Organisasjonsnummeret i Enhetsregisteret\"/>\n" +
            "                    </ns2:Ident>\n" +
            "                    <ns2:Address>\n" +
            "                        <ns2:Type V=\"RES\" DN=\"Besøksadresse\"/>\n" +
            "                        <ns2:StreetAdr>adresse</ns2:StreetAdr>\n" +
            "                        <ns2:PostalCode>1234</ns2:PostalCode>\n" +
            "                        <ns2:City>poststed</ns2:City>\n" +
            "                    </ns2:Address>\n" +
            "                    <ns2:HealthcareProfessional>\n" +
            "                        <ns2:RoleToPatient V=\"6\" S=\"2.16.578.1.12.4.1.1.9034\" DN=\"Fastlege\"/>\n" +
            "                        <ns2:FamilyName>Scully</ns2:FamilyName>\n" +
            "                        <ns2:MiddleName>Katherine</ns2:MiddleName>\n" +
            "                        <ns2:GivenName>Dana</ns2:GivenName>\n" +
            "                        <ns2:Ident>\n" +
            "                            <ns2:Id>12125678911</ns2:Id>\n" +
            "                            <ns2:TypeId V=\"FNR\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"Fødselsnummer Norsk fødselsnummer\"/>\n" +
            "                        </ns2:Ident>\n" +
            "                        <ns2:Ident>\n" +
            "                            <ns2:Id>9</ns2:Id>\n" +
            "                            <ns2:TypeId V=\"HPR\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"HPR-nummer\"/>\n" +
            "                        </ns2:Ident>\n" +
            "                    </ns2:HealthcareProfessional>\n" +
            "                </ns2:Organisation>\n" +
            "            </ns2:Receiver>\n" +
            "            <ns2:Patient>\n" +
            "                <ns2:FamilyName>Etternavn</ns2:FamilyName>\n" +
            "                <ns2:MiddleName>Mellomnavn</ns2:MiddleName>\n" +
            "                <ns2:GivenName>Fornavn</ns2:GivenName>\n" +
            "                <ns2:Ident>\n" +
            "                    <ns2:Id>01010112345</ns2:Id>\n" +
            "                    <ns2:TypeId V=\"FNR\" S=\"2.16.578.1.12.4.1.1.8116\" DN=\"Fødselsnummer\"/>\n" +
            "                </ns2:Ident>\n" +
            "            </ns2:Patient>\n" +
            "        </ns2:MsgInfo>\n" +
            "        <ns2:Document>\n" +
            "            <ns2:DocumentConnection V=\"H\" DN=\"Hoveddokument\"/>\n" +
            "            <ns2:RefDoc>\n" +
            "                <ns2:IssueDate V=\"\\d{4}-\\d{2}-\\d{2}\"/>\n" +
            "                <ns2:MsgType V=\"XML\" DN=\"XML-instans\"/>\n" +
            "                <ns2:MimeType>text/xml</ns2:MimeType>\n" +
            "                <ns2:Content>\n" +
            "                    <ns4:Dialogmelding>\n" +
            "                        <ns4:Foresporsel>\n" +
            "                            <ns4:TypeForesp V=\"2\" S=\"2.16.578.1.12.4.1.1.8125\" DN=\"Endring dialogmøte 2\"/>\n" +
            "                            <ns4:Sporsmal>Nytt tid og sted</ns4:Sporsmal>\n" +
            "                            <ns4:DokIdForesp>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}</ns4:DokIdForesp>\n" +
            "                        </ns4:Foresporsel>\n" +
            "                    </ns4:Dialogmelding>\n" +
            "                </ns2:Content>\n" +
            "            </ns2:RefDoc>\n" +
            "        </ns2:Document>\n" +
            "        <ns2:Document>\n" +
            "            <ns2:DocumentConnection V=\"V\" DN=\"Vedlegg\"/>\n" +
            "            <ns2:RefDoc>\n" +
            "                <ns2:IssueDate V=\"\\d{4}-\\d{2}-\\d{2}\"/>\n" +
            "                <ns2:MsgType V=\"A\" DN=\"Vedlegg\"/>\n" +
            "                <ns2:MimeType>application/pdf</ns2:MimeType>\n" +
            "                <ns2:Content>\n" +
            "                    <ns6:Base64Container/>\n" +
            "                </ns2:Content>\n" +
            "            </ns2:RefDoc>\n" +
            "        </ns2:Document>\n" +
            "    </ns2:MsgHead>\n" +
            "    <MottakenhetBlokk partnerReferanse=\"1\" ebRole=\"Saksbehandler\" ebService=\"DialogmoteInnkalling\" ebAction=\"MoteInnkalling\"/>\n" +
            "</EI_fellesformat>\n"
    )
}
