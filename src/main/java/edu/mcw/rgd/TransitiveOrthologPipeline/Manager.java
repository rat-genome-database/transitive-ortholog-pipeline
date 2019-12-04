package edu.mcw.rgd.TransitiveOrthologPipeline;

import edu.mcw.rgd.datamodel.Ortholog;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by cdursun on 2/3/2017.
 */
public class Manager {

    public static final int INSERT_COUNTER = 0;
    public static final int UPDATE_COUNTER = 1;

    private String version;
    final static Logger logger = Logger.getLogger("summary");
    private int transitiveOrthologPipelineId;
    private String xrefDataSrc;
    private String xrefDataSet;
    private int transitiveOrthologType;
    private Dao dao;

    public int getSubjectSpeciesType() {
        return subjectSpeciesType;
    }

    public void setSubjectSpeciesType(int subjectSpeciesType) {
        this.subjectSpeciesType = subjectSpeciesType;
    }

    private int subjectSpeciesType;

    /**
     * load spring configuration from properties/AppConfigure.xml file
     * and run the pipeline
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));

        Manager manager = (Manager) bf.getBean("main");

        if (args == null || args.length == 0 || Integer.parseInt(args[0]) == SpeciesType.HUMAN){
            System.out.println("");
            System.out.println("                 Missing parameter!                  ");
            System.out.println("----------- Run with subject species type KEY other than Human! -----------");
            System.exit(0);
        }
        else{
            manager.setSubjectSpeciesType(Integer.parseInt(args[0]));

            if (SpeciesType.getCommonName(manager.getSubjectSpeciesType()).equals("")){
                System.out.println("There is no such species type!");
                System.exit(0);
            }

            logger.info("========== " +  SpeciesType.getCommonName(manager.getSubjectSpeciesType())  + " ==========");
        }

        Date time0 = Calendar.getInstance().getTime();

        try {
            manager.run(time0);
        } catch(Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw e;
        }

        logger.info("========== Elapsed time " + Utils.formatElapsedTime(time0.getTime(), System.currentTimeMillis()) + " ==========");
        logger.info("");
    }

    /**
     * print connection information, download the genes-diseases file from CTD, parse it, QC it and load the annotations into RGD
     * @throws Exception
     */
    public void run(Date runDate) throws Exception {

        logger.info(getVersion());

        dao.init(runDate, this.transitiveOrthologType, this.transitiveOrthologPipelineId, this.subjectSpeciesType);
        Process process = new Process(runDate, this.transitiveOrthologType, this.transitiveOrthologPipelineId, this.xrefDataSrc, this.xrefDataSet);

        //get subject species human orthologs
        //get human -other species orthologs for the listed subject species ortholog destination human genes
        //  if subject species doesn't have transitive ortholog in the "subject species - human - other species" link
        //        add new two reciprocal transitive orthologs
        //  else if subject species has transitive ortholog for that species and don't have other ortholog types
        //        update last modified date for reciprocal orthologs
        // delete all transitive orthologs that don't have the current last modified date

        List<Ortholog> subjectSpeciesHumanOrthologs = dao.getSubjectSpeciesHumanOrthologs();

        logger.info("Orthologs between " + SpeciesType.getCommonName(this.subjectSpeciesType) + " and Human : " + subjectSpeciesHumanOrthologs.size());

        AtomicInteger[] counters = new AtomicInteger[2];
        for( int i=0; i<counters.length; i++ ) {
            counters[i] = new AtomicInteger(0);
        }

        subjectSpeciesHumanOrthologs.parallelStream().forEach( sSHO -> {

            try {
                // get human-other species orthologs
                List<Ortholog> humanOtherSpeciesOrthologs = dao.getHumanOtherSpeciesOrthologs(sSHO.getDestRgdId());
                for (Ortholog hOSO : humanOtherSpeciesOrthologs) {

                    // get subject species - other species orthologs through subject species - human orthologs information
                    // this list consists of transitive ortholog candidates
                    List<Ortholog> subjectSpeciesOtherSpeciesOrthologs = dao.getOrthologs(sSHO, hOSO);

                    // if there is not any subject species-other species ortholog then creates new reciprocal transitive orthologs
                    if (subjectSpeciesOtherSpeciesOrthologs.size() == 0) {
                        dao.insertOrthologs(process.createReciprocalTransitiveOrthologs(sSHO.getSrcRgdId(), hOSO.getDestRgdId()));
                        counters[INSERT_COUNTER].getAndAdd(2);
                    }
                    // if there is not any non-transitive ortholog between the subject species and other species
                    // update last modified dates of the transitive orthologs
                    else if (!process.haveNonTransitiveOrthologs(subjectSpeciesOtherSpeciesOrthologs)) {
                        //if the transitive ortholog is created before this run then we want to update it otherwise no need to
                        dao.updateLastModified(subjectSpeciesOtherSpeciesOrthologs);
                        counters[UPDATE_COUNTER].getAndAdd(2);
                    }
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        logger.info("Updated reciprocal transitive orthologs : " + counters[UPDATE_COUNTER]);
        logger.info("Created reciprocal transitive orthologs : " + counters[INSERT_COUNTER]);

        // finally delete the untouched transitive orthologs after newly introduced genuine orthologs from other sources
        logger.info("Deleted unmodified transitive orthologs : " + dao.deleteUnmodifiedTransitiveOrthologs());
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setTransitiveOrthologPipelineId(int transitiveOrthologPipelineId) {
        this.transitiveOrthologPipelineId = transitiveOrthologPipelineId;
    }

    public int getTransitiveOrthologPipelineId() {
        return transitiveOrthologPipelineId;
    }

    public void setXrefDataSrc(String xrefDataSrc) {
        this.xrefDataSrc = xrefDataSrc;
    }

    public String getXrefDataSrc() {
        return xrefDataSrc;
    }

    public void setXrefDataSet(String xrefDataSet) {
        this.xrefDataSet = xrefDataSet;
    }

    public String getXrefDataSet() {
        return xrefDataSet;
    }

    public void setTransitiveOrthologType(int transitiveOrthologType) {
        this.transitiveOrthologType = transitiveOrthologType;
    }

    public int getTransitiveOrthologType() {
        return transitiveOrthologType;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }
}
