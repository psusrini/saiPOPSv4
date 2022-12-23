/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.constraints;

import static ca.mcmaster.saipopsv4.Constants.*;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class NogoodSearchResult {
        
    //num of vars fixings used to define the  nogood
    public int size;
    //origin constraint
    public String constraintName ;
    
    public TreeSet<String> highest_objectiveMagnitude_fractionalVars = new TreeSet<String>();
    // and their objective
    public double highestObjectiveMagnitude_amongFractionalVars;    
    public Set<Triplet> allVarsExamined;
    public Set<Triplet> allFractionalVarsExamined;  
    public double objectiveMagnitudeSum_Of_AllVarsExamined;
    
    public int getNumVarsExamined (){
        return null==allVarsExamined ? ZERO:  allVarsExamined.size();
    }
    
}
