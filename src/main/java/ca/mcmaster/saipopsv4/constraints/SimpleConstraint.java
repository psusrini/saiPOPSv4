/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.constraints;

import static ca.mcmaster.saipopsv4.Constants.*;
import static ca.mcmaster.saipopsv4.Parameters.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class SimpleConstraint {
    
    private String name ;
    private double lowerBound;
    private double lhs_maxPossible=ZERO;
    private Map < String ,Double> nodeMap = new TreeMap < String ,Double>();
    private Map < String ,Double> objectiveMap = new TreeMap < String ,Double>();
    
    public SimpleConstraint ( double lowerBound, String name){
        this.   lowerBound =   lowerBound;
        this.name = name;
    }
    
    public String getName (){
        return name;
    }
    
    public int getSize (){
        return nodeMap.size();
    }
    
    public boolean isEmpty () {
        return nodeMap.size()==ZERO;
    }
    
    public SimpleConstraint copy () {
        SimpleConstraint result = new SimpleConstraint (  lowerBound, name);
        result.lhs_maxPossible = this.lhs_maxPossible;
        result.nodeMap.putAll(this.nodeMap);
        result.objectiveMap.putAll(this.objectiveMap);
        return result;
    }
    
    public void add (String var, double coeff, double objective) {
        if (coeff>ZERO) lhs_maxPossible+= coeff;
        nodeMap.put (var,   coeff) ;
        this.objectiveMap.put (var, objective) ;
    }
     public void add (Triplet triplet) {
        double coeff = triplet.constraintCoefficient;
        String var = triplet.varName;
        Double objective = triplet.objectiveCoeffcient;
        add (var, coeff,objective ); 
    }
    
    
    public void applyKnownFixing (String var, boolean  value) {
        Double coeff = nodeMap.get(var);
        if (null!=coeff){            
            if (coeff>ZERO ) lhs_maxPossible-= coeff;
            if (value)this.lowerBound -= coeff;
            nodeMap.remove (var) ;
            this.objectiveMap.remove(var);
        }        
    }
    
    public void applyKnownFixings (Map<String, Boolean> fixes) {
        for (Map.Entry<String, Boolean> entry : fixes.entrySet()){
            applyKnownFixing (entry.getKey(), entry.getValue() );
        }
    }
    
    public boolean isGauranteedInfeasible (){
        return lhs_maxPossible < lowerBound;
    }
    
    public boolean isGauranteedFeasible (){
        double lhs_smallestPossible= ZERO;
        for (Double coeff: this.nodeMap.values()){
            if (coeff< ZERO){
                lhs_smallestPossible+= coeff;
            }
        }
        return lhs_smallestPossible >= lowerBound;
    }
    
    public boolean peekForInfeasibility (String var, Boolean value){
        boolean result=isGauranteedInfeasible();
        Double coeff = nodeMap.get (var);
        
        if (null!=coeff){
            if ( value && coeff < ZERO){
                result = lhs_maxPossible + coeff < lowerBound;
            }   
            if ( !value && coeff > ZERO){
                result = lhs_maxPossible - coeff < lowerBound;
            }   
        }
                 
        return result;
    }
    
    public TreeMap<String, Boolean>  getImplications (TreeMap<String, Boolean> fixings){
        TreeMap<String, Boolean> results = new TreeMap<String, Boolean> ();
                
        SimpleConstraint lbcCopy = this.copy();
        lbcCopy.applyKnownFixings(fixings);
        
        if (! lbcCopy.isGauranteedInfeasible()){
            for (Map.Entry<String, Double> entry :lbcCopy.nodeMap.entrySet()){
                String var = entry.getKey();

                if (this.peekForInfeasibility(var, false)){
                    results.put (var, true) ;
                }
                if (this.peekForInfeasibility(var, true)){
                    results.put (var, false) ;
                }
            }
        }
        
        return results;
    }
  
    //invoke this method on a copy of the nogood
    public   NogoodSearchResult findSmallestNogood (     
            int MAX_DEPTH ,  
            TreeSet<String> fractionalVariables
            ){
                
        NogoodSearchResult result =null;
        
        List<Triplet> coefficientList =getSortedNodes_InDescendingOrder();
        double slack = this.lhs_maxPossible- this.lowerBound;   
        int numberOfVarFixes = ZERO;
                
        if (  !this.isGauranteedInfeasible() && !this.isGauranteedFeasible() && coefficientList.size()>ZERO) {
            
            //a constraint with no lhs is either gaurantteed feasible or gaurantted infeasible
            //however due to 0 being represented as -0.0000001, sometimes we get errors
            //So we add an explicit check for  coefficientList.size()>ZERO
                         
            //System.out.println(this);
            
            //Phase1 : keep picking the largest coeff list item
            TreeSet<String> largestObjectiveMagnitude_fractionalVars  = new TreeSet<String>();  
            double largestObjectiveMagnitude_ForFractionalVar= -ONE;
            double objSumOfVarsExamined = ZERO;
            Set<Triplet> allVariablesEncountered = new HashSet<Triplet> ();
            Set<Triplet> allFractionalVarsEncountered = new HashSet<Triplet> ();            
            
           
            while (slack >= ZERO && coefficientList.size()>ZERO){
                
                //sometimes slack is 0 but 0.00000001 will not compare to 0 
                //so we add the check  coefficientList.size()>ZERO

                //fix largest coeff element of sorted list
                numberOfVarFixes++ ;
                if (numberOfVarFixes > MAX_DEPTH) break;
                
                                
                Triplet thisListItem = coefficientList.get( ZERO);
                
                
                //check if fixing this var will create infeasibility
                if (slack < Math.abs (thisListItem.constraintCoefficient)){
                    //end of phase 1
                    break;                    
                } else {
                    
                    thisListItem = coefficientList.remove( ZERO);
                    slack -= Math.abs (thisListItem.constraintCoefficient);                                      
                    double thisObjMagnitude = Math.abs ( thisListItem.objectiveCoeffcient);    
                    
                    //remember that positive coeff gets 0 fix and -ve coeff gets
                    //1 fix in the search for nogood
                    //
                    //objective gain will happen when the implication is away from the BUV
                    //
                    //therefore, obj gain will happen when +ve obj var is 0 fixed in nogood, 
                    //and -ve obj var is 1 fixed in nogood
                    boolean isVarObjPositive = 
                            thisListItem.objectiveCoeffcient > ZERO;
                    boolean isVar0FixedInNogood = thisListItem.constraintCoefficient > ZERO;
                    boolean willThereBeObjectiveGain = isVarObjPositive&&isVar0FixedInNogood ||
                            !isVar0FixedInNogood && !isVarObjPositive ;
                    
                    if (willThereBeObjectiveGain) objSumOfVarsExamined +=thisObjMagnitude  ;
                    allVariablesEncountered.add (thisListItem) ;
                                                                           
                    if (fractionalVariables.contains( thisListItem.varName)) {      
                        
                        allFractionalVarsEncountered.add ( thisListItem  );
                        
                        if (thisObjMagnitude> largestObjectiveMagnitude_ForFractionalVar ) {
                            largestObjectiveMagnitude_ForFractionalVar  = thisObjMagnitude;                                
                            largestObjectiveMagnitude_fractionalVars.clear();
                            largestObjectiveMagnitude_fractionalVars.add (thisListItem.varName );                                
                        }else if (thisObjMagnitude==largestObjectiveMagnitude_ForFractionalVar ) {
                            largestObjectiveMagnitude_fractionalVars.add (thisListItem.varName );
                        }
                        
                    }                   
                }
                
            }//end phase1 , while slack >=0
            
            
            //Phase 2:  
            if (numberOfVarFixes <= MAX_DEPTH && numberOfVarFixes>=ONE){
                //we  have found a valid nogood, continue to phase 2
                
                //traverse rest of the coeff list                  
                while (coefficientList.size() > ZERO ){
                    Triplet thisListItem= coefficientList.remove( ZERO) ;
                    double thisConstraintCoeffMagnitude = Math.abs (  thisListItem.constraintCoefficient);  
                    
                    if ( thisConstraintCoeffMagnitude> slack){
                        
                        double thisObjMagnitude = Math.abs ( thisListItem.objectiveCoeffcient);
                        
                        boolean isVarObjPositive = 
                                thisListItem.objectiveCoeffcient > ZERO;
                        boolean isVar0FixedInNogood = thisListItem.constraintCoefficient > ZERO;
                        boolean willThereBeObjectiveGain = isVarObjPositive&&isVar0FixedInNogood ||
                                !isVar0FixedInNogood && !isVarObjPositive ;

                        if (willThereBeObjectiveGain) objSumOfVarsExamined +=thisObjMagnitude  ;
                        allVariablesEncountered.add (thisListItem) ;
                                            
                        if (fractionalVariables.contains( thisListItem.varName)) {    
                            
                            allFractionalVarsEncountered.add ( thisListItem  );
                            
                            if (thisObjMagnitude> largestObjectiveMagnitude_ForFractionalVar ) {
                                largestObjectiveMagnitude_ForFractionalVar  = thisObjMagnitude;                                
                                largestObjectiveMagnitude_fractionalVars.clear();
                                largestObjectiveMagnitude_fractionalVars.add (thisListItem.varName );                                
                            }else if (thisObjMagnitude==largestObjectiveMagnitude_ForFractionalVar ) {
                                largestObjectiveMagnitude_fractionalVars.add (thisListItem.varName );
                            }
                            
                        }                        
                        
                    } else break;
                }  //end while               
            } //end phase 2 
            
            
            if ( allFractionalVarsEncountered.size()>ZERO  ){                    
                //we have found a valid nogood which we will use to make a branching decision
                result = new NogoodSearchResult();
                result .size =numberOfVarFixes;
                result.constraintName = this.name;
                
                result.allFractionalVarsExamined=allFractionalVarsEncountered;
                result.allVarsExamined=allVariablesEncountered;
                result.objectiveMagnitudeSum_Of_AllVarsExamined =objSumOfVarsExamined;
                result.highestObjectiveMagnitude_amongFractionalVars =largestObjectiveMagnitude_ForFractionalVar;
                result.highest_objectiveMagnitude_fractionalVars  =largestObjectiveMagnitude_fractionalVars;
                                                
            }
                        
        } //end if neither gauranteed feasible or infeasible
        
        return result;
    }
    
    private List<Triplet> getSortedNodes_InDescendingOrder (){
        List<Triplet> sortedNodes = new ArrayList<Triplet>();
        for (Map.Entry <String, Double> entry : this.nodeMap.entrySet()){
            Triplet triplet = new Triplet () ;
            triplet.varName=entry.getKey();
            triplet.constraintCoefficient= entry.getValue();
            triplet.objectiveCoeffcient= this.objectiveMap.get(triplet.varName);
            sortedNodes.add (triplet );
        }
        
        Collections.shuffle(sortedNodes, PERF_VARIABILITY_RANDOM_GENERATOR);
        Collections.sort(sortedNodes);
        Collections.reverse(sortedNodes);
        return sortedNodes;
    }
    
}
