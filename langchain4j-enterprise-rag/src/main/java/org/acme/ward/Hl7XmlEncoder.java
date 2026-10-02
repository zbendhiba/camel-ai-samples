package org.acme.ward;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Encodes a parsed HL7v2 message into the standard HL7 XML form (urn:hl7-org:v2xml),
 * the document the DataMapper's XSLT maps over.
 */
@ApplicationScoped
public class Hl7XmlEncoder {

    private final DefaultXMLParser parser = new DefaultXMLParser();

    public String encode(Message message) throws HL7Exception {
        return parser.encode(message);
    }
}
