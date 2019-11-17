package ai.odoo.doctodigit.util;

import ai.odoo.doctodigit.config.TesseractProperties;
import net.sourceforge.tess4j.Tesseract;

public class TesseractUtils {

    public Tesseract getTesseract(TesseractProperties tesseractProperties) {
        Tesseract instance = new Tesseract();
        instance.setDatapath(tesseractProperties.getDatapath());
        instance.setLanguage(tesseractProperties.getLanguage());
        instance.setHocr(tesseractProperties.getHocr());
        instance.setTessVariable("preserve_interword_spaces", tesseractProperties.getPreserveInterwordSpaces());
        instance.setTessVariable("textord_tabfind_find_tables", "1");
        instance.setTessVariable("textord_tablefind_recognize_tables", "1");
        return instance;
    }
}
