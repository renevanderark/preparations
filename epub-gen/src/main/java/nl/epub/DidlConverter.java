package nl.epub;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DidlConverter {
    static {
        System.setProperty("javax.xml.xpath.XPathFactory:" + NamespaceConstant.OBJECT_MODEL_SAXON, "net.sf.saxon.xpath.XPathFactoryImpl");
    }

    private static final String SERVICE_URL = "http://imageviewer.kb.nl/ImagingService/imagingService?id=";
    private final String titlePageImage;
    private final String urlUrn;
    private List<String> altos = new ArrayList<String>();
    private String urn;
    private String title;
    private String filename;
    private List<File> altoFiles = new ArrayList<File>();

    public DidlConverter(String urn) throws XPathExpressionException, IOException {
        this.urn = urn.replaceAll(":", "_");
        this.urlUrn = urn;
        URL url = new URL("http://resolver.kb.nl/resolve?urn=" + urlUrn);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream inputStream = connection.getInputStream();
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setEncoding("UTF-8");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if(prefix.equalsIgnoreCase("didl")) {
                    return "urn:mpeg:mpeg21:2002:02-DIDL-NS";
                } else if(prefix.equalsIgnoreCase("dc")) {
                    return "http://purl.org/dc/elements/1.1/";
                }
                return null;
            }

            public String getPrefix(String namespaceURI) {
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        });
        TinyDocumentImpl root = (TinyDocumentImpl) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        this.title = (String) xpath.evaluate("//didl:Item/didl:Descriptor[2]/didl:Statement/text()", root, XPathConstants.STRING);
        System.out.println(new String(title.getBytes(), "UTF-8"));
        filename = title.replaceAll("\\/", " - ").replaceAll("\\?", "-");


        titlePageImage = (String) xpath.evaluate("//didl:Item//didl:Statement[text()='titlepage']/../../@dc:identifier", root, XPathConstants.STRING);
        System.out.println(titlePageImage);
        System.out.println(title);
        List<TinyElementImpl> items = (ArrayList) xpath.evaluate("//didl:Component//didl:Statement[text()='wordPositions']/../../didl:Resource", root, XPathConstants.NODESET);
        for(TinyElementImpl elem : items) {
            String ref = elem.getAttributeValue("", "ref");
            altos.add(ref);
        }
    }

    private void downloadAltos() throws IOException {
        new File("output/" + urn + "_altos").mkdirs();

        for(int i = 0; i < altos.size(); i++) {
            URL url = new URL(altos.get(i));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String ln;
            File f = new File("output/" + urn + "_altos/" + i + ".xml");
            PrintWriter pw = new PrintWriter(f);
            boolean found = false;
            while((ln = reader.readLine()) != null) {
                if(ln.matches(".*<String.*")) {
                    found = true;
                }
                pw.println(ln);
            }
            pw.flush();
            pw.close();
            if(found) {
                System.out.println("FOUND: " + i);
                this.altoFiles.add(f);
            }

            reader.close();

        }
    }

    private void convertAltos() throws IOException, TransformerException {
        new File("output/" + urn + "/OEBPS/xhtml").mkdirs();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/altoconv.xsl")));
        for(int i = 0; i < altoFiles.size(); i++) {
            /*URL url = new URL(altos.get(i));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();*/
            InputStream inputStream = new FileInputStream(altoFiles.get(i));
            StreamSource inputSource = new StreamSource(inputStream);
            transformer.transform(inputSource, new StreamResult(new File("output/" + urn + "/OEBPS/xhtml/" + (i+1) + ".xhtml")));
        }
    }

    private void printTitlePage() throws FileNotFoundException {
        File f = new File("output/" + urn + "/OEBPS/xhtml/0.xhtml");
        PrintWriter pw = new PrintWriter(f);
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:alto=\"http://schema.ccs-gmbh.com/ALTO\" xmlns:alto2=\"http://www.loc.gov/standards/alto/ns-v2#\">\n" +
                "   <head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title>Autogenerated page</title>\n" +
                "      <style type=\"text/css\">body { margin: 0; padding: 0; } img { width: 100%; max-height: 90%; }</style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <img src=\"../images/titlepage.jpg\" />\n" +
                "  </body>\n" +
                "</html>");
        pw.flush();
        pw.close();
    }

    private void downloadTitlePageImage() throws IOException {
        URL url = new URL("http://imageviewer.kb.nl/ImagingService/imagingService?id=" + titlePageImage + ":image&w=800");
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while (-1!=(n=in.read(buf)))
        {
            out.write(buf, 0, n);
        }
        out.close();
        in.close();
        byte[] response = out.toByteArray();
        new File("output/" + urn + "/OEBPS/images").mkdirs();
        FileOutputStream fos = new FileOutputStream("output/" + urn + "/OEBPS/images/titlepage.jpg");
        fos.write(response);
        fos.close();
    }

    private void printContainer() throws FileNotFoundException {
        new File("output/" + urn + "/META-INF").mkdirs();
        File f = new File("output/" + urn + "/META-INF/container.xml");
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.println("<?xml version=\"1.0\"?>\n" +
                "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                "    <rootfiles>\n" +
                "        <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                "   </rootfiles>\n" +
                "</container>");
        pw.flush();
        pw.close();
    }
    private void printToc() throws FileNotFoundException {
        File f = new File("output/" + urn + "/OEBPS/toc.ncx");
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<!DOCTYPE ncx PUBLIC \"-//NISO//DTD ncx 2005-1//EN\"\n" +
                "   \"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">");
        pw.println("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">");
        pw.println("<head>\n" +
                "        <meta name=\"dtb:depth\" content=\"2\"/>\n" +
                "        <meta name=\"dtb:totalPageCount\" content=\"0\"/>\n" +
                "        <meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n" +
                "</head>");
        pw.println("<docTitle>\n" +
                "   <text>" + title + "</text>\n" +
                "</docTitle>");

        printNavMap(pw);
        pw.println("</ncx>");
        pw.flush();
        pw.close();
    }

    private void printNavMap(PrintWriter pw) {
        pw.println("<navMap>");
        for(int i = 0; i <= altoFiles.size(); i++) {
            pw.println("<navPoint id=\"navPoint-" + (i+1) + "\" playOrder=\"" + (i+1) + "\">");
            pw.println("<navLabel>\n" +
                    " <text>Pagina " + (i+1) +  "</text>\n" +
                    "</navLabel>");
            pw.println("<content src=\"xhtml/" + i + ".xhtml\" />");
            pw.println("</navPoint>");
        }

        pw.println("</navMap>");
    }

    private void printOpf() throws FileNotFoundException {
        new File("output/" + urn + "/OEBPS").mkdirs();
        File f = new File("output/" + urn + "/OEBPS/content.opf");
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"BookID\" version=\"2.0\">");
        pw.println("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">");
        pw.println("<dc:language>Dutch</dc:language>");
        pw.println("<dc:rights>Public Domain</dc:rights>");
        pw.println("<dc:title>" + title + "</dc:title>");
        pw.println("<dc:identifier id=\"BookID\">" + urn + "</dc:identifier>");
        pw.println("</metadata>");
        printManifest(pw);
        printSpine(pw);
        pw.println("</package>");
        pw.flush();
        pw.close();
    }

    private void printSpine(PrintWriter pw) {
        pw.println("<spine toc=\"ncx\">");
        for(int i = 0; i <= altoFiles.size(); i++) {
            pw.println("<itemref idref=\"id" + i +"\" />");
        }

        pw.println("</spine>");
    }

    private void printManifest(PrintWriter pw) {
        pw.println("<manifest>");
        pw.println("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>");
        pw.println("<item id=\"cover-image\" properties=\"cover-image\" href=\"images/titlepage.jpg\" media-type=\"image/jpeg\"/>");
        for(int i = 0; i <= altoFiles.size(); i++) {
            pw.println("<item id=\"id" + i + "\" href=\"xhtml/" + i + ".xhtml\" media-type=\"application/xhtml+xml\"/>");
        }

        pw.println("</manifest>");
    }

    private void printMime() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File("output/" + urn + "/mimetype")));
        pw.print("application/epub+zip");
        pw.flush();
        pw.close();
    }


    public void generateEpub() throws IOException, TransformerException {

        downloadAltos();
        convertAltos();
        printContainer();
        printOpf();
        printTitlePage();
        downloadTitlePageImage();
        printToc();
        printMime();

        File directoryToZip = new File("output/" + urn);
        List<File> fileList = new ArrayList<File>();

        ZipDirectory.getAllFiles(directoryToZip, fileList);
        File found = null;
        for(File f : fileList) {
            if(f.getName().equalsIgnoreCase("mimetype")) {
                found = f;
                fileList.remove(f);
                break;
            }
        }
        fileList.add(0, found);
        ZipDirectory.writeZipFile(directoryToZip, fileList, filename);
    }




    public static void main(String[] args) throws IOException, XPathExpressionException, TransformerException {
        String didlUrn = args[0];

        DidlConverter didlConverter = new DidlConverter(didlUrn);
        didlConverter.generateEpub();
    }

    public File getFile() {
        return new File("books/" + filename + ".epub");
    }

    public String getTitle() {
        return title;
    }


}
