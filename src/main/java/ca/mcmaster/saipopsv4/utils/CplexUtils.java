/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.utils;
   
import static ca.mcmaster.saipopsv4.Constants.*;
import ca.mcmaster.saipopsv4.Parameters;
import static ca.mcmaster.saipopsv4.Parameters.*;
import ca.mcmaster.saipopsv4.constraints.SimpleConstraint;
import ca.mcmaster.saipopsv4.constraints.Triplet;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class CplexUtils {
    
    public static void setCplexParameters (IloCplex cplex) throws IloException {
        
        cplex.setParam( IloCplex.Param.TimeLimit, SIXTY*  SIXTY);
         
        cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);

        cplex.setParam( IloCplex.Param.MIP.Strategy.File, FILE_STRATEGY);
        
        cplex.setParam( IloCplex.Param.Emphasis.MIP,  MIP_EMPHASIS);
                
        if (USE_BARRIER_FOR_SOLVING_LP) {
            cplex.setParam( IloCplex.Param.NodeAlgorithm  ,  IloCplex.Algorithm.Barrier);
            cplex.setParam( IloCplex.Param.RootAlgorithm  ,  IloCplex.Algorithm.Barrier);
        }

        cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , HEUR_FREQ);
        
        if (USE_STRONG_BRANCHING) {
            cplex.setParam(IloCplex.Param.MIP.Strategy.VariableSelect  , THREE );
        }

      
    }
     
    public static List<SimpleConstraint> 
        getConstraints (IloCplex cplex, 
                TreeMap<String, Double> objectiveCoeffsMap) 
                throws IloException{
         
        List<SimpleConstraint> result = new ArrayList<SimpleConstraint>( );
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        final int NUM_CONSTRAINTS = lpMatrix.getNrows();
        
        int[][] ind = new int[ NUM_CONSTRAINTS][];
        double[][] val = new double[ NUM_CONSTRAINTS][];
        
        double[] lb = new double[NUM_CONSTRAINTS] ;
        double[] ub = new double[NUM_CONSTRAINTS] ;
        
        lpMatrix.getRows(ZERO,   NUM_CONSTRAINTS, lb, ub, ind, val);
        
        IloRange[] ranges = lpMatrix.getRanges() ;        
        
        //build up each constraint 
        for (int index=ZERO; index < NUM_CONSTRAINTS ; index ++ ){
            
            String thisConstraintname = ranges[index].getName();
          
            final int NUM_VARS_IN_CONSTRAINT =  ind[index].length;
                      
            boolean isUpperBound = Math.abs(ub[index])< BILLION ;
            boolean isLowerBound = Math.abs(lb[index])<BILLION ;
            boolean isEquality = ub[index]==lb[index];
            
            if  (isEquality)  {
                SimpleConstraint lbcUP =new SimpleConstraint(lb[index], thisConstraintname + "_UP");
                SimpleConstraint lbcDOWN =new SimpleConstraint(-ub[index], thisConstraintname + "_Down" );
                                 
                for (  int varIndex = ZERO;varIndex< NUM_VARS_IN_CONSTRAINT;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    double ObjectiveCoeff = objectiveCoeffsMap.get(var) ;
                    lbcUP.add(new Triplet(var,  coeff,  ObjectiveCoeff  )) ;
                    lbcDOWN.add(new Triplet(var, -coeff, ObjectiveCoeff));    
                    
                }
                
                result.add(lbcUP);
                result.add(lbcDOWN);
                
            }else {
                
                //not an equailty constraint
                SimpleConstraint lbc =new SimpleConstraint
                    ((isUpperBound && ! isLowerBound )? -ub[index] : lb[index], thisConstraintname);
                 
                for (  int varIndex = ZERO;varIndex< NUM_VARS_IN_CONSTRAINT;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    lbc.add(new Triplet(var, (isUpperBound && ! isLowerBound )? -coeff: coeff, objectiveCoeffsMap.get(var))) ;     
                                        
                }
                
                result.add(lbc) ; 
                
            }
            
        }
     
        return result;
               
    }
    
    
    public static TreeMap<String, Double> getObjective (IloCplex cplex) throws IloException {
        
        TreeMap<String, Double>  objectiveMap = new TreeMap<String, Double>();
        
        IloObjective  obj = cplex.getObjective();
       
        IloLinearNumExpr expr = (IloLinearNumExpr) obj.getExpr();
                 
        IloLinearNumExprIterator iter = expr.linearIterator();
        while (iter.hasNext()) {
           IloNumVar var = iter.nextNumVar();
           double val = iter.getValue();
           
           objectiveMap.put(var.getName(),   val   );
           //note - 0 val is put if var is missing from objective           
        }
        
        return  objectiveMap ;
        
         
    }
        
    public static List<IloNumVar> getVariables (IloCplex cplex) throws IloException{
        List<IloNumVar> result = new ArrayList<IloNumVar>();
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        for (IloNumVar var :variables){
            result.add(var ) ;
        }
        return result;
    }
    
    
}
