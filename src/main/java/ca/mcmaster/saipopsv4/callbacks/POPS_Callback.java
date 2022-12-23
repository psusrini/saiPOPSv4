/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.callbacks;
  
import static ca.mcmaster.saipopsv4.Constants.*; 
import ca.mcmaster.saipopsv4.constraints.SimpleConstraint;
import ca.mcmaster.saipopsv4.heuristics.POPS_Heuristic;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.HashSet;
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
public class POPS_Callback extends IloCplex.BranchCallback{
    
    
    private        TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel ;
    private        TreeMap<Integer, HashSet<SimpleConstraint>>  mapOfAllConstraintsInTheModel;
    private    TreeMap<String, Double>  objectiveFunctionMap =null; 
     
    
    public POPS_Callback (  
               TreeMap<String, Double>  objectiveFunctionMap,
            TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel ,
            TreeMap<Integer, HashSet<SimpleConstraint>> mapOfAllConstraintsInTheModel  
            ){
        
         
         this. objectiveFunctionMap = objectiveFunctionMap;
         this.mapOfAllVariablesInTheModel=mapOfAllVariablesInTheModel;
         this.mapOfAllConstraintsInTheModel=mapOfAllConstraintsInTheModel;
          
    }
        
    @Override
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
            
            String branchingVar = null;
            
            //get fixed and fractional vars
            TreeMap<String, Boolean> fixings = new  TreeMap<String, Boolean>();
            TreeMap<String, Double>  freeVariables = new TreeMap<String, Double>  ();
            TreeSet <String> fractionalvariables = new TreeSet <String> ();
            getFreeAndFixedVars (freeVariables, fixings, fractionalvariables) ;
            
            //System.out.flush();
            //System.out.println("\n-------------------\nThis node id is:" + getNodeId()) ;
            
            //printCplexBranchingDecision();
            
            //get branching recommendation
            try {
                
                branchingVar =  (new POPS_Heuristic(fixings, freeVariables,fractionalvariables,
                        mapOfAllConstraintsInTheModel, objectiveFunctionMap))
                        .getBranchingVariable(  );

                //overrule cplex branching
                overruleCplexBranching (branchingVar) ; 
                
                //System.out.println("MOHP  branching var is:" + branchingVar ) ;
                //System.out.println(" Num fixed vars is "+fixings.size()) ;
                //printObjectiveIncrease_dueToFixings(fixings);
                //System.out.flush();
                
            } catch (Exception ex ){
                System.err.println( ex);
                ex.printStackTrace();
                exit(ONE);
            }
        }
    }
        
    private  void getFreeAndFixedVars (  
             TreeMap<String, Double>  freeVariables,
              TreeMap<String, Boolean> fixings,
              TreeSet <String> fractionalvariables) throws IloException {
       
        IloNumVar[] allVariables = new  IloNumVar[mapOfAllVariablesInTheModel.size()] ;
        int index =ZERO;
        for  (Map.Entry <String, IloNumVar> entry : mapOfAllVariablesInTheModel.entrySet()) {
            //
            allVariables[index++] = entry.getValue();
        }
        
        double[] varValues = getValues (allVariables) ;
        IloCplex.IntegerFeasibilityStatus [] status =   getFeasibilities(allVariables);
        
        index =-ONE;
        for (IloNumVar var: allVariables){
            index ++;
            
            Double ub = getUB(var) ;
            Double lb = getLB(var) ;
            if (  status[index].equals(IloCplex.IntegerFeasibilityStatus.Infeasible)){
                freeVariables.put  (var.getName(),varValues[index] ) ;    
                fractionalvariables.add( var.getName());
            }else if (HALF < Math.abs (lb-ub) ) {
                freeVariables.put  (var.getName(),varValues[index] ) ;     
                                
            } else {
                
                fixings.put (var.getName(), varValues[index] > HALF) ;
                
                //System.err.println(var.getName() +" fixed at "+ varValues[index] + " has lb "+lb + " and ub "+ub) ;
                
            }            
        }
               
    }
    
    private void  overruleCplexBranching(String branchingVarName ) throws IloException {
        IloNumVar[][] vars = new IloNumVar[TWO][] ;
        double[ ][] bounds = new double[TWO ][];
        IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
        getArraysNeededForCplexBranching(branchingVarName, vars , bounds , dirs);

        //create both kids 

        double lpEstimate = getObjValue();
        IloCplex.NodeId zeroChildID =  makeBranch( vars[ZERO][ZERO],  bounds[ZERO][ZERO],
                                              dirs[ZERO][ZERO],  lpEstimate  );
        IloCplex.NodeId oneChildID = makeBranch( vars[ONE][ZERO],  bounds[ONE][ZERO],
                                                 dirs[ONE][ZERO],   lpEstimate );
        
        
        //System.out.println("Zero child "+ zeroChildID);
        //System.out.println("One child "+ oneChildID);
        
    }
    
    private void getArraysNeededForCplexBranching (String branchingVar,IloNumVar[][] vars ,
                                                   double[ ][] bounds ,IloCplex.BranchDirection[ ][]  dirs ){
        
        IloNumVar branchingCplexVar = mapOfAllVariablesInTheModel.get(branchingVar );
                 
        //    System.out.println("branchingCplexVar is "+ branchingCplexVar);
                 
        //get var with given name, and create up and down branch conditions
        vars[ZERO] = new IloNumVar[ONE];
        vars[ZERO][ZERO]= branchingCplexVar;
        bounds[ZERO]=new double[ONE ];
        bounds[ZERO][ZERO]=ZERO;
        dirs[ZERO]= new IloCplex.BranchDirection[ONE];
        dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;

        vars[ONE] = new IloNumVar[ONE];
        vars[ONE][ZERO]=branchingCplexVar;
        bounds[ONE]=new double[ONE ];
        bounds[ONE][ZERO]=ONE;
        dirs[ONE]= new IloCplex.BranchDirection[ONE];
        dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
    }
    
  
}
