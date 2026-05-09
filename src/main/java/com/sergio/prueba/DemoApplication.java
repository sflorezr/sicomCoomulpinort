package com.sergio.prueba;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DemoApplication {

    public static void main(String[] args) {
        try {
            File tempo = new File("c:/tempo/rutas.txt");
            FileReader fr = new FileReader(tempo);
            BufferedReader br = new BufferedReader(fr);
            String data=br.readLine();
            String ruta=data.split("-")[0];
            String tipo=data.split("-")[1];
            String tieneMoto=data.split("-")[2];
            br.close();
            // System.out.println(ruta);
            // ruta=ruta.split(".xml")[0];

            // File file = new File("src/prueba2.xml");
            String contents = new String(Files.readAllBytes(Paths.get(ruta)));
            // System.out.println("Contents (Java 7) : " + contents);
            // DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            // Document doc = dBuilder.parse(file);
            //  doc.getDocumentElement().normalize();
            //System.out.println("Root element :" + file);
            // NodeList nList = doc.getElementsByTagName("cbc:Description");
            printJSON(contents,ruta,tipo,tieneMoto);

        /*    if (doc.hasChildNodes()) {

                printNote(doc.getChildNodes());

            }*/

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private static void printJSON(String xml,String ruta,String tipo,String tienemoto) throws IOException {
        try {
            JSONObject jsonObj = XML.toJSONObject(xml);
            String json = jsonObj.toString(4);

            // System.out.println(json);
            if(tipo.equals("1") ){
                JSONObject obj = new JSONObject(json);
                jsonObj=XML.toJSONObject(obj.getJSONObject("AttachedDocument").getJSONObject("cac:Attachment").getJSONObject("cac:ExternalReference").getString("cbc:Description"));
                json=jsonObj.toString(4);
            }else if (tipo.equals("3")){
                JSONObject obj = new JSONObject(json);
                jsonObj=XML.toJSONObject(obj.getJSONObject("AttachedDocument").getJSONObject("cac:Attachment").getJSONObject("cac:ExternalReference").getString("cbc:Description"));
                json=jsonObj.toString(4);
            }
            FileWriter myWriter = new FileWriter(ruta.split(".XML")[0]+".json");
            myWriter.write(json);
            myWriter.close();

            LeerJson(ruta.split(".XML")[0]+".json",tienemoto,tipo);
        } catch (JSONException je) {
            System.out.println(je.toString());
        }
    }
    public static void LeerJson(String ruta,String tienemoto,String tipo){
        JSONParser parser = new JSONParser();
        int cantidad=0;
        String tp="";
        String marca="";
        String color="SIN COLOR";
        String modelo="";
        String cc="";
        String motor="";
        String chasis="";
        String linea="";
        try{
            String cadena="";
            Object objeto = parser.parse(new FileReader(ruta));
            JSONObject obj = new JSONObject(objeto.toString());
            try {
                JSONArray objetos = obj.getJSONObject("Invoice").getJSONArray("cac:InvoiceLine");
                //obj.getJSONObject("AttachedDocument").getJSONObject("cac:Attachment").getJSONObject("cac:ExternalReference").getString("cbc:Description")
                //JSONArray arr = obj.getJSONArray("posts");
                cadena = obj.getJSONObject("Invoice").getJSONObject("cac:AccountingSupplierParty").getJSONObject("cac:Party").getJSONObject("cac:PartyLegalEntity").getJSONObject("cbc:CompanyID").get("content").toString() + ";" + obj.getJSONObject("Invoice").getString("cbc:IssueDate");
                //System.out.println("Nit: "+obj.getJSONObject("Invoice").getJSONObject("cac:AccountingSupplierParty").getJSONObject("cac:Party").getJSONObject("cac:PartyLegalEntity").getJSONObject("cbc:CompanyID").get("content"));
                try {
                    cadena = cadena + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:PaymentMeans").getString("cbc:PaymentDueDate");
                } catch (JSONException f) {
                    cadena = cadena + ";";
                }
                cadena = cadena + ";" + obj.getJSONObject("Invoice").get("cbc:ID").toString() + "\n";
                for (int i = 0; i < objetos.length(); i++) {
                    try {
                        if(tienemoto.equals("1")){
                            cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                            //cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(0).getString("cbc:Value").split(":")[0]+" "+objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString()+objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(0).getString("cbc:Value").split(":")[3]+" "+objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(1).getString("cbc:Value").split(" ")[0].split(":")[1]+";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                        }else{
                            cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                        }

                    } catch (JSONException j) {
                        try{
                            cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";0" + "\n";
                        }catch (JSONException f) {
                            try{
                                try{
                                    cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                                }catch (JSONException x){
                                    cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").get("cbc:BaseQuantity").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                                }
                            }catch (JSONException g) {
                                try{
                                    cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";0" + "\n";
                                }catch (JSONException h){
                                    cadena = cadena + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").get("cbc:BaseQuantity").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + objetos.getJSONObject(i).getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";0" + "\n";
                                }
                            }
                        }
                    }
                    if(tienemoto.equals("1")){

                        try {
                            if (tipo.equals("1")) {
                                cantidad = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").length();
                                if (cantidad > 0) {
                                    cantidad = cantidad / 4;
                                    for (int o = 1; o <cantidad * 4; o += 4) {
                                        cadena = cadena + "MT;" + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(o).getString("cbc:Value");
                                        cadena = cadena + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(o + 1).getString("cbc:Value");
                                        cadena = cadena + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(o + 2).getString("cbc:Value") + ";" + objetos.getJSONObject(i).getJSONObject("cac:Item").get("cbc:Description").toString() + "\n";
                                    }
                                }
                            }else if (tipo.equals("3")){
                                for(int p=0;p< objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").length();p++ ){
                                    switch (objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Name").toString()) {
                                        case "CLASE": tp = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "MARCAS": marca = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "COLOR": color = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "MODELO": modelo = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "POTENCIA": cc = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "MOTOR": motor = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "SERIAL": chasis = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                        case "LINEA": linea = objetos.getJSONObject(i).getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    }
                                }
                                cadena = cadena + "MT;"+tp+" : MARCA: ";
                                cadena = cadena +marca+" COLOR: ";
                                cadena = cadena +color+";MODELO:";
                                cadena = cadena +modelo+" C.C.: ";
                                cadena = cadena +cc+";MOTOR:";
                                cadena = cadena +motor+" CHASIS:";
                                cadena = cadena +chasis+";";
                                cadena = cadena +linea+ "\n";
                            }

                        }catch (JSONException x){
                            x.printStackTrace();
                        }
                        //obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty")
                    }
                }
            }catch (JSONException a){
                //obj.getJSONObject("AttachedDocument").getJSONObject("cac:Attachment").getJSONObject("cac:ExternalReference").getString("cbc:Description")
                //JSONArray arr = obj.getJSONArray("posts");
                cadena = obj.getJSONObject("Invoice").getJSONObject("cac:AccountingSupplierParty").getJSONObject("cac:Party").getJSONObject("cac:PartyLegalEntity").getJSONObject("cbc:CompanyID").get("content").toString() + ";" + obj.getJSONObject("Invoice").getString("cbc:IssueDate");
                //System.out.println("Nit: "+obj.getJSONObject("Invoice").getJSONObject("cac:AccountingSupplierParty").getJSONObject("cac:Party").getJSONObject("cac:PartyLegalEntity").getJSONObject("cbc:CompanyID").get("content"));
                try {
                    cadena = cadena + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:PaymentMeans").getString("cbc:PaymentDueDate");
                } catch (JSONException f) {
                    cadena = cadena + ";";
                }
                cadena = cadena + ";" + obj.getJSONObject("Invoice").get("cbc:ID") + "\n";
                try {
                    if(tienemoto.equals("1")){
                        if(tipo.equals("1")) {
                            cadena = cadena + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").get("cbc:ID").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(0).getString("cbc:Value").split(":")[0] + " " + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(0).getString("cbc:Value").split(":")[3] + " " + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(1).getString("cbc:Value").split(" ")[0].split(":")[1] + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                        }else if (tipo.equals("3")){
                            cadena = cadena + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content") + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(8).get("cbc:Value").toString().toUpperCase()+ " " + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(2).getString("cbc:Value").toString().toUpperCase()+ ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                        }

//						cadena = cadena + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                    }else{
                        cadena = cadena + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONObject("cac:SellersItemIdentification").get("cbc:ID").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:AllowanceCharge").get("cbc:MultiplierFactorNumeric").toString() + "\n";
                    }

                } catch (JSONException j) {
                    cadena = cadena + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONObject("cac:StandardItemIdentification").getJSONObject("cbc:ID").get("content") + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:BaseQuantity").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Price").getJSONObject("cbc:PriceAmount").get("content").toString() + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cbc:LineExtensionAmount").get("content").toString() + ";0" + "\n";
                }
                if(tienemoto.equals("1")){
                    try {
                        if(tipo.equals("1")) {
                            cantidad = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").length();
                            if (cantidad > 0) {
                                cantidad = cantidad / 4;
                                for (int i = 01; i < cantidad * 4; i += 4) {
                                    cadena = cadena + "MT;" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(i).getString("cbc:Value");
                                    cadena = cadena + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(i + 1).getString("cbc:Value");
                                    cadena = cadena + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(i + 2).getString("cbc:Value") + ";" + obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").get("cbc:Description").toString() + "\n";
                                }
                            }
                        }else if (tipo.equals("3")){
                            for(int p=0;p< obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").length();p++ ){
                                switch (obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Name").toString()) {
                                    case "CLASE": tp = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "MARCAS": marca = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "COLOR": color = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "MODELO": modelo = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "CILINDRAJE": cc = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "MOTOR": motor = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "SERIAL": chasis = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                    case "LINEA": linea = obj.getJSONObject("Invoice").getJSONObject("cac:InvoiceLine").getJSONObject("cac:Item").getJSONArray("cac:AdditionalItemProperty").getJSONObject(p).get("cbc:Value").toString().toUpperCase();break;
                                }
                            }
                            cadena = cadena + "MT;"+tp+" : MARCA: ";
                            cadena = cadena +marca+" COLOR: ";
                            cadena = cadena +color+";MODELO:";
                            cadena = cadena +modelo+" C.C.: ";
                            cadena = cadena +cc+";MOTOR:";
                            cadena = cadena +motor+" CHASIS:";
                            cadena = cadena +chasis+";";
                            cadena = cadena +linea+ "\n";
                        }
                    }catch (JSONException x){
                        x.printStackTrace();
                    }
                }
            }
            System.out.println(cadena);
            FileWriter myWriter = new FileWriter(ruta.split(".json")[0]+".txt");
            myWriter.write(cadena);
            myWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

