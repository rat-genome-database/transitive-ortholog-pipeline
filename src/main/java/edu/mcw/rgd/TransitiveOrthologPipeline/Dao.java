package edu.mcw.rgd.TransitiveOrthologPipeline;

import edu.mcw.rgd.dao.impl.OrthologDAO;
import edu.mcw.rgd.datamodel.Ortholog;
import edu.mcw.rgd.datamodel.SpeciesType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cdursun
 * @since 2/3/2017
 * wrapper for all calls to database
 */
public class Dao {

    private final static Logger logInserted = LogManager.getLogger("inserted");
    private final static Logger logDeleted = LogManager.getLogger("deleted");

    private Date runDate;
    private int transitiveOrthologType;
    private int transitiveOrthologPipelineId;
    private int subjectSpeciesType;
    private int decisionDurationInMinForOrthologDeletion;


    public void init(Date runDate, int transitiveOrthologType, int transitiveOrthologPipelineId, int subjectSpeciesType){
        this.runDate = runDate;
        this.transitiveOrthologType = transitiveOrthologType;
        this.transitiveOrthologPipelineId = transitiveOrthologPipelineId;
        this.subjectSpeciesType = subjectSpeciesType;
        // the ortholog cache is keyed by source rgd id only; clear it between species runs
        // (runAll.sh "0" reuses this bean across every species)
        orthoCache.clear();
    }

    private OrthologDAO orthologDAO = new OrthologDAO();

    // memoizes orthologDAO.getOrthologsForSourceRgdId(rgdId) for the duration of one species run --
    // the same gene is looked up many times across the N x M parallel comparison loop
    private final ConcurrentHashMap<Integer, List<Ortholog>> orthoCache = new ConcurrentHashMap<>();

    /**
     * orthologs whose SOURCE is the given rgd id, memoized for the run.
     * NOTE: the returned list is the shared cached instance -- callers must not mutate it.
     */
    private List<Ortholog> getOrthologsForSourceRgdId(int rgdId) throws Exception {
        List<Ortholog> orthos = orthoCache.get(rgdId);
        if( orthos==null ) {
            orthos = orthologDAO.getOrthologsForSourceRgdId(rgdId);
            orthoCache.put(rgdId, orthos);
        }
        return orthos;
    }

    public List<Ortholog> getSubjectSpeciesHumanOrthologs() throws Exception{
        return orthologDAO.getAllOrthologs(this.subjectSpeciesType, SpeciesType.HUMAN);
    }

    /**
     * returns two orthologs, including the reciprocal one between srcRgdId and destRgdId
    */
    public List<Ortholog> getOrthologs(Ortholog srcOrtho, Ortholog humanOrtho) throws Exception{
        // filter into a fresh list (the cached lists must not be mutated)
        List<Ortholog> result = new ArrayList<>();
        for( Ortholog o: getOrthologsForSourceRgdId(srcOrtho.getSrcRgdId()) ) {
            if( o.getDestSpeciesTypeKey() == humanOrtho.getDestSpeciesTypeKey() ) {
                result.add(o);
            }
        }
        for( Ortholog o: getOrthologsForSourceRgdId(humanOrtho.getDestRgdId()) ) {
            if( o.getDestSpeciesTypeKey() == srcOrtho.getSrcSpeciesTypeKey() ) {
                result.add(o);
            }
        }
        return result;
    }

    public int updateLastModified(List<Ortholog> orthologs) throws Exception {
        return orthologDAO.updateLastModified(orthologs, this.transitiveOrthologPipelineId);
    }

    public int insertOrthologs(List<Ortholog> orthologs) throws Exception {
        for( Ortholog o: orthologs ) {
            logInserted.info(o.dump("|"));
        }
        return orthologDAO.insertOrthologs(orthologs);
    }

    public int deleteOrthologs(List<Ortholog> orthologs) throws Exception {
        for( Ortholog o: orthologs ) {
            logDeleted.info(o.dump("|"));
        }
        return orthologDAO.deleteOrthologs(orthologs);
    }

    /**
     * get <b>transitive</b> orthologs for given pair of species that were modified before the run date
     *
     * @return
     * @throws Exception
     */

    public List<Ortholog> getUnmodifiedTransitiveOrthologsSince(int min ) throws Exception {
        // delete cutoff: subtract the configured buffer (min minutes) from runDate so this run's
        // own freshly inserted/updated orthologs are excluded (and thus not deleted)
        Date cutoff = new Date(this.runDate.getTime() - (min * 60_000L));

        // Filter in SQL -- transitive type AND involving the subject species AND modified before the
        // cutoff -- rather than pulling the whole GENETOGENE_RGD_ID_RLT table (~1.8M rows) into Java
        // and filtering with removeIf. The previous approach dominated runtime (~4-5 min/species to
        // delete ~0 rows).
        String sql = """
            SELECT o.*, s.species_type_key src_species_type_key, d.species_type_key dest_species_type_key
            FROM genetogene_rgd_id_rlt o, rgd_ids s, rgd_ids d
            WHERE o.src_rgd_id=s.rgd_id AND o.dest_rgd_id=d.rgd_id
              AND o.last_modified_date < ?
              AND o.ortholog_type_key = ?
              AND (s.species_type_key = ? OR d.species_type_key = ?)
            """;

        return orthologDAO.executeOrthologQuery(sql, cutoff, this.transitiveOrthologType,
                this.subjectSpeciesType, this.subjectSpeciesType);
    }

    /**
     * returns the orthologs of the human @rgdId and all other species types except subjectSpeciesType
     *
     * @param rgdId
     * @return
     */
    public List<Ortholog> getHumanOtherSpeciesOrthologs(int rgdId) throws Exception{

        // human orthologs to every species except the subject species (filtered into a fresh
        // list -- the cached list must not be mutated)
        List<Ortholog> result = new ArrayList<>();
        for( Ortholog o: getOrthologsForSourceRgdId(rgdId) ) {
            if( o.getDestSpeciesTypeKey() != this.subjectSpeciesType ) {
                result.add(o);
            }
        }
        return result;
    }

    /**
     * delete the subjectSpeciesType - Human transitive orthologs before the run date
     *
     * @return
     * @throws Exception
     */
    public int deleteUnmodifiedTransitiveOrthologs() throws Exception{
        List<Ortholog> unmodifiedTransitiveOrthologs = getUnmodifiedTransitiveOrthologsSince(decisionDurationInMinForOrthologDeletion);
        deleteOrthologs(unmodifiedTransitiveOrthologs);
        return unmodifiedTransitiveOrthologs.size();
    }

    public void setDecisionDurationInMinForOrthologDeletion(int decisionDurationInMinForOrthologDeletion) {
        this.decisionDurationInMinForOrthologDeletion = decisionDurationInMinForOrthologDeletion;
    }

    public int getDecisionDurationInMinForOrthologDeletion() {
        return decisionDurationInMinForOrthologDeletion;
    }
}

