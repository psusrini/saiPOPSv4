/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.heuristics;

import static ca.mcmaster.saipopsv4.Constants.*;
import static ca.mcmaster.saipopsv4.Parameters.PERF_VARIABILITY_RANDOM_GENERATOR;
import ca.mcmaster.saipopsv4.constraints.NogoodSearchResult;
import ca.mcmaster.saipopsv4.constraints.SimpleConstraint;
import ca.mcmaster.saipopsv4.constraints.Triplet;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class POPS_Heuristic {
            
    //fixed variables and their values
    private TreeMap<String, Boolean> fixedVariables ;
    //free variables and their fractional values
    private TreeMap<String, Double>  freeVariables  ;
    private TreeSet<String> fractionalVariables ;
    private TreeMap<Integer, HashSet<SimpleConstraint>> mapOfAllConstraintsInTheModel;
    private TreeMap<String, Double>  objectiveFunctionMap;
        
    private  static Logger logger;
     
    static   {
        logger=Logger.getLogger(POPS_Heuristic.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+POPS_Heuristic.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public POPS_Heuristic (TreeMap<String, Boolean> fixings ,TreeMap<String, Double>  freeVariables,
            TreeSet<String> fractionalVariables ,
            TreeMap<Integer, HashSet<SimpleConstraint>> mapOfAllConstraintsInTheModel ,
            TreeMap<String, Double>  objectiveFunctionMap
            ){
        this.fixedVariables=fixings;
        this. freeVariables=freeVariables;
        this .fractionalVariables = fractionalVariables;
        this.mapOfAllConstraintsInTheModel=mapOfAllConstraintsInTheModel;
        this . objectiveFunctionMap = objectiveFunctionMap;
    }
    
    public String getBranchingVariable (){
                
        Set<String> suggestions =new HashSet<String> ();
         
        if (this.fractionalVariables.size ()==ONE){
            suggestions.addAll(fractionalVariables );
        }else  {
            
            List<NogoodSearchResult>  searchResults = getNogoods ( ) ;
             
            if (searchResults.size()==ZERO){
                logger.warn("No fractional variable in the smallest sized nogoods ! Try next smallest." );
                //just pick the highest obj vars                
                double highestknownObjMagnitude =   -ONE;
                for ( String  var: fractionalVariables ){
                    double thisObj = Math.abs ( this.objectiveFunctionMap.get(var));
                    if (highestknownObjMagnitude < thisObj){
                        suggestions.clear();
                        highestknownObjMagnitude= thisObj;
                        suggestions.add (var );
                    }else if (highestknownObjMagnitude == thisObj){
                        suggestions.add (var );
                    }
                }
            }else if (TWO == searchResults.get(ZERO).size){
                Map<String, Double> objectiveGrowth_UP = new HashMap <String, Double>  () ;
                Map<String, Double> objectiveGrowth_DOWN = new HashMap <String, Double>  () ;
                getObJectiveGrowths (searchResults, objectiveGrowth_DOWN, objectiveGrowth_UP  ) ;                  
                suggestions = maxiMin (objectiveGrowth_DOWN, objectiveGrowth_UP );                
            } else {
                suggestions = findHighestObjective_InHighestFrequency (searchResults) ;
            }
        }  
                        
        //random tiebreak
        List<String> candidateList = new ArrayList<String> ();
        candidateList.addAll(suggestions );
        Collections.shuffle(candidateList,  PERF_VARIABILITY_RANDOM_GENERATOR);
        return  candidateList.get(ZERO);
    }
    
    private Set<String> findHighestObjective_InHighestFrequency (List<NogoodSearchResult> searchResults) {
        Set<String> winners = new HashSet<String> ();
        
        double largestKnownNumber_ofVariblesExamined = -ONE;
        double largestKnownObjectiveMagnitude  = -ONE;
        
        for (NogoodSearchResult nsr: searchResults){
            
            double thisNumVariablesExamined = nsr.getNumVarsExamined();
            double thisHighestObjectiveMagnitude = nsr.highestObjectiveMagnitude_amongFractionalVars;
            
            boolean cond1 = thisHighestObjectiveMagnitude> largestKnownObjectiveMagnitude;
            boolean cond2 = thisNumVariablesExamined > largestKnownNumber_ofVariblesExamined &&
                    thisHighestObjectiveMagnitude == largestKnownObjectiveMagnitude;
            boolean cond3 = thisNumVariablesExamined == largestKnownNumber_ofVariblesExamined &&
                    thisHighestObjectiveMagnitude == largestKnownObjectiveMagnitude;
            
            if (cond1|| cond2){
                largestKnownNumber_ofVariblesExamined=thisNumVariablesExamined;
                largestKnownObjectiveMagnitude=thisHighestObjectiveMagnitude;
                winners.clear();
                winners.addAll (nsr.highest_objectiveMagnitude_fractionalVars);
            }else if (cond3){
                winners.addAll (nsr.highest_objectiveMagnitude_fractionalVars);
            }  
            
        }
        
        return winners;
    }
    
    //for a variable with a positive obj
    private void  getObJectiveGrowths (
            List<NogoodSearchResult> searchResults, 
            Map<String, Double> objectiveGrowth_DOWN, 
            Map<String, Double> objectiveGrowth_UP  ) {
        
        //all vars encountered and their objectives
        Map<String, Double > allVariablesEncountered = new TreeMap<String, Double > ();
        
        for (NogoodSearchResult nsr: searchResults){
            for (Triplet triplet : nsr.allFractionalVarsExamined){
                String thisVar = triplet.varName;
                double thisObjective =  triplet.objectiveCoeffcient ;
                allVariablesEncountered.put (thisVar,thisObjective );
                
                boolean isZeroFixInNogood = triplet.constraintCoefficient > ZERO;
                if (thisObjective > ZERO){
                    if (isZeroFixInNogood){
                        //implications will happen when this var branches down
                        appendScore (thisVar, objectiveGrowth_DOWN, 
                                nsr.objectiveMagnitudeSum_Of_AllVarsExamined - thisObjective) ;                         
                    }else {
                        //implications will happen when this var branches up
                        //implication sum will include its own objective magnitude
                        appendScore (thisVar, objectiveGrowth_UP, 
                                nsr.objectiveMagnitudeSum_Of_AllVarsExamined + thisObjective) ;
                    }
                }else {
                    if (isZeroFixInNogood){
                        //implications will happen when this var branches down
                        //implication sum will include its own objective magnitude
                        appendScore (thisVar, objectiveGrowth_DOWN, 
                                nsr.objectiveMagnitudeSum_Of_AllVarsExamined - thisObjective ) ;
                    }else {
                        //implications will happen when this var branches up
                        appendScore (thisVar, objectiveGrowth_UP, 
                                nsr.objectiveMagnitudeSum_Of_AllVarsExamined + thisObjective ) ;
                    }
                }
            }
        }
        
        //in addition to accumulations, there will be objective growth due to 
        //the branching variable itself
        for (Map.Entry<String, Double > entry : allVariablesEncountered.entrySet()){
            String thisVar = entry.getKey();
            Double thisObj = entry.getValue();
            if (thisObj> ZERO){
                appendScore ( thisVar,objectiveGrowth_UP , thisObj );
                appendScore ( thisVar,objectiveGrowth_DOWN, ZERO );
            }else {
                appendScore ( thisVar,objectiveGrowth_DOWN, -thisObj );
                appendScore ( thisVar, objectiveGrowth_UP, ZERO);
            }
        }
        
        
        
    }
    
    private void appendScore (String var , Map<String, Double> objectiveGrowthMap, double sum ) {
        Double current = objectiveGrowthMap.get (var) ;
        if (null==current) current = DOUBLE_ZERO;
        objectiveGrowthMap.put (var, sum+ current) ;
    }
    
    private Set<String> maxiMin ( Map<String, Double> objectiveGrowth_DOWN,  Map<String, Double> objectiveGrowth_UP ){
        Set<String> winners = new HashSet<String> ();
        
        double highestKnownPrimaryMetric = -ONE;
        double highestKnownSecondaryMetric = -ONE;
        
         
        
        for ( Map.Entry <String, Double> entry :objectiveGrowth_DOWN.entrySet() ){
             
            String thisVar = entry.getKey();
            Double thisMetric_DOWN =   entry.getValue();
             
            Double thisMetric_UP = objectiveGrowth_UP.get (thisVar);
           
            double thisPrimaryMetric = Math.min (thisMetric_DOWN,thisMetric_UP ) ;
            double thisSecondaryMetric = Math.max (thisMetric_DOWN,thisMetric_UP ) ;
            
            boolean cond1 = thisPrimaryMetric> highestKnownPrimaryMetric;
            boolean cond2 = thisPrimaryMetric== highestKnownPrimaryMetric &&
                    highestKnownSecondaryMetric< thisSecondaryMetric;
            boolean cond3 =  thisPrimaryMetric== highestKnownPrimaryMetric &&
                    highestKnownSecondaryMetric == thisSecondaryMetric;
            
            if (cond1|| cond2){
                highestKnownPrimaryMetric= thisPrimaryMetric;
                highestKnownSecondaryMetric =thisSecondaryMetric;
                winners.clear();
                winners.add (thisVar);
            } else if (cond3){
                winners.add (thisVar);
            }
        }
        
        return winners;
    }
        
    private List<NogoodSearchResult>  getNogoods ( )  {
        
        List<NogoodSearchResult> searchResults=   new ArrayList<NogoodSearchResult> ();
        int maxAllowed_NogoodSize= BILLION;
        
        //get nogoods from every constraint, starting with the ones having fewest variables
        //This ordering is not mandatory, but useful to avoid looking for nogoods in the larger constraints
        for ( HashSet<SimpleConstraint> constraintSet : mapOfAllConstraintsInTheModel.values()){
            for (SimpleConstraint lbc:  constraintSet){

                //get a copy
                SimpleConstraint  lbcCopy = lbc.copy();

                if (this.fixedVariables.size()>ZERO) lbcCopy.applyKnownFixings(fixedVariables);

                NogoodSearchResult result = lbcCopy.findSmallestNogood(maxAllowed_NogoodSize, fractionalVariables);

                if (null!= result){
                    if (result.size < maxAllowed_NogoodSize){                            
                        searchResults.clear();
                        maxAllowed_NogoodSize= result.size ;
                        searchResults.add (result );                            
                    } else if (result.size == maxAllowed_NogoodSize ){                           
                        searchResults.add ( result);
                    }
                }

            }
        }
        return  searchResults ;
    }
    
}
