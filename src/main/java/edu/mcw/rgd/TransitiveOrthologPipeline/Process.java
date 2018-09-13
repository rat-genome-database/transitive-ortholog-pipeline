package edu.mcw.rgd.TransitiveOrthologPipeline;

import edu.mcw.rgd.datamodel.Ortholog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by cdursun on 2/3/2017.
 */
public class Process {
    private Date processDate;
    private int transitiveOrthologType;
    private int transitiveOrthologPipelineId;
    private String xrefDataSrc;
    private String xrefDataSet;

    public Process(Date processDate, int transitiveOrthologType, int transitiveOrthologPipelineId, String xrefDataSrc, String xrefDataSet){
        this.processDate = processDate;
        this.transitiveOrthologType = transitiveOrthologType;
        this.transitiveOrthologPipelineId = transitiveOrthologPipelineId;
        this.xrefDataSrc = xrefDataSrc;
        this.xrefDataSet = xrefDataSet;
    }

    /**
     * creates new transitive ortholog between two given rgdId and returns new ortholog
     *
     * @param srcRgdId
     * @param destRgdId
     * @return
     */
    public Ortholog createNewTransitiveOrtholog(int srcRgdId, int destRgdId){
        Ortholog transitiveOrtholog = new Ortholog();
        transitiveOrtholog.setSrcRgdId(srcRgdId);
        transitiveOrtholog.setDestRgdId(destRgdId);
        //o.setGroupId();
        transitiveOrtholog.setXrefDataSrc(this.xrefDataSrc);
        transitiveOrtholog.setXrefDataSet(this.xrefDataSet);
        transitiveOrtholog.setOrthologTypeKey(this.transitiveOrthologType);//transitive type
        //o.setPercentHomology();
        //o.setRefKey();
        transitiveOrtholog.setCreatedBy(this.transitiveOrthologPipelineId);
        transitiveOrtholog.setCreatedDate(this.processDate);
        transitiveOrtholog.setLastModifiedBy(this.transitiveOrthologPipelineId);
        transitiveOrtholog.setLastModifiedDate(this.processDate);
        return transitiveOrtholog;
    }

    /**
     *  return true if there is a non transitive ortholog in the given list
     *
      * @param subjectSpeciesOtherSpeciesOrthologs
     * @return
     */
    public boolean haveNonTransitiveOrthologs(List<Ortholog> subjectSpeciesOtherSpeciesOrthologs){
        for (Ortholog sSOSO : subjectSpeciesOtherSpeciesOrthologs) {
            // check if there is subjectSpecies - otherSpecies non-transitive ortholog
            if (sSOSO.getOrthologTypeKey() != this.transitiveOrthologType) {
                //if (subjectSpeciesOtherSpeciesOrthologs.size()>2)
                    //System.out.println("Won't update --- src id: " + sSOSO.getSrcRgdId() + " -- dest id:" + sSOSO.getDestRgdId());
                return true;
            }
        }
        return false;
    }

    /**
     * creates two reciprocal transitive orthologs for the given rgdIds
     *
     * @param srcRgdId
     * @param destRgdId
     * @return
     * @throws Exception
     */
    public List<Ortholog>  createReciprocalTransitiveOrthologs(int srcRgdId, int destRgdId) throws Exception{

        List<Ortholog> subjectSpeciesOtherSpeciesOrthologs = new ArrayList<Ortholog>();

        Ortholog transitiveOrtholog = createNewTransitiveOrtholog(srcRgdId, destRgdId);
        subjectSpeciesOtherSpeciesOrthologs.add(transitiveOrtholog);


        Ortholog otherSpeciesToSubjectSpeciesOrtholog = (Ortholog) transitiveOrtholog.clone();
        otherSpeciesToSubjectSpeciesOrtholog.setSrcRgdId(destRgdId);
        otherSpeciesToSubjectSpeciesOrtholog.setDestRgdId(srcRgdId);
        subjectSpeciesOtherSpeciesOrthologs.add(otherSpeciesToSubjectSpeciesOrtholog);

        return subjectSpeciesOtherSpeciesOrthologs;

    }

}
