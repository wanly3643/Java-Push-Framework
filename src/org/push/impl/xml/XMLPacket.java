package org.push.impl.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.push.monitor.AnalyticsProtocol;
import org.push.protocol.IncomingPacket;
import org.push.protocol.OutgoingPacket;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Packet with XML string as incoming and outgoing packet.
 * 
 * @author Lei Wang
 */

public class XMLPacket implements IncomingPacket, OutgoingPacket {

	private static DocumentBuilder documentBuilder;
	private static Transformer transformer;
	
	static {
		try {
			documentBuilder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
		transformer.setOutputProperty("encoding","UTF-8");
	}  

	private Document document;
	private Element xmlRoot;
	private String data;
	private  boolean isXmlFormat;

	public XMLPacket(AnalyticsProtocol typeId) {
		document = documentBuilder.newDocument();
	    xmlRoot = document.createElement("root");
	    document.appendChild(xmlRoot);

        xmlRoot.setAttribute("typeId", Integer.toString(typeId.value()));
        isXmlFormat = true;
    }

	public XMLPacket(String data) {
        isXmlFormat = false;
        decode(data);
    }

	public XMLPacket() { }

	public AnalyticsProtocol getTypeId() {
        if(!xmlRoot.hasAttributes())
            return null;
        String val = xmlRoot.getAttribute("typeId");
        if(val != null) {
        	int value = Integer.valueOf(val);
        	return AnalyticsProtocol.get(value);
        }

        return null;
    }


	public Element getRoot() {
        return xmlRoot;
    }

	public boolean encode() {
        if (!isXmlFormat) {
            return true;
        }

        if (!xmlRoot.hasAttributes()) {
            return false;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();   
          
        try {
			transformer.transform(new DOMSource(document), new StreamResult(bos));
		} catch (TransformerException e) {
			return false;
		}    
          
        data = bos.toString();

        return true;
    }


	public boolean decode(String data) {
		Document document = null;
		try {
			document = documentBuilder.parse(
					new InputSource(new StringReader(data)));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (document == null)
			return false;

		NodeList nodeList = document.getChildNodes();
		if (nodeList == null || nodeList.getLength() != 1)
			return false;
		
        Node node = nodeList.item(0);
        if (node instanceof Element) {
        	Element xmlRoot = (Element)node;
        	if ("root".equals(xmlRoot.getNodeName()) && 
        			xmlRoot.hasAttributes()) {
                this.xmlRoot = xmlRoot;
                this.document = document;
                this.isXmlFormat = true;
                
                return true;
        	}
        }
        return false;
    }


	public String getArgumentAsText(String argName) {
		if (!xmlRoot.hasAttributes()) {
			return "";
		}
        String val = xmlRoot.getAttribute(argName);
        if (val == null) {
            return "";
        }
        return val;
    }

	public int getArgumentAsInt(String argName) {
		return Integer.parseInt(getArgumentAsText(argName));
    }

	public boolean getArgumentAsBool(String argName) {
		return Boolean.parseBoolean(getArgumentAsText(argName));
    }

	public void setArgumentAsText(String argName, String val) {
        if (!xmlRoot.hasAttributes()) {
            return;
        }
        xmlRoot.setAttribute(argName, val);
    }

	public void setArgumentAsInt(String argName, int val) {
        setArgumentAsText(argName, Integer.toString(val));
    }

	public void setArgumentAsBool(String argName, boolean val) {
        setArgumentAsText(argName, Boolean.toString(val));
    }

	public String getData() {
        return data;
    }
}
