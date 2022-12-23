/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.drivers;
   
import static ca.mcmaster.saipopsv4.Constants.*;
import static ca.mcmaster.saipopsv4.Parameters.*;
import ca.mcmaster.saipopsv4.callbacks.*;
import ca.mcmaster.saipopsv4.constraints.SimpleConstraint;
import ca.mcmaster.saipopsv4.utils.CplexUtils;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 *
 * @author tamvadss
 */
public class Solver {
    
    private Logger logger;
     
    private   IloCplex cplex;
     
    public    TreeMap<String, Double>  objectiveFunctionMap =null;
    public  TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    //constraints, smallest first
    public  TreeMap<Integer, HashSet<SimpleConstraint>> mapOfAllConstraintsInTheModel = 
            new TreeMap<Integer, HashSet<SimpleConstraint>> ();
   
    public Solver (Logger logger,  DriverEnum driverType  ) throws Exception{
   
        this.logger=logger;        
        
        initCplex(); 
        
        IloCplex.BranchCallback callback =  new EmptyCallback( mapOfAllVariablesInTheModel,objectiveFunctionMap );
        if (driverType.equals(DriverEnum.POPS)){
            callback=
                    new POPS_Callback(   objectiveFunctionMap,mapOfAllVariablesInTheModel, 
                            mapOfAllConstraintsInTheModel );
        } 
        
        cplex.use(callback) ;        
        
    }

    public void solve () throws IloException{
        logger.info ("Solve invoked ..." );
        for (int hours = ONE; hours <= MAX_TEST_DURATION_HOURS ; hours ++){                
            cplex.solve();
            print_statistics (cplex, hours) ;
            
          
            if (cplex.getStatus().equals( IloCplex.Status.Infeasible)) break;
            if (cplex.getStatus().equals( IloCplex.Status.Optimal)) break;
            
            if (hours == BRANCHING_OVERRULE_CYLES)  {
                logger.info ("Restoring empty callback ... ") ;
                cplex.use( new EmptyCallback(  mapOfAllVariablesInTheModel,objectiveFunctionMap));
            }            

        }
        cplex.end();
        logger.info ("Solve completed." );
    }
    
    
    private void initCplex ( ) throws Exception{
        cplex = new IloCplex ();
        cplex.importModel( PRESOLVED_MIP_FILENAME);
        CplexUtils.setCplexParameters(cplex) ;
        
        logger.info( "Cplex parameter MIP emphasis "+ MIP_EMPHASIS);
        logger.info( "Cplex parameter Strong branching "+ USE_STRONG_BRANCHING);
        logger.info( "Random seed "+ PERF_VARIABILITY_RANDOM_SEED);
        
        objectiveFunctionMap = CplexUtils.getObjective(cplex);
                
        for ( IloNumVar var : CplexUtils.getVariables(cplex)){
            mapOfAllVariablesInTheModel.put (var.getName(), var);
        }
        
        List<SimpleConstraint> lbcList = CplexUtils.getConstraints(cplex,objectiveFunctionMap );
                
        //arrange by size
        for (SimpleConstraint lbc: lbcList){
            int size = lbc.getSize();
            if (size> ZERO) {
                HashSet<SimpleConstraint> current =  mapOfAllConstraintsInTheModel.get (size);
                if (null==current) current = new HashSet<SimpleConstraint>();
                current.add (lbc) ;
                mapOfAllConstraintsInTheModel.put (size, current);
            }            
        }
                                               
    }
    
       
    private void print_statistics (IloCplex cplex, int hour) throws IloException {
        double bestSoln = BILLION;
        double relativeMipGap = BILLION;
        IloCplex.Status cplexStatus  = cplex.getStatus();
        if (cplexStatus.equals( IloCplex.Status.Feasible)  ||cplexStatus.equals( IloCplex.Status.Optimal) ) {
            bestSoln=cplex.getObjValue();
            relativeMipGap=  cplex.getMIPRelativeGap();
        };
        logger.info ("" + hour + ","+  bestSoln + ","+  
                cplex.getBestObjValue() + "," + cplex.getNnodesLeft64() +
                "," + cplex.getNnodes64() + "," + relativeMipGap ) ;
    }
    
}
