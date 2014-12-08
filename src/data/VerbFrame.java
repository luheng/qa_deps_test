package data;

import java.io.IOException;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.w3c.dom.*;

public class VerbFrame {

	public VerbFrame() {
		
	}
	
	public static VerbFrame readPropBankVerbFrame(String xmlFilename) {
		VerbFrame vf = new VerbFrame();
		Document dom;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
       
        try {
        	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
        				   false);
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            dom = db.parse(xmlFilename);
            Element doc = dom.getDocumentElement();
            
            // Process predicates.
            NodeList predicates = doc.getElementsByTagName("predicate"),
            		 rolesets,
            		 roles;
            
            for (int i = 0; i < predicates.getLength(); i++) {
            	Node pNode = predicates.item(i);
            	if (pNode.getNodeType() != Node.ELEMENT_NODE) {
            		continue;
            	}
            	Element predicate = (Element) pNode;
            	String lemma = predicate.getAttribute("lemma");
            	System.out.println(lemma);
            	
            	// Process role-sets, which are the different means for each
            	// predicate.
            	rolesets = predicate.getElementsByTagName("roleset");
            	for (int j = 0; j < rolesets.getLength(); j++) {
            		Node rsNode = rolesets.item(j);
            		if (rsNode.getNodeType() != Node.ELEMENT_NODE) {
            			continue;
            		}
            		Element roleset = (Element) rsNode;
            		String rolesetID = roleset.getAttribute("id");
            		String rolesetName = roleset.getAttribute("name");
            		System.out.println("- " + rolesetID + ", " + rolesetName);
            		
            		// Get roles - the arguments in the predicate.
            		roles = roleset.getElementsByTagName("role");
            		for (int k = 0; k < roles.getLength(); k++) {
            			Node rNode = roles.item(k);
            			if (rNode.getNodeType() != Node.ELEMENT_NODE) {
            				continue;
            			}
            			Element role = (Element) rNode;
            			String description = role.getAttribute("descr");
            			int argNum = Integer.parseInt(role.getAttribute("n"));
            			System.out.println("-- " + argNum + ", " + description);
            			// TODO: process VN roles.
            		}
            	}
            	
            }
            
        } catch (ParserConfigurationException pce) {
            System.out.println(pce.getMessage());
        } catch (SAXException se) {
            System.out.println(se.getMessage());
        } catch (IOException e) {
			e.printStackTrace();
		}
        return vf;
	}
	
	public String toString() {
		return "foo";
	}
	
	public static void main(String[] args) {
		// Test VerbFrame reader.
		String filename = "/Users/luheng/data/PropBank/data/frames/keep.xml";
		VerbFrame vf = VerbFrame.readPropBankVerbFrame(filename);
	}
}
