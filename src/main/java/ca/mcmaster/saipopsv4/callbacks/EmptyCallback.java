/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.callbacks;
 
import static ca.mcmaster.saipopsv4.Constants.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class EmptyCallback extends IloCplex.BranchCallback{
      
    private  TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel;
    private    TreeMap<String, Double>  objectiveFunctionMap =null; 
    
    public EmptyCallback ( TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel,
                  TreeMap<String, Double>  objectiveFunctionMap   ){
        this.mapOfAllVariablesInTheModel = mapOfAllVariablesInTheModel;
        this.objectiveFunctionMap = objectiveFunctionMap;
    }

    @Override
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
            
        }
    }
    
}
