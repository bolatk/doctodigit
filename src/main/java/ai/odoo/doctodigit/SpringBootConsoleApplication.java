package ai.odoo.doctodigit;

import ai.odoo.doctodigit.config.FileProperties;
import ai.odoo.doctodigit.config.OdooServiceProperties;
import ai.odoo.doctodigit.config.TesseractProperties;
import ai.odoo.doctodigit.util.TesseractUtils;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static ai.odoo.doctodigit.util.FileUtils.getFileChecksum;
import static ai.odoo.doctodigit.util.OdooUtils.login;
import static java.util.Arrays.asList;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class SpringBootConsoleApplication {

    private static Logger LOG = LoggerFactory
            .getLogger(SpringBootConsoleApplication.class);

    @Autowired
    private FileProperties fileProperties;

    @Autowired
    private OdooServiceProperties odooServiceProperties;

    @Autowired
    private TesseractProperties tesseractProperties;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootConsoleApplication.class);
    }

//    @Scheduled(initialDelay = 5000, fixedDelay = 300*1000)
    public void doMagic() throws TesseractException, IOException, XmlRpcException, NoSuchAlgorithmException {

        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        Tesseract tesseract = new TesseractUtils().getTesseract(tesseractProperties);

        LOG.info("Attempting to connect to Odoo 12");

        int uid = login(odooServiceProperties.getUrl(),
                odooServiceProperties.getAuth().getDb(),
                odooServiceProperties.getAuth().getUsername(),
                odooServiceProperties.getAuth().getPassword());
        if (uid > 0) {
            LOG.info("Login Ok");
        } else {
            LOG.info("Login Fail");
        }

        final XmlRpcClient models = new XmlRpcClient();

        XmlRpcClientConfigImpl model_config = new XmlRpcClientConfigImpl();
        model_config.setEnabledForExtensions(true);
        model_config.setServerURL(new URL(odooServiceProperties.getUrl() + "/xmlrpc/2/object"));

        models.setConfig(model_config);

        final List input_docs = asList((Object[])models.execute("execute_kw", asList(
                odooServiceProperties.getAuth().getDb(), uid, odooServiceProperties.getAuth().getPassword(),
                "muk_dms.file", "search_read",
                asList(asList(
                        asList("directory", "=", 2))),
                new HashMap() {{
                    put("fields", asList("name", "checksum", "content_binary"));
                    put("limit", 1000);
                }}
                )));

        //Use SHA-1 algorithm
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");

        for (int i = 0; i < input_docs.size(); i++) {
            Map<String, Object> map = (Map<String, Object>) input_docs.get(i);

            byte[] decodedBytes = Base64.getDecoder().decode(map.get("content_binary").toString());

            String inputFileName = map.get("name").toString();
            int dotIndex = inputFileName.lastIndexOf(".");

            FileUtils.writeByteArrayToFile(
                    new File(fileProperties.getInputFilePath() + inputFileName), decodedBytes);
            File in = FileUtils.getFile(fileProperties.getInputFilePath() + inputFileName);

            if (map.get("checksum").toString().equals(getFileChecksum(shaDigest, in))) {
                String res = tesseract.doOCR(FileUtils.getFile(
                        fileProperties.getInputFilePath() + inputFileName));
                in.delete();

                String outputFileName = inputFileName.substring(0, dotIndex) + ".doc";

                BufferedWriter writer = new BufferedWriter(new FileWriter(
                        fileProperties.getOutputFilePath() + outputFileName));
                writer.write(res);
                writer.close();

                LOG.info("Document has successfully parsed.");

                File out = FileUtils.getFile(fileProperties.getOutputFilePath() + outputFileName);
                Long size = FileUtils.sizeOf(out);
                String checksum = getFileChecksum(shaDigest, out);

                byte[] fileContent = FileUtils.readFileToByteArray(out);
                String encodedString = Base64
                        .getEncoder()
                        .encodeToString(fileContent);

                models.execute("execute_kw", asList(
                        odooServiceProperties.getAuth().getDb(), uid, odooServiceProperties.getAuth().getPassword(),
                        "muk_dms.file", "create",
                        asList(new HashMap() {{
                            put("name", outputFileName);
                            put("directory", 4);
                            put("size", size);
                            put("checksum", checksum);
                            put("content_binary", encodedString);

                        }})
                ));

                out.delete();

                models.execute("execute_kw", asList(
                        odooServiceProperties.getAuth().getDb(), uid, odooServiceProperties.getAuth().getPassword(),
                        "muk_dms.file", "write",
                        asList(
                                asList(map.get("id")),
                                new HashMap() {{ put("directory", 3); }})
                ));

            } else {
                LOG.info("Downloaded file checksum has changed. File gets corrupted.");
            }

        }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 60*1000)
    public void testMethod() throws TesseractException, IOException {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        Tesseract tesseract = new TesseractUtils().getTesseract(tesseractProperties);

        String inputFileName = "Положение отдела ТОРО.pdf";

        String res = tesseract.doOCR(FileUtils.getFile(
                fileProperties.getInputFilePath() + inputFileName));

        int dotIndex = inputFileName.lastIndexOf(".");
        String outputFileName = inputFileName.substring(0, dotIndex) + ".txt";

        BufferedWriter writer = new BufferedWriter(new FileWriter(
                fileProperties.getOutputFilePath() + outputFileName));
        writer.write(res);
        writer.close();

        LOG.info("Document has successfully parsed.");

    }

}
